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

After a couple of minutes, you should see output something like:
...
Jun 06, 2018 9:25:52 AM com.ckkeith.monitor.PhotonMonitor run
INFO: Logging to C:\Users\chris\Documents\tmp\CK_particle_log.txt
2018-06-06T09:25:52-07  CK : PhotonMonitor thread starting up.
2018-06-06T09:25:52-07  thermistor-test         1c002c001147343438323536        false   Tue May 29 17:05:00 PDT 2018    unknown (no variables)
...
Use Ctrl-C (Cmd-C? on mac) to quit (if-and-when you want).
