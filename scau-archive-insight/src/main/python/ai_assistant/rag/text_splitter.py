from langchain_text_splitters import RecursiveCharacterTextSplitter


def create_text_splitter(chunk_size=500, chunk_overlap=50):
    return RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separators=["\n\n", "\n", "。", "！", "？", "，"],
    )


def split_text(text, chunk_size=500, chunk_overlap=50):
    splitter = create_text_splitter(chunk_size, chunk_overlap)
    return splitter.split_text(text)
