# -*- coding: utf-8 -*-
import json
from langchain.tools import tool
from db import get_conn, put_conn

import re
import requests


def _query(sql, params=None, fetchone=False):
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(sql, params)
            if fetchone:
                return cur.fetchone()
            return cur.fetchall()
    finally:
        put_conn(conn)


@tool
def get_admission_stats(year: int = None) -> str:
    """获取招生总览统计：总录取人数、开设专业数、录取平均分、覆盖省份数。可指定年份，不指定则返回全部年份合计"""
    if year:
        sql = """
        SELECT
            (SELECT COUNT(*) FROM admission_fact WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s) AS total_admissions,
            (SELECT COUNT(DISTINCT major_id) FROM admission_fact WHERE major_id IS NOT NULL AND EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s) AS major_count,
            (SELECT ROUND(AVG(admission_score))::int FROM admission_fact WHERE admission_score IS NOT NULL AND EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s) AS avg_score,
            (SELECT COUNT(DISTINCT province_id) FROM admission_fact WHERE province_id IS NOT NULL AND EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s) AS province_count
        """
        row = _query(sql, (year, year, year, year), fetchone=True)
    else:
        row = _query("""
            SELECT
                (SELECT COUNT(*) FROM admission_fact) AS total_admissions,
                (SELECT COUNT(DISTINCT major_id) FROM admission_fact WHERE major_id IS NOT NULL) AS major_count,
                (SELECT ROUND(AVG(admission_score))::int FROM admission_fact WHERE admission_score IS NOT NULL) AS avg_score,
                (SELECT COUNT(DISTINCT province_id) FROM admission_fact WHERE province_id IS NOT NULL) AS province_count
        """, fetchone=True)
    return json.dumps({
        "total_admissions": row[0],
        "major_count": row[1],
        "avg_score": row[2],
        "province_count": row[3],
    }, ensure_ascii=False)


@tool
def get_admission_trend(year: int = None) -> str:
    """获取招生趋势数据（按年份分组）。可指定年份查询单年数据，不指定则返回全部年份"""
    year_filter = "WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s" if year else ""
    params = (year,) if year else None
    rows = _query(f"""
        SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS year,
               COUNT(*)::int AS count
        FROM admission_fact
        {year_filter}
        GROUP BY year ORDER BY year
    """, params)
    return json.dumps([{"year": r[0], "count": r[1]} for r in rows], ensure_ascii=False)


@tool
def get_major_distribution(year: int = None) -> str:
    """获取各专业录取人数分布。可指定年份，不指定则返回全部年份合计"""
    year_filter = "AND EXTRACT(YEAR FROM COALESCE(f.admission_date, f.create_time::date))::int = %s" if year else ""
    params = (year,) if year else None

    sql = f"""
        SELECT COALESCE(m.major_name, '未知') AS name, COUNT(*)::int AS count
        FROM admission_fact f
        LEFT JOIN major_dim m ON f.major_id = m.major_id
        WHERE 1=1 {year_filter}
        GROUP BY m.major_name ORDER BY count DESC
    """
    rows = _query(sql, params)
    return json.dumps([{"name": r[0], "count": r[1]} for r in rows], ensure_ascii=False)


@tool
def get_province_distribution(year: int = None) -> str:
    """获取各省份录取人数分布。可指定年份，不指定则返回全部年份合计"""
    year_filter = "AND EXTRACT(YEAR FROM COALESCE(f.admission_date, f.create_time::date))::int = %s" if year else ""
    params = (year,) if year else None
    sql = f"""
        SELECT COALESCE(p.province_name, '未知') AS name, COUNT(*)::int AS count
        FROM admission_fact f
        LEFT JOIN province_dim p ON f.province_id = p.province_id
        WHERE 1=1 {year_filter}
        GROUP BY p.province_name ORDER BY count DESC
    """
    rows = _query(sql, params)
    return json.dumps([{"name": r[0], "count": r[1]} for r in rows], ensure_ascii=False)


