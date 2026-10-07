"""M1 graph: classify → web_research → synthesize → validate, one repair retry.

Later milestones add nodes between classify and synthesize without changing the API.
"""

import time
from collections.abc import Callable, Iterator
from uuid import uuid4

from langgraph.graph import END, START, StateGraph

from app.graph.classify import classify
from app.graph.state import State
from app.graph.synthesize import synthesize
from app.graph.validate import validate
from app.graph.web_research import web_research
from app.llm import ModelClient, ModelFailure
from app.schemas import Classification, ErrorInfo, GenerateRequest, GenerateResponse, Usage

Trace = Callable[[str, str], None]


def build_graph():
    builder = StateGraph(State)
    for node in (classify, web_research, synthesize, validate):
        builder.add_node(node.__name__, node)
    builder.add_edge(START, "classify")
    builder.add_conditional_edges("classify", lambda state: END if state.get("error") else "web_research")
    builder.add_edge("web_research", "synthesize")
    builder.add_edge("synthesize", "validate")
    builder.add_conditional_edges(
        "validate", lambda state: "synthesize" if state["problems"] and not state.get("error") else END)
    return builder.compile()


GRAPH = build_graph()


def new_run_id() -> str:
    return f"run_{uuid4().hex[:12]}"


def stream_generate(request: GenerateRequest, llm: ModelClient,
                    trace: Trace | None = None) -> Iterator[tuple[str, object]]:
    """Yields ("progress", node_name) per finished node, then ("result", GenerateResponse)."""
    started = time.monotonic()
    if trace:
        trace("01_request.json", request.model_dump_json(indent=2))
    state: State = {"request": request, "usage": {}}
    try:
        for mode, payload in GRAPH.stream(state, {"configurable": {"llm": llm, "trace": trace}},
                                          stream_mode=["updates", "values"]):
            if mode == "values":
                state = payload
            else:
                for node in payload:
                    yield "progress", node
    except ModelFailure as exc:
        state = {**state, "error": ErrorInfo(code=exc.code, message=exc.message)}

    error = state.get("error")
    found = state.get("classification")
    recipe = None if error else state["recipe"]
    allergies = request.profile.allergies
    if recipe and allergies:
        recipe.warnings.append(f"Checked against your allergies: no {', '.join(allergies)} ingredients. "
                               "Always check product labels.")
    response = GenerateResponse(
        run_id=new_run_id(),
        status="succeeded" if not error else "rejected" if error.code == "NOT_A_RECIPE" else "failed",
        classification=Classification(type=found.type, normalized_name=found.normalized_name) if found else None,
        recipe=recipe,
        sources=[] if error else state["sources"],
        usage=Usage(latency_ms=round((time.monotonic() - started) * 1000), **state.get("usage", {})),
        error=error,
    )
    if trace:
        trace("05_response.json", response.model_dump_json(indent=2))
    yield "result", response


def generate(request: GenerateRequest, llm: ModelClient, trace: Trace | None = None) -> GenerateResponse:
    for _, payload in stream_generate(request, llm, trace):
        response = payload
    return response
