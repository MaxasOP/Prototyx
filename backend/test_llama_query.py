import requests
import json

def test_query():
    url = "https://openrouter.ai/api/v1/chat/completions"
    headers = {
        "Authorization": "Bearer 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",
        "Content-Type": "application/json"
    }
    payload = {
        "model": "nvidia/nemotron-3-ultra-550b-a55b:free",
        "messages": [
            {"role": "user", "content": "Say 'The Nemotron is awake and ready.'"}
        ]
    }
    
    print("Sending test query to OpenRouter (Nemotron-3 Ultra)...")
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        if response.status_code == 200:
            result = response.json()
            print("\n--- Response ---")
            print(result.get("choices", [{}])[0].get("message", {}).get("content"))
            print("----------------")
            print("\nSUCCESS: OpenRouter is responding correctly!")
        else:
            print(f"\nFAIL: OpenRouter returned status {response.status_code}")
            print(response.text)
    except Exception as e:
        print(f"\nERROR: Could not communicate with OpenRouter: {str(e)}")

if __name__ == "__main__":
    test_query()
