"""Run from backend/: python -m app.run_example --provider demo"""

import argparse
import asyncio
from datetime import datetime
from pathlib import Path
import sys
from uuid import uuid4

from pydantic import ValidationError

from app.errors import ImportFailure
from app.pipeline import create_pipeline


def main():
    parser = argparse.ArgumentParser(description="固定例子 → 可替换 VLM → 菜谱 JSON")
    parser.add_argument("--provider", help="覆盖 .env 的供应商配置，例如 demo 或 ollama")
    args = parser.parse_args()
    output = Path(__file__).resolve().parents[1] / "outputs" / f"{datetime.now():%Y%m%d-%H%M%S}-{uuid4().hex[:8]}"
    output.mkdir(parents=True)

    def trace(name: str, value: str):
        (output / name).write_text(value, encoding="utf-8")

    try:
        result = asyncio.run(create_pipeline(args.provider).run("fixture://tomato_eggs", trace))
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8")
        print(result.model_dump_json(indent=2))
        print(f"\n输入、原始输出和结果已保存到：{output}")
    except ImportFailure as exc:
        print(f"{exc.code}: {exc.message}\n已完成阶段的记录：{output}", file=sys.stderr)
        raise SystemExit(1)
    except (ValidationError, OSError) as exc:
        print(f"示例文件或配置错误：{exc}", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
