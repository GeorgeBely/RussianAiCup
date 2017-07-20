set /a port = %1
set /a port2 = %port% + 1
set /a port3 = %port% + 2
set /a port4 = %port% + 3

set jar=%2
set jar2=%3
set jar3=%4
set jar4=%5
set map=%6
set jeep=%7
set collision=%8
set /a playerCount=%9
set /a carCount= 4/%playerCount%

cd.> result.txt

set fileName=auto.properties

echo render-to-screen=false > %fileName%
echo render-to-screen-sync=false >> %fileName%
echo render-to-screen-size=640x480 >> %fileName%
echo render-to-screen-tick= >> %fileName%
echo results-file=result.txt >> %fileName%
echo log-file=game.log >> %fileName%
echo team-size=%carCount% >> %fileName%
echo player-count=%playerCount% >> %fileName%
echo p1-type=Local >> %fileName%
echo p2-type=Local >> %fileName%
echo p3-type=Local >> %fileName%
echo p4-type=Local >> %fileName%
echo p1-name=%jar% >> %fileName%
echo p2-name=%jar2% >> %fileName%
echo p3-name=%jar3% >> %fileName%
echo p4-name=%jar4% >> %fileName%
echo loose-map-check=true >> %fileName%
echo swap-car-types=%jeep% >> %fileName%
echo disable-car-collision=%collision% >> %fileName%
echo map=%map% >> %fileName%
echo base-adapter-port=%port% >> %fileName%
echo seed= >> %fileName%
echo plugins-directory= >> %fileName%

ping -n 2 127.0.0.1 >nul
start javaw -jar "local-runner.jar" %fileName%

ping -n 2 127.0.0.1 >nul
start D:\Java\jdk1.8.0_45\bin\java -jar C:\Users\gbeliy\IdeaProjects\AiCupRacing\out\artifacts\java_cgdk_jar\%jar%.jar 127.0.0.1 %port% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Java\jdk1.8.0_45\bin\java -jar C:\Users\gbeliy\IdeaProjects\AiCupRacing\out\artifacts\java_cgdk_jar\%jar2%.jar 127.0.0.1 %port2% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Java\jdk1.8.0_45\bin\java -jar C:\Users\gbeliy\IdeaProjects\AiCupRacing\out\artifacts\java_cgdk_jar\%jar3%.jar 127.0.0.1 %port3% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Java\jdk1.8.0_45\bin\java -jar C:\Users\gbeliy\IdeaProjects\AiCupRacing\out\artifacts\java_cgdk_jar\%jar4%.jar 127.0.0.1 %port4% 0000000000000000