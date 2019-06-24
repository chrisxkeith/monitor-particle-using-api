# ... in process, grep doesn't display to git bash console.

# Must shut down Visual Studio code or any other processes that might be running Java.
set -x
if [ \( "Windows_NT" != "$OS" \) -o \( "MINGW64" != "$MSYSTEM" \) ] ; then
    echo "Only runs in git bash (MINGW64) on Windows."
    exit -666
fi

javaTaskCount=`tasklist //fo csv //v | grep -i java | wc -l`
if [ "$javaTaskCount" -ne 1 ] ; then
    echo "More or less than one java process found."
    exit -666
fi

while [ 1 ] ; do
	date
	wmic /output:c:\\users\\chris\\Documents\\proc.csv process get commandline,PeakVirtualSize,PeakWorkingSetSize /format:csv
	grep -i c:\\users\\chris\\Documents\\proc.csv java | cut -d, -f3
	sleep `expr 10` # 60 \* 10
done
