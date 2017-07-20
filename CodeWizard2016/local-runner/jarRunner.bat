set /a port = %1
set /a port2 = %port% + 1
set /a port3 = %port% + 2
set /a port4 = %port% + 3
set /a port5 = %port% + 4
set /a port6 = %port% + 5
set /a port7 = %port% + 6
set /a port8 = %port% + 7
set /a port9 = %port% + 8
set /a port10 = %port% + 9

set jar=%2
set jar2=%3
set jar3=%4
set jar4=%5
set jar5=%6
set jar6=%7
set jar7=%8
set jar8=%9
set jar9=%9
set jar10=%9

set skills=false
set /a playerCount=10

cd.> result.txt

set fileName=auto.properties

echo render-to-screen=false > %fileName%
echo render-to-screen-sync=false >> %fileName%
echo render-to-screen-size=640x480 >> %fileName%
echo render-to-screen-tick= >> %fileName%
echo results-file=result.txt >> %fileName%
echo log-file=game.log >> %fileName%
echo player-count=%playerCount% >> %fileName%

echo p1-type=Local >> %fileName%
echo p2-type=Local >> %fileName%
echo p3-type=Local >> %fileName%
echo p4-type=Local >> %fileName%
echo p5-type=Local >> %fileName%
echo p6-type=Local >> %fileName%
echo p7-type=Local >> %fileName%
echo p8-type=Local >> %fileName%
echo p9-type=Local >> %fileName%
echo p10-type=Local >> %fileName%

echo p1-name=%jar% >> %fileName%
echo p2-name=%jar2% >> %fileName%
echo p3-name=%jar3% >> %fileName%
echo p4-name=%jar4% >> %fileName%
echo p5-name=%jar5% >> %fileName%
echo p6-name=%jar6% >> %fileName%
echo p7-name=%jar7% >> %fileName%
echo p8-name=%jar8% >> %fileName%
echo p9-name=%jar9% >> %fileName%
echo p10-name=%jar10% >> %fileName%


echo p1-startup-command= >> %fileName%
echo p2-startup-command= >> %fileName%
echo p3-startup-command= >> %fileName%
echo p4-startup-command= >> %fileName%
echo p5-startup-command= >> %fileName%
echo p6-startup-command= >> %fileName%
echo p7-startup-command= >> %fileName%
echo p8-startup-command= >> %fileName%
echo p9-startup-command= >> %fileName%
echo p10-startup-command= >> %fileName%

echo base-adapter-port=%port% >> %fileName%
echo skills-enabled=%skills% >> %fileName%
echo seed= >> %fileName%
echo plugins-directory= >> %fileName%


ping -n 2 127.0.0.1 >nul
start javaw -jar "local-runner.jar" %fileName%

ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar%.jar 127.0.0.1 %port% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar2%.jar 127.0.0.1 %port2% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar3%.jar 127.0.0.1 %port3% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar4%.jar 127.0.0.1 %port4% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar5%.jar 127.0.0.1 %port5% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar6%.jar 127.0.0.1 %port6% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar7%.jar 127.0.0.1 %port7% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar8%.jar 127.0.0.1 %port8% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar9%.jar 127.0.0.1 %port9% 0000000000000000
ping -n 2 127.0.0.1 >nul
start D:\Distributions\Develop\jre1.8.0_101\bin\java -jar C:\Workspace\Java\CodeWizards2016\local-runner\%jar10%.jar 127.0.0.1 %port10% 0000000000000000