@tool
def get_score_stats(year: int = None) -> str:
    """获取录取分数统计（平均分、最高分、最低分），可按年份筛选"""
    if year:
        row = _query("""
            SELECT AVG(admission_score)::int, MAX(admission_score)::int, MIN(admission_score)::int
            FROM admission_fact
            WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s
              AND admission_score IS NOT NULL
        """, (year,), fetchone=True)
    else:
        row = _query("""
            SELECT AVG(admission_score)::int, MAX(admission_score)::int, MIN(admission_score)::int
            FROM admission_fact WHERE admission_score IS NOT NULL
        """, fetchone=True)
    return json.dumps({"avg_score": row[0], "max_score": row[1], "min_score": row[2]}, ensure_ascii=False)


@tool
def get_gender_distribution(year: int = None) -> str:
    """获取录取学生性别比例。可指定年份，不指定则返回全部年份合计"""
    year_filter = "AND EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s" if year else ""
    params = (year,) if year else None
    rows = _query(f"""
        SELECT COALESCE(gender, '未知') AS gender, COUNT(*)::int AS count
        FROM admission_fact
        WHERE 1=1 {year_filter}
        GROUP BY gender
    """, params)
    return json.dumps([{"gender": r[0], "count": r[1]} for r in rows], ensure_ascii=False)


@tool
def get_graduation_destination(year: int = None) -> str:
    """获取毕业去向分布（就业、升学等）。可指定年份，不指定则返回全部年份合计"""
    year_filter = "AND EXTRACT(YEAR FROM g.graduation_date) = %s" if year else ""
    params = (year,) if year else None
    rows = _query(f"""
        SELECT COALESCE(d.dest_name, '未知') AS name, COUNT(*)::int AS count
        FROM graduation_fact g
        LEFT JOIN destination_dim d ON g.dest_id = d.dest_id
        WHERE 1=1 {year_filter}
        GROUP BY d.dest_name ORDER BY count DESC
    """, params)
    return json.dumps([{"name": r[0], "count": r[1]} for r in rows], ensure_ascii=False)


@tool
def get_sankey_major_degree(year: int = None) -> str:
    """获取专业到学位的培养路径流向数据。可指定年份（基于毕业年份），不指定则返回全部"""
    year_filter = "AND EXTRACT(YEAR FROM g.graduation_date) = %s" if year else ""
    params = (year,) if year else None
    rows = _query(f"""
        SELECT m.major_name AS source, deg.degree_name AS target, COUNT(*)::int AS value
        FROM admission_fact a
        JOIN graduation_fact g ON a.student_no = g.student_no
        JOIN major_dim m ON a.major_id = m.major_id
        JOIN degree_dim deg ON g.degree_id = deg.degree_id
        WHERE 1=1 {year_filter}
        GROUP BY m.major_name, deg.degree_name
    """, params)
    return json.dumps([{"source": r[0], "target": r[1], "value": r[2]} for r in rows], ensure_ascii=False)


@tool
def get_sankey_degree_dest(year: int = None) -> str:
    """获取学位到毕业去向的流向数据。可指定年份（基于毕业年份），不指定则返回全部"""
    year_filter = "AND EXTRACT(YEAR FROM g.graduation_date) = %s" if year else ""
    params = (year,) if year else None
    rows = _query(f"""
        SELECT deg.degree_name AS source, d.dest_name AS target, COUNT(*)::int AS value
        FROM graduation_fact g
        JOIN degree_dim deg ON g.degree_id = deg.degree_id
        JOIN destination_dim d ON g.dest_id = d.dest_id
        WHERE 1=1 {year_filter}
        GROUP BY deg.degree_name, d.dest_name
    """, params)
    return json.dumps([{"source": r[0], "target": r[1], "value": r[2]} for r in rows], ensure_ascii=False)


