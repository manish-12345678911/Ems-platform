@echo off
echo =====================================================================
echo  Starting All EMS Backend Microservices
echo =====================================================================

set "JAVA_BIN=C:\Program Files\Java\jdk-27\bin\java.exe"

echo Starting API Gateway (Port 8080)...
start "API Gateway (8080)" cmd /k ""%JAVA_BIN%" -jar "%~dp0api-gateway\target\api-gateway-1.0.0-SNAPSHOT.jar""

echo Starting Incident Service (Port 8081)...
start "Incident Service (8081)" cmd /k ""%JAVA_BIN%" -jar "%~dp0incident-service\target\incident-service-1.0.0-SNAPSHOT.jar""

echo Starting Dispatch Service (Port 8082)...
start "Dispatch Service (8082)" cmd /k ""%JAVA_BIN%" -jar "%~dp0dispatch-service\target\dispatch-service-1.0.0-SNAPSHOT.jar""

echo Starting Tracking Service (Port 8083)...
start "Tracking Service (8083)" cmd /k ""%JAVA_BIN%" -jar "%~dp0tracking-service\target\tracking-service-1.0.0-SNAPSHOT.jar""

echo Starting Routing Service (Port 8084)...
start "Routing Service (8084)" cmd /k ""%JAVA_BIN%" -jar "%~dp0routing-service\target\routing-service-1.0.0-SNAPSHOT.jar""

echo Starting Hospital Service (Port 8085)...
start "Hospital Service (8085)" cmd /k ""%JAVA_BIN%" -jar "%~dp0hospital-service\target\hospital-service-1.0.0-SNAPSHOT.jar""

echo Starting Redeployment Service (Port 8086)...
start "Redeployment Service (8086)" cmd /k ""%JAVA_BIN%" -jar "%~dp0redeployment-service\target\redeployment-service-1.0.0-SNAPSHOT.jar""

echo Starting Audit Service (Port 8087)...
start "Audit Service (8087)" cmd /k ""%JAVA_BIN%" -jar "%~dp0audit-service\target\audit-service-1.0.0-SNAPSHOT.jar""

echo.
echo All microservices launched in separate windows!
echo =====================================================================
