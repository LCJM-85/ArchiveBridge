import os
import asyncio


def load_pdf(path):
    """PyMuPDF 逐页提取文本"""
    import fitz
    doc = fitz.open(path)
    texts = []
    for page in doc:
        text = page.get_text()
        if text.strip():
            texts.append(text)
    doc.close()
    return texts


def load_docx(path):
    """python-docx 提取段落"""
    from docx import Document
    doc = Document(path)
    return [p.text for p in doc.paragraphs if p.text.strip()]


def load_xlsx(path):
    """openpyxl 每行转文本"""
    import openpyxl
    wb = openpyxl.load_workbook(path, read_only=True, data_only=True)
    texts = []
    for sheet in wb:
        for row in sheet.iter_rows(values_only=True):
            line = " | ".join(str(c) for c in row if c is not None)
            if line.strip():
                texts.append(line)
    wb.close()
    return texts


def load_txt(path):
    """直接读取文本文件"""
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        content = f.read()
    return [content]


async def load_url(url):
    """用 Playwright + Chrome 渲染网页后提取文本，支持 JS 动态页面"""
    from playwright.async_api import async_playwright

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        try:
            page = await browser.new_page()
            await page.goto(url, wait_until="networkidle", timeout=30000)
            await page.wait_for_timeout(2000)
            text = await page.inner_text("body")
            return [text.strip()]
        finally:
            await browser.close()


def load_document(file_path: str, file_type: str = None):
    """同步解析文档（PDF/DOCX/XLSX/TXT），返回文本列表"""
    if file_type is None:
        ext = os.path.splitext(file_path)[1].lower().lstrip(".")
        file_type = ext

    if file_type == "pdf":
        return load_pdf(file_path)
    elif file_type in ("docx", "doc"):
        return load_docx(file_path)
    elif file_type in ("xlsx", "xls"):
        return load_xlsx(file_path)
    elif file_type in ("txt", "csv"):
        return load_txt(file_path)
    else:
        raise ValueError(f"不支持的文件类型: {file_type}")


async def load_document_async(file_path: str, file_type: str = None):
    """异步解析文档，目前只有 URL 需要异步"""
    if file_type is None:
        ext = os.path.splitext(file_path)[1].lower().lstrip(".")
        file_type = ext

    if file_type in ("html", "url"):
        return await load_url(file_path)
    else:
        return load_document(file_path, file_type)
