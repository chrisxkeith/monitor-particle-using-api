rem ----- You may have to copy this script to a directory that is public to all users
rem ----- to make sure that it can be executed by Task Scheduler.
rem ----- Or you might have to replace %HOMEDRIVE%%HOMEPATH% with hard coded values.

set userhome=%HOMEDRIVE%%HOMEPATH%
mkdir %userhome%\Documents\tmp\%COMPUTERNAME%
set log=%userhome%\Documents\tmp\%COMPUTERNAME%\monitor.log

echo %DATE% %TIME% >> %log%
set >> %log%

pushd %userhome%\Documents\Github\monitor-particle-using-api
echo About to git pull >> %log%
git pull 2>&1 >> %log%
echo About call mvn install... >> %log%
call mvn clean install exec:java -D maven.test.skip=true -D exec.mainClass="com.ckkeith.monitor.Main" 2>&1 >> %log%
echo %DATE% %TIME% >> %log%
echo mvn clean install... finished >> %log%
popd
