# ElephantFinance BE (local)

## Google login (local only)

### 1) Create Google OAuth credentials

- Create a project in Google Cloud Console
- Enable APIs if prompted
- Create **OAuth 2.0 Client ID** (Application type: **Web application**)
- Add Authorized redirect URI:
  - `http://localhost:8080/login/oauth2/code/google`

### 2) Set environment variables (Windows PowerShell)

```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
```

Also set DB vars used by `src/main/resources/application.yml`:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/your_db"
$env:DB_USER="root"
$env:DB_PW="your_password"
```

### 3) Run locally

```powershell
cd "c:\ElephantFinance\BE"
.\gradlew bootRun
```

### 4) Test the flow

- Open `http://localhost:8080/`
- Start Google login: `http://localhost:8080/oauth2/authorization/google`
- After login, check user info: `http://localhost:8080/me`

## Paper Auto Trading gRPC Integration

This branch controls the AI paper-auto session through gRPC and stores only
`AutoTradingSession` state in BE. KIS orders stay in AI and use the shared
virtual account configured in the AI process.

### 1) Run the AI gRPC server

Set the AI process to the KIS virtual account only. The exact bundle ID must
be a paper-auto ready bundle in the AI repository.

```bash
cd ../AI
export KIS_MODE=virtual
export KIS_PAPER_APP_KEY="<paper-app-key>"
export KIS_PAPER_APP_SECRET="<paper-app-secret>"
export KIS_PAPER_ACCOUNT_NUMBER="<paper-account-number>"
export KIS_PAPER_ACCOUNT_PRODUCT_CODE="<paper-product-code>"

python -m pip install -r requirements.txt
PYTHONPATH=new python new/scripts/generate_ai_grpc_stubs.py
PYTHONPATH=new python new/scripts/run_ai_grpc_server.py \
  --host 127.0.0.1 \
  --port 50051 \
  --bundle-id "<paper-ready-bundle-id>"
```

### 2) Configure and run BE

Keep `AI_PAPER_BUNDLE_ID` equal to the bundle passed to the AI server, or
leave it blank and rely on the AI server `--bundle-id`.

```bash
cd ../BE
export AI_SERVER_HOST=localhost
export AI_SERVER_PORT=50051
export AI_SERVER_TIMEOUT=5
export AI_PAPER_BUNDLE_ID="<paper-ready-bundle-id>"
export AI_PAPER_CONFIRM_PHRASE=PAPER_AUTO_OK

./gradlew generateProto
./gradlew bootRun
```

The existing datasource, Redis, JWT, OAuth and KIS display/market-data
settings in `application.yml` are still required for local BE startup.

### 3) Test with Swagger or curl

Authenticate first and send the access token as `Authorization: Bearer
<access-token>`. Before starting a session, store selected recommendations:

```bash
curl -X POST http://localhost:8080/api/recommendations/select \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"selectedRecommendations":[{"recommendationId":1},{"recommendationId":2}]}'
```

Start a paper-auto session:

```bash
curl -X POST http://localhost:8080/api/auto-trading/sessions \
  -H "Authorization: Bearer <access-token>" \
  -H "Idempotency-Key: demo-paper-auto-001" \
  -H "Content-Type: application/json" \
  -d '{"recommendationIds":[1,2],"purchaseOptionId":2,"cycles":3,"intervalSec":10}'
```

Repeat the same `Idempotency-Key` to receive the stored session without
calling AI Start again. Use the returned BE `sessionId` below:

```bash
curl -H "Authorization: Bearer <access-token>" \
  http://localhost:8080/api/auto-trading/sessions/<sessionId>

curl -H "Authorization: Bearer <access-token>" \
  http://localhost:8080/api/auto-trading/sessions/<sessionId>/ai-status

curl -X POST -H "Authorization: Bearer <access-token>" \
  http://localhost:8080/api/auto-trading/sessions/<sessionId>/stop
```

`GET /ai-status` compares the AI-generated session ID with the BE session's
stored `aiSessionId`. In this first integration, it also reconciles a stopped
session to `STOPPED` and a finished running session to `COMPLETED`, releasing
the shared-account active slot.

With `spring.jpa.hibernate.ddl-auto=update`, inspect the generated
`auto_trading_sessions` table to verify `STARTING`, `RUNNING`, `STOPPING`,
`STOPPED`, `FAILED`, or `COMPLETED` state changes.

### 4) Failure and excluded behavior

- If the AI gRPC server is down, session creation persists a `FAILED` row and
  the REST response uses the existing `AI503_01` error handling.
- AI currently exposes global paper-auto status without taking a session ID,
  so BE compares the response's AI session ID before reconciliation.
- AI status does not distinguish a completed run from an asynchronous failed
  run; detailed failure reconciliation belongs with the later Kafka event
  integration.
- Kafka consumer, Kafka event persistence, order/fill persistence, STOMP
  notifications, Notification tables, FE wiring and BE-side KIS order
  execution are intentionally excluded from this branch.
