package wwcs2022.socialmemcon;

import com.graphhopper.util.shapes.GHPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class LocationEntry {

    private static Logger log = LoggerFactory.getLogger(LocationEntry.class);

    private final String name;
    private final double lat;
    private final double lng;

    public LocationEntry(String line) {
        String [] split = line.split(",");
        this.name = split[0].strip();
        this.lat = Double.parseDouble(split[1].strip());
        this.lng = Double.parseDouble(split[2].strip());
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public GHPoint toPoint() {
        return new GHPoint(lat, lng);
    }

    @Override
    public String toString() {
        return "LocationEntry{" +
                "name='" + name + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                '}';
    }

    public static List<LocationEntry> readPoints(File in) {
        try {
            return Files.lines(in.toPath())
                    .filter(line -> !line.isBlank())
                    .map(LocationEntry::new)
                    .collect(Collectors.toList());
        }
        catch (IOException ex) {
            log.error("Error reading coordinates file", ex);
            return null;
        }
    }
}