@tool
def get_student_count() -> str:
    """获取当前在籍学生总数"""
    row = _query("SELECT COUNT(*)::int FROM student_fact", fetchone=True)
    return json.dumps({"student_count": row[0]}, ensure_ascii=False)


@tool
def get_prediction_data(year: int = None) -> str:
    """获取招生预测数据。可指定年份查询单年数据，不指定则返回最近3年趋势"""
    if year:
        row = _query("""
            SELECT COUNT(*)::int FROM admission_fact
            WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s
        """, (year,), fetchone=True)
        return json.dumps({"year": year, "count": row[0]}, ensure_ascii=False)
    rows = _query("""
        SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS year,
               COUNT(*)::int AS count
        FROM admission_fact GROUP BY year ORDER BY year DESC LIMIT 3
    """)
    return json.dumps([{"year": r[0], "count": r[1]} for r in rows], ensure_ascii=False)


@tool
def get_year_over_year(year: int = None) -> str:
    """获取招生数据同比对比。指定年份则与该年与前一年对比，不指定则用最近两年"""
    if year:
        rows = _query("""
            SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS yr,
                   COUNT(*)::int AS cnt
            FROM admission_fact
            WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int IN (%s, %s)
            GROUP BY yr ORDER BY yr DESC
        """, (year, year - 1))
    else:
        rows = _query("""
            SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS yr,
                   COUNT(*)::int AS cnt
            FROM admission_fact GROUP BY yr ORDER BY yr DESC LIMIT 2
        """)
    if len(rows) < 2:
        return json.dumps({"message": "数据不足两年，无法对比"}, ensure_ascii=False)
    latest, prev = rows[0], rows[1]
    change = ((latest[1] - prev[1]) / prev[1] * 100) if prev[1] > 0 else 0
    return json.dumps({
        "latest_year": latest[0], "latest_count": latest[1],
        "previous_year": prev[0], "previous_count": prev[1],
        "change_pct": round(change, 1),
    }, ensure_ascii=False)


@tool
def get_college_admission_stats(year: int = None) -> str:
    """获取各学院录取人数统计。可指定年份，不指定则返回全部年份合计"""
    year_filter = "AND EXTRACT(YEAR FROM COALESCE(f.admission_date, f.create_time::date))::int = %s" if year else ""
    params = (year,) if year else None
    rows = _query(f"""
        SELECT COALESCE(c.college_name, '未知') AS college,
               COUNT(*)::int AS count,
               COUNT(DISTINCT f.major_id)::int AS major_count
        FROM admission_fact f
        LEFT JOIN major_dim m ON f.major_id = m.major_id
        LEFT JOIN college_dim c ON m.college_id = c.college_id
        WHERE 1=1 {year_filter}
        GROUP BY c.college_name ORDER BY count DESC
    """, params)
    return json.dumps([{"college": r[0], "admissions": r[1], "majors": r[2]} for r in rows], ensure_ascii=False)


@tool
def get_score_distribution(year: int = None) -> str:
    """获取录取分数段分布。可指定年份，不指定则返回全部年份合计"""
    year_filter = "AND EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = %s" if year else ""
    params = (year,) if year else None
    rows = _query(f"""
        SELECT
            CASE
                WHEN admission_score >= 650 THEN '650分以上'
                WHEN admission_score >= 600 THEN '600-649分'
                WHEN admission_score >= 550 THEN '550-599分'
                WHEN admission_score >= 500 THEN '500-549分'
                WHEN admission_score >= 450 THEN '450-499分'
                ELSE '450分以下'
            END AS score_range,
            COUNT(*)::int AS count
        FROM admission_fact
        WHERE admission_score IS NOT NULL {year_filter}
        GROUP BY score_range ORDER BY MIN(admission_score) DESC
    """, params)
    return json.dumps([{"range": r[0], "count": r[1]} for r in rows], ensure_ascii=False)


