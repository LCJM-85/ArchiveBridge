import os
import sys
from pathlib import Path


_DLL_DIRECTORY_HANDLES = []


def configure_nvidia_dll_directories(site_packages=None):
    """让 Windows 能加载由 pip 安装在虚拟环境中的 NVIDIA DLL。"""
    if os.name != "nt" or not hasattr(os, "add_dll_directory"):
        return []

    root = Path(site_packages) if site_packages else Path(sys.prefix) / "Lib" / "site-packages"
    bin_directories = sorted(
        path for path in (root / "nvidia").glob("*/bin") if path.is_dir()
    )
    if not bin_directories:
        return []

    current_path = os.environ.get("PATH", "")
    os.environ["PATH"] = os.pathsep.join(
        [*(str(path) for path in bin_directories), current_path]
    )
    handles = [os.add_dll_directory(str(path)) for path in bin_directories]
    _DLL_DIRECTORY_HANDLES.extend(handles)
    return handles
