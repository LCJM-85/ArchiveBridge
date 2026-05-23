"""
llm_extractor.py — 使用 LLM Vision API 从图片/PDF中提取结构化档案数据

用法:
  python llm_extractor.py <image_path> <rules_json_path> [--api-key KEY] [--base-url URL] [--model MODEL]

输出 (stdout):
  {"data": [{"field_code": "value", ...}], "errors": [...]}
"""

import json
import sys
import os
import base64
import argparse
import traceback
import urllib.request
import urllib.error

def encode_image(image_path):
    """读取图片并压缩，返回(base64, mime_type)"""
    try:
        import cv2
        import numpy as np
        with open(image_path, 'rb') as f:
            raw = f.read()
        img = cv2.imdecode(np.frombuffer(raw, np.uint8), cv2.IMREAD_COLOR)
        if img is not None:
            h, w = img.shape[:2]
            max_dim = 1200
            if max(h, w) > max_dim:
                scale = max_dim / max(h, w)
                new_w, new_h = int(w * scale), int(h * scale)
                img = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_AREA)
            _, buf = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, 85])
            return base64.b64encode(buf).decode('utf-8'), "image/jpeg"
    except Exception:
        pass
    # fallback: 原图
    with open(image_path, 'rb') as f:
        b64 = base64.b64encode(f.read()).decode('utf-8')
    ext = os.path.splitext(image_path)[1].lower()
    type_map = {'.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png',
                '.tiff': 'image/tiff', '.webp': 'image/webp'}
    return b64, type_map.get(ext, 'image/png')

def call_llm_api(messages, api_key, base_url, model):
    """调用 OpenAI 兼容的 LLM API"""
    url = f"{base_url.rstrip('/')}/chat/completions"
    payload = json.dumps({
        "model": model,
        "messages": messages,
        "temperature": 0.1,
        "max_tokens": 4096,
    }).encode('utf-8')

    req = urllib.request.Request(url, data=payload, method='POST')
    req.add_header('Content-Type', 'application/json')
    req.add_header('Authorization', f'Bearer {api_key}')

    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            result = json.loads(resp.read().decode('utf-8'))
            return result['choices'][0]['message']['content']
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8') if e.fp else ''
        raise RuntimeError(f"LLM API HTTP {e.code}: {body}")
    except urllib.error.URLError as e:
        raise RuntimeError(f"LLM API 连接失败: {e.reason}")

def build_prompt(rules):
    """从元数据规则构建prompt"""
    fields_desc = "\n".join([
        _format_field(r) for r in rules
    ])

    field_names = [r['fieldCode'] for r in rules]
    field_keys = ", ".join(f'"{fn}"' for fn in field_names)

    return (
        "你是一个档案信息提取助手。请识别图片表格中的每一列数据，理解其含义，"
        "然后映射到最匹配的字段编码。\n\n"
        "## 可用的字段编码及含义\n"
        f"{fields_desc}\n\n"
        "## 要求\n"
        f"1. 返回 JSON 数组，用以下字段编码作为键: {field_keys}\n"
        "2. 图片表头可能和字段名不完全一致，请根据含义匹配\n"
        "3. 如果图片中有字段不在列表中，尝试用最接近的字段编码\n"
        "4. 完全无法匹配的列忽略掉，不要硬塞到不相关的字段\n"
        "5. 如果某个学生某字段在图片中无对应值，设为 null\n"
        "6. 保持原始数据格式（日期保持 yyyy-MM-dd，分数保持数字）\n"
        "7. 只返回 JSON 数组，不要 markdown 代码块或其它说明文字\n"
        "8. 如果图片不是表格或无有效数据，返回空数组 []"
    )

def _format_field(r):
    """格式化单行字段描述，包含别名信息"""
    base = f"- {r['fieldCode']}（{r['fieldName']}）"
    alias = r.get('sourceField', '') or ''
    if alias and alias != r.get('fieldName', ''):
        base += f"，别名: {alias}"
    return base

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('image_path', help='图片或PDF路径')
    parser.add_argument('rules_path', help='元数据规则JSON文件路径')
    parser.add_argument('--api-key', default=os.environ.get('LLM_API_KEY', ''))
    parser.add_argument('--base-url', default=os.environ.get('LLM_BASE_URL', 'https://api.deepseek.com/v1'))
    parser.add_argument('--model', default=os.environ.get('LLM_MODEL', 'deepseek-chat'))
    args = parser.parse_args()

    if not args.api_key:
        result = {"data": [], "errors": [{"msg": "未配置 LLM API Key"}]}
        print(json.dumps(result, ensure_ascii=False))
        return

    # 读取规则
    try:
        with open(args.rules_path, 'r', encoding='utf-8') as f:
            rules = json.load(f)
    except Exception as e:
        result = {"data": [], "errors": [{"msg": f"读取规则文件失败: {e}"}]}
        print(json.dumps(result, ensure_ascii=False))
        return

    if not rules:
        result = {"data": [], "errors": [{"msg": "元数据规则为空"}]}
        print(json.dumps(result, ensure_ascii=False))
        return

    if not os.path.exists(args.image_path):
        result = {"data": [], "errors": [{"msg": f"文件不存在: {args.image_path}"}]}
        print(json.dumps(result, ensure_ascii=False))
        return

    try:
        # 编码图片（自动压缩到最长边1200px）
        image_b64, image_type = encode_image(args.image_path)

        prompt = build_prompt(rules)

        messages = [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt},
                    {"type": "image_url", "image_url": {"url": f"data:{image_type};base64,{image_b64}"}}
                ]
            }
        ]

        content = call_llm_api(messages, args.api_key, args.base_url, args.model)

        # 解析 LLM 返回的 JSON
        content = content.strip()
        # 移除可能的 markdown 代码块标记
        if content.startswith('```'):
            lines = content.split('\n')
            content = '\n'.join(lines[1:-1] if lines[-1].startswith('```') else lines[1:]).strip()

        parsed = json.loads(content)
        if isinstance(parsed, dict):
            parsed = [parsed]

        result = {"data": parsed, "errors": []}
        print(json.dumps(result, ensure_ascii=False, default=str))

    except json.JSONDecodeError as e:
        result = {"data": [], "errors": [{"msg": f"LLM 返回非JSON: {content[:200] if 'content' in dir() else str(e)}"}]}
        print(json.dumps(result, ensure_ascii=False))
    except Exception as e:
        result = {"data": [], "errors": [{"msg": f"LLM提取失败: {e}"}]}
        print(json.dumps(result, ensure_ascii=False))

if __name__ == '__main__':
    main()
