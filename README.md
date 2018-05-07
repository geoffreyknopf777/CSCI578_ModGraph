# CSCI578_ModGraph
A java server applet that graphs the modifiability and bundle/chord visualization of a C/C++ or Java software project from GitHub
https://github.com/geoffreyknopf777/CSCI578_ModGraph

### Prerequisites

Eclipse ( Already part of cs578 VM)
Version: Oxygen.2 Release (4.7.2)
Build id: 20171218-0600
```
For Dev purpose you'll need the Web perspective and Java EE perspective
Aditionally eclipse jsp support packages packages
  - you will be prompted when trying to modify jsp
    Eclipse Java EE Developer Tools	3.9.3.v201803221418	org.eclipse.jst.enterprise_ui.feature.feature.group	Eclipse Web Tools Platform
```

Tomcat 
Version: v9.0.7


### Installing Support Software (tomcat) 

There will only be the need to install tomcat
Here is a quick guide taken from multiple tomcat supporting documentation 
```
// Download the tar file from website
apache-tomcat-9.0.7.tar.gz 

// Get the latest apt-get
sudo apt-get update

// Get the latest jdk (Optional) trying without

// (Optional) determine privlaged user 

// untar
tar -xzvf apache-tomcat-9.0.7.tar.gz 
 
// Set enviorn var for CATALINA_HOME in .profile
export CATALINA_HOME="/home/cs578user/Documents/cs_578/apache-tomcat-9.0.7/"

// go into CATALINA_HOME/bin and untart commons-daemon native
tar xvfz commons-daemon-native.tar.gz 

// go in to commons-.../unix and configure 
cs578user@ubuntu:~/Documents/cs 578/apache-tomcat-9.0.7/bin/commons-daemon-1.1.0-native-src/unix$ ./configure 
// then call make
make 

// cp jsvc up to bin
cp jsvc ../..
cd ../..

// to have tomcat run 
$CATALINA_HOME/bin/startup.sh

// to stom tomcat run
$CATALINA_HOME/bin/shutdown.sh

// view on your browser 
http://localhost:8080/

// add a user in conf to login to portals
cd $CATALINA_HOME/conf
//edit tomcat-users.xml
  <role rolename="cs578"/>
  <user username="cs578" password="cool" roles="cs578,manager-gui,admin-gui"/>

// To get the sample hello program 
https://tomcat.apache.org/tomcat-9.0-doc/appdev/sample/

// download and put the sample.war file in $CATALINA_HOME/webapps

// restart tomcat and go to
http://localhost:8080/sample/ 
```

### Deploying the Webapp with cs578.war
This is the quickest method if available because the war contains the full build that tomcat can run. 

similar to sample in the tomcat installing guide...
```
// download and put the cs578.war file in $CATALINA_HOME/webapps

// restart tomcat and go to
http://localhost:8080/cs578/input.jsp/ 

// NOTE: to restart
use the $CATALINA_HOME/bin/shutdown.sh and $CATALINA_HOME/bin/startup.sh
```

### Building from source Step 1 Jar creation (skip to next step if you have the jars) 
First you will need to create the jar files for arcade and ModGraph

our arcade is slightly modified and should not be assumed to be the same as provided by the course. 
```
import the arcade directory as a project
export as arcade.jar
```
next make the ModGraph jar that is dependant on arcade.jar
```
import the ModGraphy directory as a project
go to properties->Java Build Path-> Add External Jars 
  add the arcade.jar
export as ModGraph.jar
```

### Building from source Step 2 War creation 
Now that you have the jar files ready
time to make the war file for /cs578
```
import the /CSCI578_ModGraph/cs578 as a Web project
go to properties->Java Build Path-> Classpath-> Add External Jars
  add the ModGraph.jar
  (optionally) add the arcade.jar 
go to properties->Deployment Assembly->Add...
  go to Java Build Path Entries
    choose all the jars 
      add ModGraph
export as cs578.war
```

### Building from source Step 3 Deploy
You can use the steps above for deploying the war

Alternatively you can use eclipse to run for you

Add a tomcat server in eclipse
```
in Server tab create a server and point it to your tomcat install

then go to the /cs578/WebContent in Project Explorer

then pick input.jsp to Run As... then choose tomcatv9 as choice

the browser will launch within eclipse!
```

### Authors 
- Geoffrey Knopf
- Samuel Villarreal 
- Gregory Lawler
- Revanth Rayala 

### Acknowledgments
USC cs578 Nenad Medvidovic and  Daniel Link
  - Providers of VM env and Aracde src
  
Apache Tomcat team
  - Use of the open source project and sample webapp base
