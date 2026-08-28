import os
import sys
from dotenv import load_dotenv

# Ensure we can import from agents
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from agents.committee import run_committee_debate

def test():
    load_dotenv()
    print("Testing Committee Debate with strictly Local Ollama...")
    try:
        # Test with TCS
        result = run_committee_debate(["TCS"])
        print("\n--- DEBATE SUCCESS ---")
        print(f"Mode: {result.get('mode')}")
        for log in result.get('debate_logs', []):
            print(f"[{log['agent']}]: {log['message'][:100]}...")
        print(f"Views: {result.get('implied_views')}")
    except Exception as e:
        print(f"\n--- DEBATE FAILED ---")
        print(f"Error: {str(e)}")

if __name__ == "__main__":
    test()
