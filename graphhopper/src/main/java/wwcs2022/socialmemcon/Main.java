package wwcs2022.socialmemcon;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Main {

    private static final String PBF_LOCATION = "https://download.geofabrik.de/europe/hungary-latest.osm.pbf";
    private static final String DATA_FILE = "data/hungary-latest.osm.pbf";
    private static final String GH_CACHE_DIR = "cache";
    private static final String COORDINATES_FILE = "coordinates.txt";
    private static final String OUTPUT_WORKBOOK = "output2.xlsx";
    private static final int REPORT_INTERVAL_MS = 5000;
    private static final String VEHICLE = "car";
    private static final String PROFILE = "car";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String [] args) {
        if (!downloadFile()) {
            log.info("Unable to obtain data file. Exiting");
            return;
        }
        GraphHopper hopper = createGraphHopperInstance();
        List<LocationEntry> locations = LocationEntry.readPoints(new File(COORDINATES_FILE));
        if (locations == null) {
            log.info("Unable to obtain coordinates of interest. Exiting");
            return;
        }
        log.info("Computing distances");
        Map<String, Map<String,Double>> distances = computeDistances(locations, hopper);
        log.info("Distances computed. Writing Spreadsheet.");
        writeSpreadsheet(distances);
        log.info("Spreadsheet written.");

    }

    private static void writeSpreadsheet(Map<String, Map<String,Double>> distances) {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(OUTPUT_WORKBOOK)) {
            writeDistanceMatrix(wb.createSheet("matrix"), distances);
            writeDistancePairs(wb.createSheet("pairs"), distances);
            wb.write(fos);
        }
        catch (IOException ex) {
            log.error("An exception occurred while generated the spreadsheet", ex);
        }
    }

    private static void writeDistancePairs(Sheet sheet, Map<String, Map<String,Double>> distances) {
        int rowIndex = 0;
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("From");
        row.createCell(1).setCellValue("To");
        row.createCell(2).setCellValue("Distance");
        for (var e1 : distances.entrySet()) {
            String from = e1.getKey();
            for (var e2 : e1.getValue().entrySet()) {
                String to = e2.getKey();
                double distance = e2.getValue();
                row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(from);
                row.createCell(1).setCellValue(to);
                row.createCell(2).setCellValue(distance);
            }
        }
    }

    private static void writeDistanceMatrix(Sheet sheet, Map<String, Map<String,Double>> distances) {
        // Build a map with all locations
        Set<String> locations = new TreeSet<>(distances.keySet());
        distances.values()
                .forEach(map -> locations.addAll(map.keySet()));
        Row row = sheet.createRow(0);
        int rowIndex = 1;
        int col = 1;
        // Write the header
        for (String loc : locations) {
            row.createCell(col++).setCellValue(loc);
        }
        for (String from : locations) {
            row = sheet.createRow(rowIndex++);
            col = 0;
            Map<String,Double> innerMap = distances.get(from);
            row.createCell(col++).setCellValue(from);
            for (String to : locations) {
                Double dist = innerMap.get(to);
                if (dist != null) {
                    row.createCell(col).setCellValue(dist);
                }
                col++;
            }
        }
    }

    private static Map<String, Map<String,Double>> computeDistances(List<LocationEntry> locations, GraphHopper hopper) {
        Map<String,Map<String,Double>> result = new LinkedHashMap<>();
        for (LocationEntry from : locations) {
            Map<String,Double> innerMap = new LinkedHashMap<>();
            for (LocationEntry to : locations) {
                if (from != to) {
                    GHRequest req = new GHRequest(from.toPoint(), to.toPoint()).setProfile(PROFILE);
                    GHResponse resp = hopper.route(req);
                    innerMap.put(to.getName(), resp.getBest().getDistance());
                }
            }
            result.put(from.getName(), innerMap);
        }
        return result;
    }

    private static GraphHopper createGraphHopperInstance() {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(DATA_FILE);
        // specify where to store graphhopper files
        hopper.setGraphHopperLocation(GH_CACHE_DIR);

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(new Profile(PROFILE).setVehicle(VEHICLE).setWeighting("fastest").setTurnCosts(false));

        // this enables speed mode for the profile we called car
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile(VEHICLE));

        // now this can take minutes if it imports or a few seconds for loading of course this is dependent on the area you import
        log.info("Creating GraphHopper instance");
        hopper.importOrLoad();
        log.info("GraphHopper instance loaded");
        return hopper;
    }

    private static boolean downloadFile() {
        File output = new File(DATA_FILE);
        if (output.exists()) {
            log.info("PBF data file found. Skipping download.");
            return true;
        }
        File dir = output.getParentFile();
        if (!dir.exists()) {
            log.info("Creating directory {}", dir);
            dir.mkdirs();
        }
        log.info("Downloading PBF data file from {}", PBF_LOCATION);
        try (BufferedInputStream in = new BufferedInputStream(new URL(PBF_LOCATION).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(DATA_FILE)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            long totalBytes = 0;
            long lastReport = 0;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytes += bytesRead;
                if (System.currentTimeMillis() - lastReport >= REPORT_INTERVAL_MS) {
                    log.info("{} bytes downloaded", totalBytes);
                    lastReport = System.currentTimeMillis();
                }
            }
            log.info("Download finished");
            return true;
        } catch (IOException e) {
            log.error("An error occurred while downloading the file", e);
            return false;
        }
    }

}
