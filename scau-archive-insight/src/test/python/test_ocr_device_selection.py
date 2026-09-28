import os
import runpy
import sys
import types
import unittest
from pathlib import Path
from unittest.mock import patch


OCR_SCRIPT = (
    Path(__file__).resolve().parents[2]
    / "main"
    / "python"
    / "ppstructure"
    / "ocr_table.py"
)
sys.path.insert(0, str(OCR_SCRIPT.parent))


class FakePaddle:
    def __init__(self, *, cuda_devices, gpu_healthy):
        self.selected_devices = []
        self.gpu_healthy = gpu_healthy
        self.device = types.SimpleNamespace(
            cuda=types.SimpleNamespace(
                device_count=lambda: cuda_devices,
                synchronize=lambda: None,
            ),
            set_device=self.selected_devices.append,
            is_compiled_with_cuda=lambda: cuda_devices > 0,
        )
        self.nn = types.SimpleNamespace(
            functional=types.SimpleNamespace(conv2d=self._conv2d)
        )

    def ones(self, shape):
        return object()

    def _conv2d(self, image, kernel):
        if not self.gpu_healthy:
            raise RuntimeError("cuDNN unavailable")
        return object()


def load_ocr(device_value, fake_paddle):
    pipeline_configs = []
    fake_paddleocr = types.SimpleNamespace(
        PPStructureV3=lambda **kwargs: pipeline_configs.append(
            {
                **kwargs,
                "_cache_home": os.environ.get("PADDLE_PDX_CACHE_HOME"),
            }
        )
        or object()
    )
    environment = os.environ.copy()
    if device_value is None:
        environment.pop("OCR_DEVICE", None)
    else:
        environment["OCR_DEVICE"] = device_value

    with patch.dict(os.environ, environment, clear=True), patch.dict(
        sys.modules,
        {"paddle": fake_paddle, "paddleocr": fake_paddleocr},
    ):
        runpy.run_path(str(OCR_SCRIPT), run_name="ocr_table_for_test")
    return pipeline_configs


class OcrDeviceSelectionTest(unittest.TestCase):
    def test_auto_uses_gpu_only_after_health_check_succeeds(self):
        paddle = FakePaddle(cuda_devices=1, gpu_healthy=True)

        configs = load_ocr(None, paddle)

        self.assertEqual(["gpu"], paddle.selected_devices)
        self.assertEqual("gpu", configs[0].get("device"))

    def test_auto_falls_back_to_cpu_when_gpu_runtime_is_incomplete(self):
        paddle = FakePaddle(cuda_devices=1, gpu_healthy=False)

        configs = load_ocr(None, paddle)

        self.assertEqual(["gpu", "cpu"], paddle.selected_devices)
        self.assertEqual("cpu", configs[0].get("device"))

    def test_cpu_mode_never_probes_gpu(self):
        paddle = FakePaddle(cuda_devices=1, gpu_healthy=True)

        configs = load_ocr("cpu", paddle)

        self.assertEqual(["cpu"], paddle.selected_devices)
        self.assertEqual("cpu", configs[0].get("device"))

    def test_defaults_paddlex_cache_to_project_models_directory(self):
        configs = load_ocr("cpu", FakePaddle(cuda_devices=0, gpu_healthy=False))

        self.assertEqual(
            str(OCR_SCRIPT.parents[4] / "models" / ".paddlex"),
            configs[0]["_cache_home"],
        )


if __name__ == "__main__":
    unittest.main()
