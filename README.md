# monitor-particle-using-api

Install Git client, Java JDK and maven as described here : https://github.com/chrisxkeith/commute-time-aggregator/blob/master/README.md . You may not need Spring Tool Suite immediately, but will need it to do any development. 

Assumes that you have set up https://github.com/chrisxkeith/particle-photon-sensor-test-bed on one or more Particle microcontrollers.

You will need to create and install a Google Cloud Platform credential file (e.g., client_secret.json) in order for the email functionality to work.

See https://github.com/chrisxkeith/monitor-particle-using-api/blob/master/bootstrap.sh for build steps.

If you want to convert 'one sensor data point point row' to 'multiple sensors data points per row' you can try https://github.com/chrisxkeith/pivot-table-sort-of .
