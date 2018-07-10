#! /bin/bash
# Select all, copy to temporary shell script in temporary directory, then run it.
# After a couple of minutes, you should see output something like:
# 2018-07-10T12:27:52-07  someone@somewhere.io     PhotonMonitor thread starting.
# ...
# Use Ctrl-C (Cmd-C? on mac) to quit.

mvn --version
if [ $? -ne 0 ] ; then
	echo "Please install maven."
	exit -2
fi
echo ""

if [ ! -f ~/Documents/particle-tokens.txt ] ; then
	echo "Please create ~/Documents/particle-tokens.txt containing one or more lines with <token><tab><accountName>"
	exit -2
fi

if [ -d ~/Documents/Github/monitor-particle-using-api ] ; then
	echo "Please rename ~/Documents/Github/monitor-particle-using-api"
	exit -2
fi

set -x
mkdir -p ~/Documents/Github												; if [ $? -ne 0 ] ; then exit -6 ; fi
cd ~/Documents/Github													; if [ $? -ne 0 ] ; then exit -6 ; fi
rm -rf JParticle/														; if [ $? -ne 0 ] ; then exit -6 ; fi
git clone https://github.com/Walter-Stroebel/JParticle.git				; if [ $? -ne 0 ] ; then exit -6 ; fi

git clone https://github.com/chrisxkeith/monitor-particle-using-api.git	; if [ $? -ne 0 ] ; then exit -6 ; fi
cp -R JParticle/src/* monitor-particle-using-api/src/					; if [ $? -ne 0 ] ; then exit -6 ; fi
cd ~/Documents/Github/monitor-particle-using-api	  					; if [ $? -ne 0 ] ; then exit -6 ; fi

mvn clean install exec:java -Dexec.mainClass="com.ckkeith.monitor.Main"	; if [ $? -ne 0 ] ; then exit -6 ; fi
