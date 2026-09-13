# Prototyx: Secure Multi-Agent Wealth Intelligence Terminal

[![Android](https://img.shields.io/badge/Android-36-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Bento%20UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115+-009688?style=for-the-badge&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com)
[![Python](https://img.shields.io/badge/Python-3.10+-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://python.org)
[![Groq](https://img.shields.io/badge/Groq-LPU%20Inference-F05032?style=for-the-badge&logo=fastapi&logoColor=white)](https://groq.com)
[![OpenRouter](https://img.shields.io/badge/OpenRouter-Nemotron--3%20Ultra-6366F1?style=for-the-badge)](https://openrouter.ai)
[![Room DB](https://img.shields.io/badge/Room-Local--First%20Cache-47A248?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Deployment](https://img.shields.io/badge/Render-Live%20Backend-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://prototyx.onrender.com)

> **Academic Project Synopsis**: SVKM's NMIMS — Mukesh Patel School of Technology Management & Engineering (MPSTME)  
> **Course**: Mobile Application Development (MAD) / Mini Project  
> **Author**: Manas Chauhan ([MANAS.CHAUHAN13@nmims.in](mailto:MANAS.CHAUHAN13@nmims.in))  
> **Live API Service**: [https://prototyx.onrender.com](https://prototyx.onrender.com)

---

## 📌 Executive Summary

Retail investors and self-directed wealth managers face an acute **information asymmetry**: institutional hedge funds leverage Bloomberg terminals, multi-analyst research desks, and algorithmic portfolio optimizers, while retail participants are left with static stock charts, biased social media sentiment, and fragmented broker tools.

**Prototyx** democratizes institutional-grade portfolio intelligence by combining:
1. **Adversarial Multi-Agent Debate**: A simulated live boardroom of 4 specialized AI financial agents (Macro, Fundamental, Technical, and Risk/Compliance) debating assets to reach unbiased consensus.
2. **Parallel Agentic Asset Consultation**: An orchestrator pattern that dispatches parallel workers for real-time quantitative telemetry (via `yFinance`) and live sentiment/FUD discovery (via `DuckDuckGo Search`), synthesized instantly via Groq LPUs.
3. **Quantitative Portfolio Risk Mesh**: Mathematical cross-asset correlation analysis, Net Portfolio Exposure Index ($\text{NEI}$), structural overlap alerts, and portfolio beta computation.
4. **Mean-Variance Optimization (MVO)**: Markowitz Modern Portfolio Theory (MPT) rebalancing driven dynamically by AI return views with natural language rationales.
5. **Local-First Resilient Architecture**: An offline-first Android client with Room database caching (30-minute auto-expiry), encrypted AES-256 credential storage, and seamless merge-sync with a cloud PostgreSQL database.

---

## 🏛️ System Architecture

```mermaid
flowchart TB
    subgraph Client["📱 Android Native Client (Kotlin & Jetpack Compose)"]
        UI["Bento-Grid UI Dashboard\n(Theme: Dark Neon & Gold)"]
        VM["Architecture ViewModels\n(StateFlow & Coroutines)"]
        Room["Room SQLite DB\n(Local-First Cache, 30m TTL)"]
        SecStore["EncryptedSharedPreferences\n(AES-256 Master Key)"]
        NetClient["Retrofit & OkHttp Engine\n(Auth Bearer Interceptor)"]

        UI --> VM
        VM --> Room
        VM --> NetClient
        SecStore -.-> NetClient
    end

    subgraph Gateway["☁️ Backend Gateway (FastAPI on Render)"]
        AuthMiddleware["OAuth2 & JWT Authenticator\n(Passlib & Bcrypt)"]
        MergeSync["Merge-Sync State Engine"]
        CloudDB[("PostgreSQL / SQLite Cloud DB\n(SQLModel ORM)")]

        NetClient == "HTTPS / REST (JWT Bearer)" ==> AuthMiddleware
        AuthMiddleware --> MergeSync
        MergeSync <--> CloudDB
    end

    subgraph MultiAgentEngine["🤖 Multi-Agent AI & Quant Engine"]
        Committee["🏛️ Investment Committee Debate\n(Nvidia Nemotron-3 Ultra 550B via OpenRouter)"]
        Orchestrator["🧠 Manager Orchestrator\n(Groq LPU Qwen-3.8-27b)"]
        DataWorker["📉 Data Analyst Worker\n(yFinance Real-Time Telemetry)"]
        NewsWorker["📰 News Researcher Worker\n(DuckDuckGo Live Search)"]
        RiskMesh["🕸️ Quantitative Risk Mesh\n(NumPy Net Exposure & Correlation)"]
        Optimizer["⚖️ Modern Portfolio Theory\n(Mean-Variance & Black-Litterman)"]

        AuthMiddleware --> Committee
        AuthMiddleware --> Orchestrator
        AuthMiddleware --> RiskMesh
        AuthMiddleware --> Optimizer

        Orchestrator --> DataWorker
        Orchestrator --> NewsWorker
    end
```

---

## 🔬 Core Features & Intelligence Engines

### 1. 🏛️ AI-Driven Investment Committee Debate
Rather than relying on a single biased LLM prompt, Prototyx simulates a live, adversarial boardroom debate among four virtual financial specialists:
* **🌍 Macro Analyst Agent**: Evaluates central bank interest rates, inflation regimes, currency headwinds, and macroeconomic indicators.
* **📊 Fundamental Analyst Agent**: Audits earnings quality, operating margins, Return on Equity (ROE), P/E ratios, and discounted cash flow dynamics.
* **📈 Technical Analyst Agent**: Analyzes multi-timeframe price action, 50/200-day moving average crossovers, and Relative Strength Index (RSI).
* **🛡️ Compliance & Risk Agent**: Enforces diversification mandates, sector exposure caps, maximum drawdown limits, and liquidity controls.

The debate is executed through **Nvidia Nemotron-3 Ultra (550B)** via OpenRouter. The backend parses the structured transcript in real time, extracts dialectical perspectives, and outputs a consensus vector of 12-month expected asset returns.

```mermaid
sequenceDiagram
    autonumber
    actor User as User (Android Client)
    participant API as FastAPI Backend
    participant LLM as OpenRouter (Nemotron-3 Ultra 550B)
    participant Quant as Portfolio Optimizer

    User->>API: POST /api/agents/debate { tickers: ["TCS", "RELIANCE"] }
    API->>LLM: Ingest adversarial prompt & historical metrics
    LLM-->>API: Stream multi-agent boardroom dialogue
    Note over API: Parse [Macro], [Fundamental], [Technical], [Compliance] logs
    Note over API: Extract JSON 12-Month Projected Return Views
    API->>Quant: Feed return views into MVO / Black-Litterman
    Quant-->>API: Optimal weights & Sharpe ratio
    API-->>User: Structured debate logs + Rebalanced portfolio
```

---

### 2. ⚡ Parallel Agentic Asset Consultation
Built on an asynchronous worker-orchestrator pattern:
1. **Manager Agent**: Parses user natural language queries (e.g., *"Should I buy Bitcoin?"* or *"Analyze NVDA"*), isolates the target financial asset, and dispatches parallel worker routines.
2. **Data Analyst Worker**: Asynchronously fetches spot prices, 30-day volatility, 52-week ranges, and trailing P/E from `yFinance`.
3. **News Researcher Worker**: Conducts live web queries through `DuckDuckGo Search`, aggregating breaking headlines, regulatory notices, and market FUD.
4. **Synthesis Engine**: Groq's high-speed LPU engine running `qwen/qwen3.8-27b` synthesizes the quantitative and qualitative inputs into:
   * **Strategy Summary**
   * **Risk Rating**
   * **Action Verdict** (`BUY` | `HOLD` | `SELL` | `WAIT`)

---

### 3. 🕸️ Quantitative Portfolio Risk Mesh
The Risk Mesh mathematically prevents concentration risk and exposes hidden dependencies across user holdings:
* **Cross-Asset Correlation Matrix**: Evaluates pairwise sector and asset co-movements.
* **Net Portfolio Exposure Index ($\text{NEI}$)**: Computes the inner-product risk density:
  $$\text{NEI} = \mathbf{w}^T \mathbf{C} \mathbf{w}$$
  where $\mathbf{w}$ is the normalized portfolio weight vector and $\mathbf{C}$ is the cross-asset correlation matrix.
* **Structural Redundancy Alerts**: Automatically identifies and flags correlated asset clustering (e.g., holding heavy allocations in both `TCS` and `INFY` with $r = 0.85$).
* **Portfolio Beta ($\beta_p$)**:
  $$\beta_p = \sum_{i=1}^{N} w_i \beta_i$$
* **Automated AI Risk Audit**: Translates the matrix mathematics into a plain-English risk memo outlining vulnerabilities.

---

### 4. ⚖️ Mean-Variance Optimization (MVO) & Rebalancing
Implements Markowitz Modern Portfolio Theory (MPT) to calculate the efficient frontier:
$$\max_{\mathbf{w}} \frac{\mathbf{w}^T \boldsymbol{\mu} - r_f}{\sqrt{\mathbf{w}^T \boldsymbol{\Sigma} \mathbf{w}}}$$
$$\text{subject to} \quad \sum_{i=1}^{N} w_i = 1, \quad w_i \ge 0$$
* Ingests dynamic asset views $\boldsymbol{\mu}$ produced by the AI Investment Committee.
* Computes expected annual return, portfolio volatility, and optimal Sharpe ratio.
* Accompanied by an AI-generated rebalancing rationale explaining why specific asset weights were scaled or reduced.

---

### 5. 📱 Local-First Persistence & Cloud Merge-Sync
* **Android Client**: Room SQLite database acts as the single source of truth for the UI. Market data is stamped with timestamps and expires after a 30-minute Time-To-Live (TTL), conserving battery and network bandwidth.
* **Cloud Merge-Sync**: When network connectivity is established, local weight adjustments are securely synced with the FastAPI backend over authenticated HTTPS endpoints.
* **AES-256 Storage**: User JWT access tokens and credentials are encrypted at rest using Android Jetpack `EncryptedSharedPreferences`.

---

## 🛠️ Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Mobile Client** | Kotlin 2.0+ | Native Android execution, static typing, and coroutine concurrency |
| **UI Framework** | Jetpack Compose | Modern declarative Bento-style UI, dark aesthetic, interactive charts |
| **Local Database** | Room SQLite ORM | Local-first persistence, holding weights cache, 30-min TTL |
| **Networking** | Retrofit 2 + OkHttp 3 | REST communication with Bearer token interceptor and logging |
| **Client Security** | AndroidX Security Crypto | `EncryptedSharedPreferences` utilizing AES-256 keys |
| **Backend API** | FastAPI (Python 3.10+) | High-performance ASGI framework for routing and token validation |
| **Backend ORM** | SQLModel (SQLAlchemy) | Unified Pydantic and database schema modeling |
| **Production Database** | PostgreSQL / SQLite | Persistent relational storage for user accounts and portfolio states |
| **LLM Inference (Debate)**| Nvidia Nemotron-3 Ultra (550B) | Multi-agent boardroom debate via OpenRouter API |
| **LLM Inference (Synthesis)**| Qwen-3.8-27B on Groq LPU | Ultra-low latency asset consultation and worker orchestration |
| **Market Telemetry** | yFinance API | Real-time prices, historical volatility, and company fundamentals |
| **Web Research** | DuckDuckGo Search API | Live financial news retrieval and sentiment scraping |
| **Mathematical Engine** | NumPy & SciPy | Cross-asset matrix dot-products, Net Exposure Index, beta calculations |
| **Hosting & Cloud** | Render Cloud Platform | Fully managed deployment for containerized ASGI service |

---

## 📂 Repository Structure

```
Prototyx/
├── android/                                  # Native Android Mobile Application
│   ├── app/
│   │   ├── build.gradle.kts                  # Android dependencies & SDK configurations
│   │   └── src/main/
│   │       ├── AndroidManifest.xml           # Permissions & application declaration
│   │       └── java/com/example/prototyx/
│   │           ├── MainActivity.kt           # Main Compose entrypoint
│   │           ├── PrototyxApp.kt            # App scaffold & navigation host
│   │           ├── Navigation.kt             # Navigation graphs & routes
│   │           ├── data/
│   │           │   ├── DataRepository.kt     # Local-first repository pattern
│   │           │   ├── local/                # Room DB entities, DAOs & database
│   │           │   ├── model/                # Client domain data models
│   │           │   ├── network/              # Retrofit instance & API endpoints
│   │           │   └── security/             # EncryptedSharedPreferences AuthManager
│   │           ├── theme/                    # Color tokens, Typography & Dark Theme
│   │           └── ui/
│   │               ├── components/           # Bento cards, buttons, dialogs
│   │               └── screens/
│   │                   ├── DashboardScreen.kt   # Bento-grid holdings & quick actions
│   │                   ├── CommitteeScreen.kt   # Live 4-agent boardroom debate
│   │                   ├── RiskMeshScreen.kt    # Net exposure & correlation mesh
│   │                   ├── OptimizerScreen.kt   # MVO Sharpe optimizer & rebalancing
│   │                   ├── EarningsScreen.kt    # Transcripts & LLM summaries
│   │                   └── LoginScreen.kt       # JWT authentication & registration
│   ├── gradle/                               # Gradle wrapper configuration
│   ├── build.gradle.kts                      # Root Gradle build script
│   └── settings.gradle.kts                   # Project plugins & module definitions
│
├── backend/                                  # Python FastAPI Cloud Backend
│   ├── app.py                                # Main FastAPI gateway & route controllers
│   ├── database.py                           # SQLModel engine & session management
│   ├── models.py                             # Relational DB models & Pydantic schemas
│   ├── auth_utils.py                         # Password hashing (Bcrypt) & JWT creation
│   ├── requirements.txt                      # Python dependencies
│   ├── test_backend.py                       # Quantitative & agent integration tests
│   ├── test_auth.py                          # Authentication & holdings sync tests
│   ├── .env                                  # Environment variables & API credentials
│   ├── agents/
│   │   ├── committee.py                      # Nemotron-3 Ultra investment committee
│   │   └── orchestrator.py                   # Groq LPU Manager, Data & News workers
│   ├── quant/
│   │   ├── risk_mesh.py                      # Net Exposure Index & correlation matrix
│   │   └── optimizer.py                      # Mean-Variance Optimization engine
│   └── data/
│       ├── market_data.py                    # yFinance market metric scrapers
│       └── earnings.py                       # Earnings transcript fetchers & parsers
└── README.md                                 # Master documentation & technical manual
```

---

## 📡 API Reference

Base URL (Production): `https://prototyx.onrender.com`  
Base URL (Local Development): `http://localhost:8000`

### 1. Authentication & State Synchronization

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register a new user (`email`, `password`, `name`). Returns JWT. |
| `POST` | `/api/auth/login` | No | Authenticate user credentials. Returns JWT access token. |
| `POST` | `/api/holdings/sync` | Bearer Token | Merge-sync client holdings dictionary `{ "TICKER": weight }`. |

### 2. Multi-Agent AI & Quant Intelligence

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/agents/debate` | Optional | Triggers the 4-agent boardroom debate for given ticker symbols. |
| `POST` | `/api/agents/consult` | Optional | Natural-language query orchestrating Data & News workers via Groq. |
| `POST` | `/api/quant/risk-mesh` | Optional | Computes correlation matrix, Net Exposure Index, beta, and structural alerts. |
| `POST` | `/api/quant/optimize` | Optional | Computes MVO rebalancing weights, Sharpe ratio, and AI rationale. |
| `GET`  | `/api/market/indicators/{ticker}` | Optional | Fetches real-time price, RSI, moving averages, and volatility. |
| `GET`  | `/api/earnings/transcript/{ticker}` | Optional | Fetches earnings transcript and AI executive summary. |

#### Example: Running Committee Debate
```bash
curl -X POST "https://prototyx.onrender.com/api/agents/debate" \
  -H "Content-Type: application/json" \
  -d '{"tickers": ["TCS", "RELIANCE", "AAPL"]}'
```

#### Example: Ingesting Holdings Merge-Sync
```bash
curl -X POST "https://prototyx.onrender.com/api/holdings/sync" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_ACCESS_TOKEN>" \
  -d '{"holdings": {"NVDA": 0.35, "AAPL": 0.25, "MSFT": 0.40}}'
```

---

## 🚀 Installation & Setup Guide

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer with Android SDK Platform 36
* **Java Development Kit**: JDK 17
* **Python**: Python 3.10 or higher
* **Git**: Version control

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/MaxasOP/Prototyx.git
cd Prototyx
```

---

### Step 2: Backend Setup & Execution

1. Navigate to the `backend/` directory:
   ```bash
   cd backend
   ```

2. Create and activate a Python virtual environment:
   ```bash
   # Windows
   python -m venv venv
   .\venv\Scripts\activate

   # macOS / Linux
   python3 -m venv venv
   source venv/bin/activate
   ```

3. Install required dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Configure environment variables in `backend/.env`:
   ```env
   OPENROUTER_API_KEY=your_openrouter_api_key
   GROQ_API_KEY=your_groq_api_key
   SECRET_KEY=your_random_jwt_secret_key
   DATABASE_URL=sqlite:///./prototyx.db
   # For PostgreSQL: postgresql://user:password@localhost:5432/prototyx
   ```

5. Run test suites to verify system health:
   ```bash
   python test_backend.py
   python test_auth.py
   ```

6. Start the development server:
   ```bash
   uvicorn app:app --host 0.0.0.0 --port 8000 --reload
   ```
   Interactive Swagger UI will be available at: `http://localhost:8000/docs`

---

### Step 3: Android Client Setup & Execution

1. Open Android Studio.
2. Select **Open** and select the `Prototyx/android` directory.
3. Allow Gradle to download dependencies and sync the project.
4. Verify server endpoint configuration in `RetrofitInstance.kt`:
   * By default, the app targets the live production cloud backend:  
     `https://prototyx.onrender.com/`
   * To switch to local development, update the base URL to your machine's local IP or Android emulator loopback:
     ```kotlin
     RetrofitInstance.updateBaseUrl("10.0.2.2:8000") // Android Emulator loopback
     ```
5. Select a connected device or an Android Virtual Device (AVD running API 24+) and click **Run** (`Shift + F10`).

---

## 🔒 Security & Privacy Engineering

* **Cryptographic Password Hashing**: Passwords stored on the server are hashed using standard `Bcrypt` with salt rounds via `passlib`. Plaintext passwords are never recorded.
* **Stateless JWT Authorization**: API sessions utilize time-delimited HMAC-SHA256 tokens. Protected endpoints enforce bearer token validation.
* **Encrypted Client Keystore**: Tokens on the mobile client are held in `EncryptedSharedPreferences`, utilizing the Android Keystore system with AES-256-GCM encryption.
* **Deterministic Fallbacks**: If external LLM gateways experience latency or rate limits, the backend gracefully falls back to deterministic local quantitative algorithms without crashing.

---

## 📚 Academic References

1. **Markowitz, H. (1952)**. *Portfolio Selection*. The Journal of Finance, 7(1), 77–91. [doi:10.1111/j.1540-6261.1952.tb01525.x](https://doi.org/10.1111/j.1540-6261.1952.tb01525.x)
2. **Black, F., & Litterman, R. (1992)**. *Global Portfolio Optimization*. Financial Analysts Journal, 48(5), 28–43. [doi:10.2469/faj.v48.n5.28](https://doi.org/10.2469/faj.v48.n5.28)
3. **FastAPI Project Documentation (2026)**. *FastAPI: Modern, High-Performance Web Framework for Python*. [fastapi.tiangolo.com](https://fastapi.tiangolo.com/)
4. **Google Android Developers (2026)**. *Jetpack Compose Architecture and Room Database Guidelines*. [developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose)
5. **OpenRouter API Documentation (2026)**. *Unified Interface for Large Language Models*. [openrouter.ai/docs](https://openrouter.ai/docs)
6. **Groq Cloud Documentation (2026)**. *LPU Inference Engine and API Reference*. [groq.com](https://groq.com)

---

## 📄 License

This project is developed for academic evaluation under SVKM's NMIMS MPSTME.  
Distributed under the **MIT License**. See `LICENSE` for more information.
