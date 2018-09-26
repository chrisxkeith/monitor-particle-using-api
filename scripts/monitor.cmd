rem ----- You may have to copy this script to a directory that is public to all users
rem ----- to make sure that it can be executed by Task Scheduler.
rem ----- Or you might have to replace %HOMEDRIVE%%HOMEPATH% with hard coded values.
rem ----- TODO : create tmp/ and put ret.ret file into it.

set h=%HOMEDRIVE%%HOMEPATH%
set o=%h%\Documents\tmp\monitor.log
set r=%h%\Documents\tmp\ret.ret
date < %r% >> %o% 2>&1
time < %r% >> %o% 2>&1
set >> %o% 2>&1
where mvn >> %o% 2>&1 
cd %h%\Documents\Github\monitor-particle-using-api >> %o% 2>&1 
mvn clean install exec:java -Dmaven.test.skip=true -Dexec.mainClass="com.ckkeith.monitor.Main" >> %o% 2>&1
