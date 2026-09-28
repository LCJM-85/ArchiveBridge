import json
import sys

from ocr_table import recognize


def send_result(result):
    print("RESULT\t" + json.dumps(result, ensure_ascii=False), flush=True)


print("READY", flush=True)

for line in sys.stdin:
    try:
        request = json.loads(line)
        input_path = request["inputPath"]
        send_result(recognize(input_path))
    except Exception as exc:
        send_result({"grids": [], "errors": [{"msg": str(exc)}]})
