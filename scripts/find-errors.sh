# Must first use File Explorer to show file system of remote machine.

set -x

mkdir -p ~/Documents/tmp/2018-CK-NUC/                           		    ; if [ $? -ne 0 ] ; then exit -6 ; fi
cd ~/Documents/tmp/2018-CK-NUC/							                    ; if [ $? -ne 0 ] ; then exit -6 ; fi
# cp '\\2018-CK-NUC\Users\chris\Documents\tmp\2018-CK-NUC\monitor01.log' .    ; if [ $? -ne 0 ] ; then exit -6 ; fi
grep -n xception monitor01.log | tail -20
cp '\\2018-CK-NUC\Users\chris\Documents\tmp\2018-CK-NUC\monitor.log' .	    ; if [ $? -ne 0 ] ; then exit -6 ; fi
grep -n xception monitor.log | tail -20
