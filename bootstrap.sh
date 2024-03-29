#! /bin/bash
# Select all, copy to temporary shell script in temporary directory, then run it.

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
# Edit the following line to chris-keith-gmail-com to your (modified) particle account name.
mkdir -p ~/Documents/tmp/${HOSTNAME}/chris-keith-gmail-com				; if [ $? -ne 0 ] ; then exit -6 ; fi
mkdir -p ~/Documents/Github												; if [ $? -ne 0 ] ; then exit -6 ; fi
cd ~/Documents/Github													; if [ $? -ne 0 ] ; then exit -6 ; fi
rm -rf JParticle/														; if [ $? -ne 0 ] ; then exit -6 ; fi
git clone https://github.com/Walter-Stroebel/JParticle.git				; if [ $? -ne 0 ] ; then exit -6 ; fi

git clone https://github.com/chrisxkeith/monitor-particle-using-api.git	; if [ $? -ne 0 ] ; then exit -6 ; fi
cp -R JParticle/src/* monitor-particle-using-api/src/					; if [ $? -ne 0 ] ; then exit -6 ; fi
cd ~/Documents/Github/monitor-particle-using-api	  					; if [ $? -ne 0 ] ; then exit -6 ; fi
mkdir -p src/main/resources/com/ckkeith/monitor/	  					; if [ $? -ne 0 ] ; then exit -6 ; fi

# echo "Now copy your GCP client_secret.json/credentials.json files into:"
# echo "~/Documents/Github/monitor-particle-using-api/src/main/resources/com/ckkeith/monitor/"
echo "Copy runparams.xml into ~/Documents/tmp/your-machine-name/your-munged-particle-account-name"
echo "Then run : ~/Documents/Github/monitor-particle-using-api/scripts/run.sh"
# echo "You should get a browser window asking you to verify your credentials."
echo "After that (in a couple of minutes), you should see output something like:"
echo "2019-05-10T10:04:48-07  chris.keith@gmail.com   AccountMonitor thread starting."
echo "Use Ctrl-C (Cmd-C? on mac) to quit."
