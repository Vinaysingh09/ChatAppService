@echo off
title Chat Application - Stable Runner
echo.
echo ==========================================
echo 🚀 Starting Chat Application (Stable Mode)
echo ==========================================
echo.

echo 🔧 Checking Java version...
java -version
echo.

echo 🔧 Setting JVM options for stability...
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError

echo 🔧 Setting Spring Boot options...
set SPRING_OPTS=--spring.main.keep-alive=true --logging.level.root=INFO

echo.
echo 🚀 Starting application...
echo 📡 Server will be available at: http://localhost:8081
echo 🔌 WebSocket endpoint: ws://localhost:8081/ws
echo 💡 Press Ctrl+C to stop the application
echo.

REM Kill any existing process on port 8081
echo 🧹 Cleaning up any existing processes on port 8081...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8081') do (
    taskkill /F /PID %%a 2>nul
)

echo.
echo ▶️ Running: java %JAVA_OPTS% -jar target/chat-application-1.0.0.jar %SPRING_OPTS%
echo.

java %JAVA_OPTS% -jar target/chat-application-1.0.0.jar %SPRING_OPTS%

echo.
echo 🛑 Application stopped.
pause 