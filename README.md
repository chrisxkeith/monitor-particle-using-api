# monitor-particle-using-api

Install Git client, Java JDK and maven as described here : https://github.com/chrisxkeith/commute-time-aggregator/blob/master/README.md . You may not need Spring Tool Suite immediately, but will need it to do any development. 

Assumes that you have flashed https://github.com/chrisxkeith/particle-photon-sensor-test-bed onto one or more Particle microcontrollers.

You will need to create and install a Google Cloud Platform credential file (e.g., client_secret.json) in order for the email functionality to work. The file should be put into the src/main/resources/com/ckkeith/monitor/ directory.

See https://github.com/chrisxkeith/monitor-particle-using-api/blob/master/bootstrap.sh for build steps. If you want to run the script directly, copy it into a temporary directory and run from that directory using Git Bash.

Email addresses (To: and From:) are currently hardcoded. You will need to find them (search for chris.keith@gmail.com) and change them.

If you want to convert 'one sensor data point point row' to 'multiple sensors data points per row' you can try https://github.com/chrisxkeith/pivot-table-sort-of .
