# monitor-particle-using-api

Install Git client, Java JDK and maven as described here : https://github.com/chrisxkeith/commute-time-aggregator/blob/master/README.md . You can also use Visual Studio Code for development instead of Spring Tool Suite. 

See https://github.com/chrisxkeith/monitor-particle-using-api/blob/master/bootstrap.sh for build steps. If you want to run the script directly, copy it into a temporary directory and run from that directory using Git Bash.

You will need to create and install a Google Cloud Platform credential file (e.g., client_secret.json) in order for the email functionality to work. The file should be put into the src/main/resources/com/ckkeith/monitor/ directory.

You will also need to create a runparams.xml file. See https://github.com/chrisxkeith/monitor-particle-using-api/blob/master/src/main/java/com/ckkeith/monitor/RunParams.java .

Email addresses (To: and From:) are currently hardcoded. You will need to find them (search for chris.keith@gmail.com) and change them.

Assumes that you have flashed https://github.com/chrisxkeith/particle-photon-sensor-test-bed onto one or more Particle microcontrollers.
