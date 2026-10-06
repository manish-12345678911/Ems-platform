@echo off
title H8 EMS - Pushkar Module (Security & Auth Gate Testbed)
echo ==========================================================
echo  H8 EMS - MODULE 02: SECURITY, RBAC & HIPAA PHONE ANONYMIZER
echo  Author: Pushkar (Security & Authorization Engineer)
echo ==========================================================
echo.
echo Launching standalone security testbed on port 8082...
echo Testbed will open at: http://localhost:8082/login-demo.html
echo.
start "" http://localhost:8082/login-demo.html
python -m http.server 8082 --directory src\auth-ui
pause
