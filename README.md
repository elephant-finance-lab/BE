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

