rem ----- You may have to copy this script to a directory that is public to all users
rem ----- to make sure that it can be executed by Task Scheduler.
rem ----- Or you might have to replace %HOMEDRIVE%%HOMEPATH% with hard coded values.
rem ----- TODO : create tmp/ and put ret.ret file into it.
rem
rem How to use powershell to make a tee
rem https://stackoverflow.com/questions/796476/displaying-windows-command-prompt-output-and-redirecting-it-to-a-file

set userhome=%HOMEDRIVE%%HOMEPATH%
set log=%userhome%\Documents\tmp\monitor.log
set file_containing_return=%userhome%\Documents\tmp\ret.ret
set account=chris-keith-gmail-com

rem Put timestamp in log
date < %file_containing_return% >> %log% 2>&1
time < %file_containing_return% >> %log% 2>&1
set >> %log% 2>&1
echo powershell "where mvn 2>&1 | tee -Append %log%"

echo powershell "del /f /q %userhome%\Documents\tmp\%account%\*.html 2>&1 | tee -Append %log%"

pushd %userhome%\Documents\Github\monitor-particle-using-api
echo powershell ".\mvn clean install exec:java -Dmaven.test.skip=true -Dexec.mainClass="com.ckkeith.monitor.Main" 2>&1 | tee -Append %log%"
popd
