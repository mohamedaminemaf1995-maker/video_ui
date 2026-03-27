@echo off

echo ==============================
echo 1. Generation des thumbnails
echo ==============================

cd /d D:\workspaces\demo

REM Lancer Python
python thumbnail.py

IF %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Le script Python a echoue !
    pause
    exit /b %ERRORLEVEL%
)

echo ==============================
echo 2. Lancement Spring Boot
echo ==============================

java -jar D:\workspaces\demo\target\demo-0.0.1-SNAPSHOT.jar --server.port=8080

pause