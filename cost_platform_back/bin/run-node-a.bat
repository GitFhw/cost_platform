@echo off
echo.
echo [info] start cost platform node A
echo.

cd %~dp0
cd ../cost_admin/target

set JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java %JAVA_OPTS% -jar cost_admin.jar --spring.profiles.active=druid,node-a

cd ../../bin
pause
