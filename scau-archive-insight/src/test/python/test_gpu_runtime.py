import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


PYTHON_ROOT = Path(__file__).resolve().parents[2] / "main" / "python"
sys.path.insert(0, str(PYTHON_ROOT / "ppstructure"))


class GpuRuntimeTest(unittest.TestCase):
    def test_registers_nvidia_bin_directories_before_paddle_import(self):
        from gpu_runtime import configure_nvidia_dll_directories

        with tempfile.TemporaryDirectory() as temp_dir:
            site_packages = Path(temp_dir)
            cudnn_bin = site_packages / "nvidia" / "cudnn" / "bin"
            cublas_bin = site_packages / "nvidia" / "cublas" / "bin"
            cudnn_bin.mkdir(parents=True)
            cublas_bin.mkdir(parents=True)
            registered = []

            with patch.object(os, "add_dll_directory", registered.append), patch.dict(
                os.environ, {"PATH": "existing-path"}, clear=False
            ):
                handles = configure_nvidia_dll_directories(site_packages)

                self.assertEqual(
                    [str(cublas_bin), str(cudnn_bin)], sorted(registered)
                )
                self.assertEqual(2, len(handles))
                self.assertTrue(os.environ["PATH"].startswith(str(cublas_bin)))


if __name__ == "__main__":
    unittest.main()
