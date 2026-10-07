"""只读事实核对：数据库基准、当前工具输出及运行中问答服务。"""
import sys, json, urllib.request, concurrent.futures, os, asyncio, re
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'main/python/ai_assistant'))
import tools
from db import get_conn, put_conn

def baseline():
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT count(*), count(*) FILTER (WHERE graduated IS FALSE), count(*) FILTER (WHERE graduated IS TRUE), count(*) FILTER (WHERE graduated IS NULL) FROM student_fact")
            row = cur.fetchone()
            cur.execute("SELECT student_no,name FROM graduation_fact WHERE student_no IS NOT NULL AND NOT EXISTS (SELECT 1 FROM admission_fact a WHERE a.student_no=graduation_fact.student_no) ORDER BY student_no LIMIT 1")
            only_grad = cur.fetchone()
        return dict(zip(['all_student_records','active','graduated','unknown_status'],row)), only_grad
    finally:
        conn.rollback()
        put_conn(conn)

def chat(question):
    try:
        req=urllib.request.Request('http://127.0.0.1:8765/chat',data=json.dumps({'question':question,'history':[]}).encode(),headers={'Content-Type':'application/json'})
        with urllib.request.urlopen(req,timeout=55) as response:
            return {'question':question,'response':json.load(response)}
    except Exception as exc:
        return {'question':question,'error':str(exc)}

if __name__ == '__main__':
    truth, only_grad = baseline()
    print(json.dumps({'baseline':truth,'tool_count':json.loads(tools.get_student_count.invoke({}))},ensure_ascii=False),flush=True)
    questions=['目前仍在籍、尚未毕业的学生有多少人？请查询数据库，不要把已毕业学籍记录计入。',
               '数据库招生预测工具能直接给出2027年预测录取人数吗？如不能，请明确不要用历史人数代替预测。',
               '列出姓名含“王”的全部学生，并说明是否存在10条结果限制，能否据此确定总人数？']
    if only_grad:
        print(json.dumps({'graduation_only_baseline':only_grad,'search_tool':json.loads(tools.search_student.invoke({'keyword':only_grad[0]})),'detail_tool':json.loads(tools.get_student_detail.invoke({'student_no':only_grad[0]}))},ensure_ascii=False,default=str),flush=True)
        questions.append(f'请查学号{only_grad[0]}的真实姓名和毕业记录。没有招生记录不代表姓名未知。')
    if '--direct' in sys.argv:
        if '--detail-only' in sys.argv:
            questions = questions[-1:]
        # 独立进程加载当前代码，不占用8765，也不触发用户服务重启。
        import yaml
        cfg = yaml.safe_load((Path(__file__).resolve().parents[2] / 'main/resources/application.yaml').read_text(encoding='utf-8'))
        def resolve(value):
            match = re.fullmatch(r'\$\{([^:}]+)(?::(.*))?\}', value or '')
            return os.getenv(match[1], match[2] or '') if match else value
        os.environ['GLM_API_KEY'] = resolve(cfg['llm']['api-key'])
        os.environ['LLM_BASE_URL'] = resolve(cfg['llm']['base-url'])
        from agent import create_agent_executor
        from langchain_core.messages import AIMessage, ToolMessage
        async def direct():
            agent = create_agent_executor()
            for question in questions:
                try:
                    result = await asyncio.wait_for(agent.ainvoke({'messages': [('human', question)]}), timeout=75)
                    answer = next((m.content for m in reversed(result['messages']) if isinstance(m, AIMessage) and m.content), '')
                    tool_names = [m.name for m in result['messages'] if isinstance(m, ToolMessage)]
                    print(json.dumps({'question': question, 'tools_used': tool_names, 'answer': answer}, ensure_ascii=False), flush=True)
                except Exception as exc:
                    # 避免第三方异常内容泄露凭据；只记录异常类型。
                    print(json.dumps({'question': question, 'error_type': type(exc).__name__}, ensure_ascii=False), flush=True)
        asyncio.run(direct())
    else:
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            for result in pool.map(chat,questions): print(json.dumps(result,ensure_ascii=False),flush=True)
