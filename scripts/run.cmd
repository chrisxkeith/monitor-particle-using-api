rem : Not fully tested...
pushd %HOMEDRIVE%%HOMEPATH%\Documents\Github\monitor-particle-using-api\
                                                        	IF %ERRORLEVEL% NEQ 0 goto :theend
mkdir -p %HOMEDRIVE%%HOMEPATH%\Documents\tmp\%HOSTNAME%
                                                        	IF %ERRORLEVEL% NEQ 0 goto :theend
powershell "mvn clean install exec:java -Dmaven.test.skip=true -Dexec.mainClass=`"com.ckkeith.monitor.Main`" | ^
	tee -a %HOMEDRIVE%%HOMEPATH%\Documents\tmp\%HOSTNAME%\monitor.log"
