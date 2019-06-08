# Currently doesn't run because of security.

# run.sh seems to (sometimes) leave processes running.
# Try Powershell to see if it works better.

Set-PSDebug -Trace 1
Push-Location $Env:HOMEDIR$Env:HOMEPATH\Documents\Github\monitor-particle-using-api\

# TODO : Check exit statuses
# TODO : New-Item -path $Env:HOMEDIR$Env:HOMEPATH\Documents\tmp\$Env:HOSTNAME

mvn clean install exec:java "`-Dmaven.test.skip=true" "`-Dexec.mainClass=`"com.ckkeith.monitor.Main`""

# TODO : | Tee-Object -Append $Env:HOMEDIR$Env:HOMEPATH\Documents\tmp\$Env:HOSTNAME\monitor.log

Pop-Location