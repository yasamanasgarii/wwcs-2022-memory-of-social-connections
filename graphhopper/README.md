# Computing distances

This Java project is used to compute distances between cities in a road network using the [GraphHopper routing engine](https://github.com/graphhopper/graphhopper).

To be able to run it, you need:

* A Java Runtime Environment, typically OpenJDK
* The Maven build tool

On Ubuntu, you can typically install this with:

```
sudo apt install openjdk maven
```

Once maven is installed, you can execute by running the following command (in the directory that contains `pom.xml`):

```
mvn exec:java
```

Note that this will download the a number of Java libraries automatically, and compile the project before it is executed. The OpenStreetMaps data file is downloaded if it is not available (it is not checked into the Github repository), which may take additional time. GraphHopper also does some preprocessing the first time you run it, and stores the results of this in the `cache` directory. So the first time you run it, it will be a lot slower than the second time.

