cd ~/Documents/Github/monitor-particle-using-api/
															  if [ $? -ne 0 ] ; then exit -6 ; fi
account=chris-keith-gmail-com
rm -f ~/Documents/tmp/${account}/*.html

mvn clean install exec:java -Dmaven.test.skip=true -Dexec.mainClass="com.ckkeith.monitor.Main" | \
 tee -a ~/Documents/tmp/monitor.log &

set -x
while [ ! -f ~/Documents/tmp/${account}/all000.html ] ; do
	date
	sleep 1
done
'/c/Program Files (x86)/Google/Chrome/Application/chrome.exe' "${HOMEDRIVE}${HOMEPATH}\\Documents\\tmp\\${account}\\all000.html" &
