set port=%1
set jar=%2

set fileName=auto.properties

cd.> result.txt

echo render-to-screen=false > %fileName%
echo render-to-screen-sync=false >> %fileName%
echo render-to-screen-size=640x480 >> %fileName%
echo render-to-screen-tick= >> %fileName%
echo results-file=result.txt >> %fileName%
echo log-file=game.log >> %fileName%

echo p1-type=Local >> %fileName%
echo p2-type=Quick >> %fileName%

echo p1-name=%jar% >> %fileName%
echo p2-name=Quick >> %fileName%

echo p1-startup-command= >> %fileName%
echo p2-startup-command= >> %fileName%

echo base-adapter-port=%port% >> %fileName%

echo facilities=false >> %fileName%

echo fog-of-war=false >> %fileName%
echo fog-of-war-quality=2 >> %fileName%

echo seed= >> %fileName%
echo plugins-directory= >> %fileName%


ping -n 2 127.0.0.1 >nul
start javaw -jar "local-runner.jar" %fileName%

ping -n 2 127.0.0.1 >nul
start D:\jre1.8.0_151\bin\java -jar C:\Workspace\aicup2017\local-runner\%jar%.jar 127.0.0.1 %port% 0000000000000000