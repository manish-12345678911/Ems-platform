@echo off
setlocal
echo ========================================================
echo  Push Module to Personal GitHub Repository
echo ========================================================
echo.

set /p REPO_URL="Enter your GitHub Repository URL (e.g. https://github.com/username/repo-name.git): "

if "%REPO_URL%"=="" (
    echo [ERROR] No repository URL entered. Exiting.
    pause
    exit /b 1
)

echo.
echo [1/5] Initializing Git repository...
if not exist ".git" (
    git init
)

echo [2/5] Staging module files...
git add .

echo [3/5] Creating commit...
git commit -m "feat: complete module implementation and documentation"

echo [4/5] Setting main branch...
git branch -M main

echo [5/5] Configuring remote and pushing...
git remote remove origin >nul 2>&1
git remote add origin %REPO_URL%
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================================
    echo  SUCCESS! Your module has been pushed to GitHub.
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo  [NOTE] If push failed due to authentication:
    echo  Ensure you have write access or push from your own machine,
    echo  or use a Personal Access Token (PAT):
    echo  https://<TOKEN>@github.com/<username>/<repo>.git
    echo ========================================================
)

pause
