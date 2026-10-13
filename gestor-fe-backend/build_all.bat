@echo off
echo ========================================================
echo Compilando todos los microservicios de Gestor FE Backend
echo ========================================================

echo.
echo [1/6] Compilando service-common...
cd /d "%~dp0\service-common"
call mvn clean install -DskipTests=true
if %ERRORLEVEL% NEQ 0 ( echo Error en service-common && exit /b %ERRORLEVEL% )

echo.
echo [2/6] Compilando gestor-fe-eureka-server...
cd /d "%~dp0\gestor-fe-eureka-server"
call mvn clean package -DskipTests=true
if %ERRORLEVEL% NEQ 0 ( echo Error en gestor-fe-eureka-server && exit /b %ERRORLEVEL% )

echo.
echo [3/6] Compilando gestor-fe-config-server...
cd /d "%~dp0\gestor-fe-config-server"
call mvn clean package -DskipTests=true
if %ERRORLEVEL% NEQ 0 ( echo Error en gestor-fe-config-server && exit /b %ERRORLEVEL% )

echo.
echo [4/6] Compilando gestor-fe-gateway...
cd /d "%~dp0\gestor-fe-gateway"
call mvn clean package -DskipTests=true
if %ERRORLEVEL% NEQ 0 ( echo Error en gestor-fe-gateway && exit /b %ERRORLEVEL% )

echo.
echo [5/6] Compilando gestor-fe-admin-server...
cd /d "%~dp0\gestor-fe-admin-server"
call mvn clean package -DskipTests=true
if %ERRORLEVEL% NEQ 0 ( echo Error en gestor-fe-admin-server && exit /b %ERRORLEVEL% )

echo.
echo [6/6] Compilando gestor-fe-admin-service y core-service...
cd /d "%~dp0\gestor-fe-admin-service"
call mvn clean package -DskipTests=true
if %ERRORLEVEL% NEQ 0 ( echo Error en gestor-fe-admin-service && exit /b %ERRORLEVEL% )

cd /d "%~dp0\gestor-fe-core-service"
call mvn clean package -DskipTests=true
if %ERRORLEVEL% NEQ 0 ( echo Error en gestor-fe-core-service && exit /b %ERRORLEVEL% )

echo.
echo ========================================================
echo TODOS LOS MICROSERVICIOS FUERON COMPILADOS EXITOSAMENTE!
echo ========================================================
cd /d "%~dp0"
