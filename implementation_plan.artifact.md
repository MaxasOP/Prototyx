# Switch to OpenRouter Nemotron-3 Ultra

Replace local Ollama LLM calls with OpenRouter API using the `nvidia/nemotron-3-ultra-550b-a55b:free` model.

## User Review Required

> [!IMPORTANT]
> Use environment variables for OpenRouter API keys in production environments.

## Proposed Changes

### Backend Agents

#### [MODIFY] [committee.py](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/backend/agents/committee.py)
- Update `run_committee_debate` to call OpenRouter API.
- Update `summarize_earnings_llm` to call OpenRouter API.
- Update model name to `nvidia/nemotron-3-ultra-550b-a55b:free`.

#### [MODIFY] [test_llama_query.py](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/backend/test_llama_query.py)
- Update to match the new API structure for testing purposes.

## Verification Plan

### Manual Verification
- Launch the backend application and verify that the Investment Committee debate works.
- Check logs to ensure requests are going to OpenRouter and returning valid responses.