@tool
def get_graduation_count(year: int = None) -> str:
    """获取毕业总人数。可指定年份，不指定则返回全部年份合计"""
    year_filter = "WHERE EXTRACT(YEAR FROM graduation_date) = %s" if year else ""
    params = (year,) if year else None
    row = _query(f"SELECT COUNT(*)::int FROM graduation_fact {year_filter}", params, fetchone=True)
    return json.dumps({"total_graduates": row[0]}, ensure_ascii=False)


@tool
def search_student(keyword: str) -> str:
    """按姓名或学号搜索学生，在招生表、学籍表、毕业表中联合查找（最多10条）"""
    # 从三张表分别搜索，合并结果
    sql = """
        SELECT DISTINCT student_no, name FROM (
            SELECT student_no, name FROM admission_fact
                WHERE name ILIKE %s OR student_no ILIKE %s OR id_card ILIKE %s OR exam_no ILIKE %s
            UNION
            SELECT student_no, name FROM student_fact
                WHERE name ILIKE %s OR student_no ILIKE %s
            UNION
            SELECT student_no, name FROM graduation_fact
                WHERE name ILIKE %s OR student_no ILIKE %s OR id_card ILIKE %s
        ) AS candidates ORDER BY student_no LIMIT 10
    """
    params = (f'%{keyword}%',) * 9
    candidates = _query(sql, params)
    if not candidates:
        return json.dumps({"message": "未找到匹配的学生"}, ensure_ascii=False)

    # 获取每个候选学生的完整信息
    student_nos = tuple(r[0] for r in candidates)
    result = []
    for sno in student_nos:
        detail = _query("""
            SELECT a.name, a.gender, COALESCE(p.province_name, '') AS province,
                   COALESCE(m.major_name, '') AS major, a.admission_score
            FROM admission_fact a
            LEFT JOIN province_dim p ON a.province_id = p.province_id
            LEFT JOIN major_dim m ON a.major_id = m.major_id
            WHERE a.student_no = %s
        """, (sno,), fetchone=True)

        has_student = _query("SELECT 1 FROM student_fact WHERE student_no = %s", (sno,), fetchone=True)
        has_grad = _query("SELECT 1 FROM graduation_fact WHERE student_no = %s", (sno,), fetchone=True)

        if detail:
            result.append({
                "student_no": sno, "name": detail[0], "gender": detail[1],
                "province": detail[2], "major": detail[3], "admission_score": detail[4],
                "in_school": bool(has_student), "graduated": bool(has_grad),
            })
        else:
            result.append({
                "student_no": sno, "name": "（仅毕业记录）", "gender": "",
                "province": "", "major": "", "admission_score": None,
                "in_school": bool(has_student), "graduated": bool(has_grad),
            })

    return json.dumps(result, ensure_ascii=False)


