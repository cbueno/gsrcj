# Introduction #

If you want to use the **gsrcj** library within your maven project, add the following lines to your _pom.xml_:

# Repository #
Insert the following into the **repositories** tag:
```
<repository>
	<snapshots>
		<enabled>false</enabled>
	</snapshots>
	<id>releases.artifactory.wikisquare.de</id>
	<name>Releases for
			geopublishing.org - wikisquare.de</name>
	<url>http://artifactory.wikisquare.de/artifactory/libs-releases</url>
</repository>
<repository>
	<snapshots>
		<enabled>true</enabled>
		<updatePolicy>always</updatePolicy>
	</snapshots>
	<id>snapshots.artifactory.wikisquare.de</id>
	<name>Snapshots for
			geopublishing.org - wikisquare.de</name>
	<url>http://artifactory.wikisquare.de/artifactory/libs-snapshots</url>
</repository>
```

# Dependency #
Insert the following into the **dependencies** tag:
```
<dependency>
	<version>0.5-SNAPSHOT</version>
	<groupId>org.geopublishing</groupId>
	<artifactId>gsrcj</artifactId>
</dependency>
```