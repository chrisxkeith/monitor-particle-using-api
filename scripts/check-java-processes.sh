while [ 1 ] ; do
	now=`date`
	nJavaProcesses=`ps -fa | grep java | wc -l`
	echo "$now $nJavaProcesses"
	sleep 30
done
