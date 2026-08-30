import requests
import json

BASE_URL = "http://localhost:8000"

def test_auth_flow():
    email = "test@example.com"
    password = "securepassword"
    name = "Test User"

    # 1. Register
    print("Testing Registration...")
    reg_response = requests.post(f"{BASE_URL}/api/auth/register", json={
        "email": email,
        "password": password,
        "name": name
    })
    print(f"Reg Status: {reg_response.status_code}")
    if reg_response.status_code != 200:
        print(f"Error: {reg_response.text}")
        # Maybe user exists, try login
    
    # 2. Login
    print("\nTesting Login...")
    login_response = requests.post(f"{BASE_URL}/api/auth/login", json={
        "email": email,
        "password": password
    })
    print(f"Login Status: {login_response.status_code}")
    assert login_response.status_code == 200
    token = login_response.json()["accessToken"]
    print(f"Token obtained: {token[:20]}...")

    # 3. Sync Holdings (Protected)
    print("\nTesting Sync Holdings...")
    headers = {"Authorization": f"Bearer {token}"}
    holdings = {"NVDA": 0.25, "AAPL": 0.15, "GOOGL": 0.10}
    sync_response = requests.post(
        f"{BASE_URL}/api/holdings/sync", 
        json={"holdings": holdings},
        headers=headers
    )
    print(f"Sync Status: {sync_response.status_code}")
    assert sync_response.status_code == 200
    synced_data = sync_response.json()
    print(f"Synced Data: {synced_data}")
    assert synced_data["NVDA"] == 0.25

    # 4. Remove a holding
    print("\nTesting Remove Holding...")
    holdings_update = {"AAPL": 0.0}
    sync_response_2 = requests.post(
        f"{BASE_URL}/api/holdings/sync", 
        json={"holdings": holdings_update},
        headers=headers
    )
    synced_data_2 = sync_response_2.json()
    print(f"Updated Synced Data: {synced_data_2}")
    assert "AAPL" not in synced_data_2

    print("\nAuth and Sync Flow Verified!")

if __name__ == "__main__":
    try:
        test_auth_flow()
    except Exception as e:
        print(f"Test failed: {e}")
        print("Make sure the backend is running at http://localhost:8000")
