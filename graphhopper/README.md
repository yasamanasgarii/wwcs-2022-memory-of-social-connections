# Computing distances

This Java project is used to compute distances between cities in a road network using the [GraphHopper routing engine](https://github.com/graphhopper/graphhopper).

To be able to run it, you need a Java Runtime Environment. On Windows and Mac OS you can obtain this from the [Adoptium Project](https://adoptium.net/) - the default version, which gives you Java 17 with the Hotspot JVM is fine
On Ubuntu, you can typically install this with:

```
sudo apt install openjdk
```

You can compile and run the project with the Maven build tool, by running the following on the command line within the project folder containing `pom.xml` and the `mvnw` wrapper scripts.

On Windows:

```
mvnw exec:java
```

On Linux and Mac OS:

```
./mvnw exec:java
```

Note that this will first download Maven, and then a number of Java libraries automatically, and compile the project before it is executed. The OpenStreetMaps data file is downloaded if it is not available (it is not checked into the Github repository), which may take additional time. GraphHopper also does some preprocessing the first time you run it, and stores the results of this in the `cache` directory. So the first time you run it, it will be a lot slower than the second time.

