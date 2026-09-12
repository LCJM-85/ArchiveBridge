import shutil
import sys
import unittest
import uuid
from pathlib import Path


MODULE_ROOT = Path(__file__).resolve().parents[2] / "main" / "python" / "ai_assistant"
sys.path.insert(0, str(MODULE_ROOT))

from rag.path_security import resolve_rag_file


class RagPathSecurityTest(unittest.TestCase):
    def setUp(self):
        self.test_base = (
            Path("target") / "security-test-data" / str(uuid.uuid4())
        ).resolve()
        self.rag_root = self.test_base / "rag"
        self.rag_root.mkdir(parents=True)

    def tearDown(self):
        shutil.rmtree(self.test_base, ignore_errors=True)

    def test_accepts_regular_file_inside_rag_root_and_derives_type(self):
        managed_file = self.rag_root / "guide.txt"
        managed_file.write_text("knowledge", encoding="utf-8")

        resolved, file_type = resolve_rag_file(str(managed_file), self.rag_root)

        self.assertEqual(managed_file.resolve(), resolved)
        self.assertEqual("txt", file_type)

    def test_rejects_file_outside_rag_root(self):
        outside = self.test_base / "server-secret.txt"
        outside.write_text("secret", encoding="utf-8")

        with self.assertRaises(ValueError):
            resolve_rag_file(str(outside), self.rag_root)

    def test_rejects_missing_file_and_directory(self):
        with self.assertRaises(ValueError):
            resolve_rag_file(str(self.rag_root / "missing.txt"), self.rag_root)
        with self.assertRaises(ValueError):
            resolve_rag_file(str(self.rag_root), self.rag_root)

    def test_rejects_symlink_that_resolves_outside_rag_root(self):
        outside = self.test_base / "server-secret.txt"
        outside.write_text("secret", encoding="utf-8")
        link = self.rag_root / "linked-secret.txt"
        try:
            link.symlink_to(outside)
        except OSError as exc:
            self.skipTest(f"当前环境不能创建符号链接: {exc}")

        with self.assertRaises(ValueError):
            resolve_rag_file(str(link), self.rag_root)


if __name__ == "__main__":
    unittest.main()
