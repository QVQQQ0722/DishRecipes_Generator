"""M1 eval, run from agent/: python -m evals.run_eval

Calls the real model and web search for every case, so it costs money. It checks status,
must-have ingredients and "no search on rejection"; step order and cost are not scored yet.
"""

import json
import sys
from datetime import datetime
from uuid import uuid4

from app import AGENT_ROOT
from app.graph import generate
from app.schemas import GenerateRequest, GenerateResponse, RecipeInput

CASES = AGENT_ROOT / "evals" / "dishes.json"


def score(case: dict, response: GenerateResponse) -> list[str]:
    """Empty list = pass. must_have is a list of groups; any term in a group satisfies it."""
    expected = case.get("expect", "succeeded")
    if response.status != expected:
        code = response.error.code if response.error else None
        return [f"status {response.status} (error {code}), expected {expected}"]
    if expected == "rejected":
        return ["rejected input still ran a web search"] if response.usage.search_requests else []
    names = " | ".join(item.name.lower() for item in response.recipe.ingredients)
    return [f"missing ingredient: {' / '.join(group)}" for group in case.get("must_have", [])
            if not any(term in names for term in group)]


def percentile(values: list[int], fraction: float) -> int:
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, round(fraction * (len(ordered) - 1)))]


def main():
    from app.llm import AzureModelClient

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    llm = AzureModelClient()
    rows = []
    for case in json.loads(CASES.read_text(encoding="utf-8")):
        request = GenerateRequest(request_id=str(uuid4()), user_id="eval",
                                  input=RecipeInput(type="dish_name", value=case["dish"]))
        response = generate(request, llm)
        problems = score(case, response)
        rows.append({"dish": case["dish"], "passed": not problems, "problems": problems,
                     "usage": response.usage.model_dump(), "response": response.model_dump()})
        print(f"{'PASS' if not problems else 'FAIL'}  {case['dish']}  {'; '.join(problems)}")

    latencies = [row["usage"]["latency_ms"] for row in rows]
    summary = {
        "cases": len(rows), "passed": sum(row["passed"] for row in rows),
        "latency_ms_p50": percentile(latencies, 0.5), "latency_ms_p95": percentile(latencies, 0.95),
        **{f"total_{key}": sum(row["usage"][key] for row in rows)
           for key in ("input_tokens", "output_tokens", "search_requests")},
    }
    output = AGENT_ROOT / "outputs" / f"eval-{datetime.now():%Y%m%d-%H%M%S}.json"
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps({"summary": summary, "rows": rows}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))
    print(f"Saved to: {output}", file=sys.stderr)


if __name__ == "__main__":
    main()
