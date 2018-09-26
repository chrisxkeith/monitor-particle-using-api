cd ~/Documents/Github/monitor-particle-using-api/
															  if [ $? -ne 0 ] ; then exit -6 ; fi
mvn clean install exec:java -Dmaven.test.skip=true -Dexec.mainClass="com.ckkeith.monitor.Main" | \
 tee -a ~/Documents/tmp/monitor.log
