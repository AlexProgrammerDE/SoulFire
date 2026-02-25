@echo off
setlocal

if not "%SF_PWD%"=="" (
  cd /d "%SF_PWD%"
)

if "%SF_RAM%"=="" (
  echo SF_RAM is not set. Defaulting to 2G.
  set "SF_RAM=2G"
)

if "%SF_JAR%"=="" (
  echo SF_JAR is not set. Building and locating locally built jar...
  call .\gradlew.bat :dedicated-launcher:uberJar --no-daemon --max-workers=1
  if errorlevel 1 (
    echo Error: Gradle build failed.
    exit /b 1
  )

  for /f "delims=" %%F in ('dir /b /a-d /o-d "dedicated-launcher\build\libs\SoulFireDedicated-*.jar" 2^>nul') do (
    echo %%F | findstr /I /C:"unshaded" /C:"sources" /C:"javadoc" >nul
    if errorlevel 1 if "%SF_JAR%"=="" set "SF_JAR=dedicated-launcher\build\libs\%%F"
  )
)

if "%SF_JAR%"=="" (
  echo Error: Could not find built jar in dedicated-launcher\build\libs.
  echo Please run '.\gradlew build' first or set the SF_JAR environment variable.
  exit /b 1
)

if not exist "%SF_JAR%" (
  echo Error: Unable to access jarfile "%SF_JAR%"
  exit /b 1
)

echo Starting SoulFire dedicated server...
java -Xmx%SF_RAM% %SF_JVM_FLAGS% -XX:+EnableDynamicAgentLoading -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:+UseCompactObjectHeaders -XX:+AlwaysActAsServerClassMachine -XX:+UseNUMA -XX:+UseFastUnorderedTimeStamps -XX:+UseVectorCmov -XX:+UseCriticalJavaThreadPriority -Dsf.flags.v2=true -jar "%SF_JAR%"
