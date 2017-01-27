# osc-nuage-plugin
To install an updated dependency jar into basedir\local-maven-repo

C:\git\osc-nuage-plugin>mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=c:\nuage\java-bambou\target\bambou-2.0.0.jar -DgroupId=net.nuagenetworks -DartifactId=bambou -Dversion=2.0.0 -Dpackaging=jar   -DlocalRepositoryPath=.\local-maven-repo

C:\git\osc-nuage-plugin>mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=c:\nuage\vspk-java\target\vspk-4.0.jar -DgroupId=net.nuagenetworks -DartifactId=vspk -Dversion=4.0 -Dpackaging=jar   -DlocalRepositoryPath=.\local-maven-repo
