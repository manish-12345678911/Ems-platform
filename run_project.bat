@echo off
echo =====================================================================
echo  H8 EMS Tactical Platform - Automated Deployment and Startup Script
echo =====================================================================
echo.

echo [1/3] Checking Docker containers (PostgreSQL, Kafka, Redis, Keycloak)...
docker-compose up -d postgres redis kafka keycloak
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] Docker-compose returned a warning. Ensure Docker Desktop is running.
) else (
    echo [OK] Core infrastructure containers are active.
)
echo.

echo [2/3] Starting Unified Web + API Proxy Server on Port 8088...
start "H8 EMS Unified Server (Port 8088)" cmd /k "python %~dp0server.py"
echo [OK] Unified server started at http://localhost:8088
echo.

echo [3/3] Deployment Overview:
echo  - Landing Platform:     http://localhost:8088/
echo  - Dispatcher Command:   http://localhost:8088/dispatcher/
echo  - Paramedic Crew PWA:   http://localhost:8088/crew/
echo  - Hospital ED Hub:      http://localhost:8088/ed/
echo  - Central API Gateway:  http://localhost:8080/  (Spring Boot services)
echo.
echo  API calls from the web pages are proxied through :8088 to :8080
echo  No CORS issues - everything goes through the unified server.
echo.
echo =====================================================================
echo  System is RUNNING and Ready!
echo =====================================================================
pause
