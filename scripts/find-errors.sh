set -x

cd ~/Documents/tmp/${HOSTNAME}/							                    ; if [ $? -ne 0 ] ; then exit -6 ; fi
grep xception monitor*.log
grep 'AccountMonitor thread starting' monitor*.log
