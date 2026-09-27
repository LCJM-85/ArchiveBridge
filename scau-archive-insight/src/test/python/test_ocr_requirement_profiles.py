import re
import unittest
from pathlib import Path


PYTHON_ROOT = Path(__file__).resolve().parents[2] / "main" / "python"


def paddle_requirements(path):
    return [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if re.match(r"^paddlepaddle(?:-gpu)?==", line.strip())
    ]


class OcrRequirementProfilesTest(unittest.TestCase):
    def test_cpu_and_gpu_profiles_each_install_exactly_one_matching_runtime(self):
        cpu_profile = PYTHON_ROOT / "requirements.txt"
        gpu_profile = PYTHON_ROOT / "requirements-gpu.txt"

        self.assertTrue(gpu_profile.is_file(), "缺少独立 GPU 依赖清单")
        cpu_runtime = paddle_requirements(cpu_profile)
        gpu_runtime = paddle_requirements(gpu_profile)
        self.assertEqual(["paddlepaddle==3.0.0"], cpu_runtime)
        self.assertEqual(["paddlepaddle-gpu==3.0.0"], gpu_runtime)


if __name__ == "__main__":
    unittest.main()
