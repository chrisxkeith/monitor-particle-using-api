#! /bin/bash
#set -x

if [ -d ~/Documents/Github/monitor-particle-using-api ] ; then
	echo "Please rename ~/Documents/Github/monitor-particle-using-api"
	exit -2
fi

if [ ! -f ~/Documents/particle-tokens.txt ] ; then
	echo "Please create ~/Documents/particle-tokens.txt containing tokens."
	exit -2
fi

cd ~/Documents/Github													; if [ $? -ne 0 ] ; then exit -6 ; fi
rm -rf JParticle/														; if [ $? -ne 0 ] ; then exit -6 ; fi
git clone https://github.com/Walter-Stroebel/JParticle.git				; if [ $? -ne 0 ] ; then exit -6 ; fi

git clone https://github.com/chrisxkeith/monitor-particle-using-api.git	; if [ $? -ne 0 ] ; then exit -6 ; fi
cp -R JParticle/src/* monitor-particle-using-api/src/					; if [ $? -ne 0 ] ; then exit -6 ; fi
cd ~/Documents/Github/monitor-particle-using-api	  					; if [ $? -ne 0 ] ; then exit -6 ; fi

mvn clean install exec:java -Dexec.mainClass="com.ckkeith.monitor.Main"	; if [ $? -ne 0 ] ; then exit -6 ; fi
exit 0

You should see output something like:
name                    id                              connect lastHeard
Candlestick             40002a001847343438323536        true    Tue Jun 05 09:00:55 PDT 2018
... then use Ctrl-C (Cmd-C? on mac) to quit.