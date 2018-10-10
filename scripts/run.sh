nJavaProcesses=`ps -fa | grep java | wc -l`
if [ $nJavaProcesses -gt 0 ] ; then
	echo "Already running some Java processes."
	ps -fs | grep java
	exit -1
fi
cd ~/Documents/Github/monitor-particle-using-api/
															  if [ $? -ne 0 ] ; then exit -6 ; fi
account=chris-keith-gmail-com
set -x
rm -f ~/Documents/tmp/${account}/*.html

mvn clean install exec:java -Dmaven.test.skip=true -Dexec.mainClass="com.ckkeith.monitor.Main" | \
 tee -a ~/Documents/tmp/monitor.log
