rem ----- Can be called from Task Scheduler

set h=%HOMEDRIVE%%HOMEPATH%
mkdir %h%\Documents\tmp\
set o=%h%\Documents\tmp\ret.ret
date < %o%
time < %o%
set >> %o% 2>&1 
where mvn >> %o% 2>&1 
cd %h%\Documents\Github\monitor-particle-using-api >> %o% 2>&1 
mvn clean install exec:java -Dexec.mainClass="com.ckkeith.monitor.Main" >> %o% 2>&1 
