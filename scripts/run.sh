nJavaProcesses=`ps -fa | grep java | wc -l`
if [ $nJavaProcesses -gt 0 ] ; then
	echo "Already running some Java processes."
	ps -fs | grep java
	exit -1
fi

cd ~/Documents/Github/monitor-particle-using-api/				; if [ $? -ne 0 ] ; then exit -6 ; fi

mkdir -p ~/Documents/tmp/${HOSTNAME}							; if [ $? -ne 0 ] ; then exit -6 ; fi
mvn clean install exec:java -Dmaven.test.skip=true \
	-Dexec.mainClass="com.ckkeith.monitor.Main" | \
	tee -a ~/Documents/tmp/${HOSTNAME}/monitor.log				; if [ $? -ne 0 ] ; then exit -6 ; fi