@tool
def get_student_detail(student_no: str) -> str:
    """获取单个学生的完整信息（含招生、学籍、毕业全链路数据）"""
    result = {}

    # 招生信息
    admission = _query("""
        SELECT f.student_no, f.name, f.gender, f.id_card, f.exam_no,
               f.admission_score, f.admission_date,
               COALESCE(p.province_name, '') AS province,
               COALESCE(m.major_name, '') AS major
        FROM admission_fact f
        LEFT JOIN province_dim p ON f.province_id = p.province_id
        LEFT JOIN major_dim m ON f.major_id = m.major_id
        WHERE f.student_no = %s
    """, (student_no,), fetchone=True)
    if admission:
        result["student_no"] = admission[0]
        result["name"] = admission[1]
        result["gender"] = admission[2]
        result["id_card"] = admission[3]
        result["exam_no"] = admission[4]
        result["admission_score"] = admission[5]
        result["admission_date"] = str(admission[6]) if admission[6] else None
        result["province"] = admission[7]
        result["major"] = admission[8]
    else:
        result["student_no"] = student_no
        result["name"] = "（无招生记录）"
    # 学籍信息
    student = _query("""
        SELECT s.graduated, s.create_time,
               COALESCE(m.major_name, '') AS cur_major,
               COALESCE(c.class_name, '') AS cur_class
        FROM student_fact s
        LEFT JOIN major_dim m ON s.major_id = m.major_id
        LEFT JOIN class_dim c ON s.class_id = c.class_id
        WHERE s.student_no = %s
    """, (student_no,), fetchone=True)
    if student:
        result["student_status"] = "在籍" if not student[0] else "已毕业"
        result["student_created"] = str(student[1]) if student[1] else None
        result["current_major"] = student[2]
        result["current_class"] = student[3]
    else:
        result["student_status"] = "无学籍记录"

    # 毕业信息
    grad = _query("""
        SELECT g.graduation_date,
               COALESCE(deg.degree_name, '') AS degree,
               COALESCE(d.dest_name, '') AS destination
        FROM graduation_fact g
        LEFT JOIN degree_dim deg ON g.degree_id = deg.degree_id
        LEFT JOIN destination_dim d ON g.dest_id = d.dest_id
        WHERE g.student_no = %s
    """, (student_no,), fetchone=True)
    if grad:
        result["graduation_date"] = str(grad[0]) if grad[0] else None
        result["degree"] = grad[1]
        result["destination"] = grad[2]

    return json.dumps(result, ensure_ascii=False)


@tool
def web_search(query: str, max_results: int = 5) -> str:
    """搜索互联网获取最新信息，支持新闻、百科、实时数据等。参数 query 为搜索关键词，max_results 为返回结果数（最多10）"""
    try:
        headers = {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            )
        }
        resp = requests.get(
            "https://www.bing.com/search",
            params={"q": query, "count": min(max_results, 10)},
            headers=headers,
            timeout=15,
        )
        resp.raise_for_status()
        html = resp.text

        results = []
        for li in re.findall(r'<li class="b_algo"[^>]*>(.*?)</li>', html, re.S)[:max_results]:
            # 提取链接和标题
            a_tag = re.search(r'<a[^>]*href="(https?://[^"]+)"[^>]*>(.*?)</a>', li, re.S)
            if not a_tag:
                continue
            url = a_tag.group(1)
            title = re.sub(r'<[^>]+>', '', a_tag.group(2)).strip()
            # 提取摘要
            p_tag = re.search(r'<p[^>]*>(.*?)</p>', li, re.S)
            snippet = re.sub(r'<[^>]+>', '', p_tag.group(1)).strip() if p_tag else ""
            results.append({"title": title, "url": url, "snippet": snippet})

        if not results:
            return json.dumps({"error": "未搜索到相关结果"}, ensure_ascii=False)
        return json.dumps(results, ensure_ascii=False)
    except Exception as e:
        return json.dumps({"error": f"搜索失败: {str(e)}"}, ensure_ascii=False)


@tool
def web_fetch(url: str) -> str:
    """获取指定网页的文本内容，参数 url 为完整网页链接"""
    try:
        from rag.document_loader import load_url
        import asyncio
        result = asyncio.run(load_url(url))
        text = "\n".join(result) if result else ""
        # 限制长度避免 token 溢出
        if len(text) > 8000:
            text = text[:8000] + "\n\n...（内容过长已截断）"
        return text if text else "无法获取网页内容"
    except Exception as e:
        return f"获取失败: {str(e)}"


tools = [
    get_admission_stats, get_admission_trend, get_major_distribution,
    get_province_distribution, get_score_stats, get_gender_distribution,
    get_graduation_destination, get_sankey_major_degree, get_sankey_degree_dest,
    get_student_count, get_prediction_data, get_year_over_year,
    get_college_admission_stats, get_score_distribution, get_graduation_count,
    search_student, get_student_detail,
    web_search, web_fetch,
]
