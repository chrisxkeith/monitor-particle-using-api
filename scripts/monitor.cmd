rem ----- You may have to copy this script to a directory that is public to all users
rem ----- to make sure that it can be executed by Task Scheduler.
rem ----- Or you might have to replace %HOMEDRIVE%%HOMEPATH% with hard coded values.
rem
rem ----- How to use powershell to make a tee
rem ----- https://stackoverflow.com/questions/796476/displaying-windows-command-prompt-output-and-redirecting-it-to-a-file

set userhome=%HOMEDRIVE%%HOMEPATH%
mkdir %userhome%\Documents\tmp\%COMPUTERNAME%
set log=%userhome%\Documents\tmp\%COMPUTERNAME%\monitor.log

rem ----- Put timestamp in log
echo %DATE% %TIME% >> %log%
set >> %log%
where mvn >> %log%

pushd %userhome%\Documents\Github\monitor-particle-using-api
mvn clean install exec:java -D maven.test.skip=true -D exec.mainClass="com.ckkeith.monitor.Main" 2>&1 >> %log%
popd
