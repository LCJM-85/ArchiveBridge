from pathlib import Path


ALLOWED_FILE_TYPES = {"pdf", "docx", "xlsx", "txt"}


def resolve_rag_file(file_path: str, rag_root: Path | None = None) -> tuple[Path, str]:
    """Resolve an existing knowledge file and prove that it stays inside storage/rag."""
    if not file_path or not str(file_path).strip():
        raise ValueError("文件路径不能为空")

    configured_root = Path(rag_root) if rag_root is not None else Path.cwd() / "storage" / "rag"
    try:
        real_root = configured_root.resolve(strict=True)
        real_file = Path(file_path).resolve(strict=True)
    except (OSError, RuntimeError) as exc:
        raise ValueError("知识库文件不存在或路径无效") from exc

    if not real_file.is_file() or not real_file.is_relative_to(real_root):
        raise ValueError("知识库文件不在受管目录内")

    file_type = real_file.suffix.lower().lstrip(".")
    if file_type not in ALLOWED_FILE_TYPES:
        raise ValueError("不支持的知识库文件类型")

    return real_file, file_type
