@echo off
echo Starting Spring Boot Chat Application Backend...
echo.

echo Setting up environment...
set JAVA_OPTS=-Xmx512m -Dspring.profiles.active=dev

echo Creating uploads directory if it doesn't exist...
if not exist "uploads" mkdir uploads

echo.
echo Compiling and running Spring Boot application...
echo This may take a moment on first run...
echo.

REM Try to run with Maven if available
where mvn >nul 2>&1
if %ERRORLEVEL% == 0 (
    echo Using Maven...
    mvn spring-boot:run
) else (
    echo Maven not found. Trying with Gradle...
    where gradle >nul 2>&1
    if %ERRORLEVEL% == 0 (
        gradle bootRun
    ) else (
        echo Neither Maven nor Gradle found.
        echo.
        echo Please install Maven or compile the project first, then run:
        echo java -jar target/chat-application-1.0.0.jar
        echo.
        echo For now, trying to run with direct Java compilation...
        
        REM Try to compile with javac directly
        echo Attempting direct Java compilation...
        if not exist "classes" mkdir classes
        
        REM This is a simplified approach - in reality, we'd need all dependencies
        echo Note: This approach requires all Spring Boot dependencies in classpath
        echo Please install Maven for proper dependency management
        pause
    )
)

echo.
echo Backend should be running on http://localhost:8080
echo Press any key to exit...
pause > nul 