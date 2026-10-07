from pathlib import Path

from dotenv import load_dotenv

AGENT_ROOT = Path(__file__).resolve().parents[1]

# Real environment variables win over agent/.env.
load_dotenv(AGENT_ROOT / ".env", override=False)
