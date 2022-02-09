package wwcs2022.socialmemcon;

import com.graphhopper.util.JsonFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RestrictionEntry {

    private static final Logger log = LoggerFactory.getLogger(RestrictionEntry.class);

    private final String name;
    private final int year;
    private final double speedLimit;
    private final List<Double> latList, lngList;

    public RestrictionEntry(String line) {
        String [] split1 = line.split(";");
        this.name = split1[0].strip();
        this.year = Integer.parseInt(split1[1].strip());
        this.speedLimit = Double.parseDouble(split1[2].strip());
        String polygonStr = split1[3];
        String [] split2 = polygonStr.split("::");
        this.latList = new ArrayList<>();
        this.lngList = new ArrayList<>();
        for (String latLngStr : split2) {
            String [] split3 = latLngStr.split(",");
            double lat = Double.parseDouble(split3[0].strip());
            double lng = Double.parseDouble(split3[1].strip());
            latList.add(lat);
            lngList.add(lng);
        }
    }

    public String getName() {
        return name;
    }

    public int getYear() {
        return year;
    }

    public double getSpeedLimit() {
        return speedLimit;
    }

    public JsonFeature asFeature() {
        GeometryFactory gFac = new GeometryFactory();

        JsonFeature result = new JsonFeature();
        Coordinate [] coors = new Coordinate[latList.size()];
        for (int i=0; i < latList.size(); i++) {
            coors[i] = new Coordinate(latList.get(i), lngList.get(i));
        }
        result.setGeometry(gFac.createPolygon(coors));
        return result;
    }

    public static List<RestrictionEntry> readEntries(File f) {
        try {
            return Files.lines(f.toPath())
                    .filter(line -> !line.isBlank())
                    .map(RestrictionEntry::new)
                    .collect(Collectors.toList());
        }
        catch (IOException ex) {
            log.error("Error reading restriction entries", ex);
            return null;
        }
    }

}
