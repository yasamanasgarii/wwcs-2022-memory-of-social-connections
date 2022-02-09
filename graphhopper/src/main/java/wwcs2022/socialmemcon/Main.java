package wwcs2022.socialmemcon;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {

    private static final String PBF_LOCATION = "https://download.geofabrik.de/europe/hungary-latest.osm.pbf";
    private static final String DATA_FILE = "data/hungary-latest.osm.pbf";
    //private static final String DATA_FILE = "data/test.osm.pbf";
    private static final String GH_CACHE_DIR = "cache";
    private static final String COORDINATES_FILE = "coordinates.txt";
    private static final String RESTRICTIONS_FILE = "restrictions.txt";
    private static final String OUTPUT_PREFIX = "output_";
    private static final String VEHICLE = "car";
    private static final String PROFILE = "car";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String [] args) throws IOException {

        File coorFile = new File(COORDINATES_FILE);
        File resFile = new File(RESTRICTIONS_FILE);

        if (!coorFile.exists()) {
            log.info("No coordinates file for the routing found. Please make sure {} exists.", coorFile);
            return;
        }

        if (!resFile.exists()) {
            log.info("No restrictions file found for the deletion of parts of the network."
             + " Please make sure {} exists", resFile);
            return;
        }

        List<LocationEntry> locations = LocationEntry.readPoints(coorFile);
        if (locations == null) {
            log.info("Unable to obtain coordinates of interest. Exiting");
            return;
        }
        log.info("{} locations of interest read from input file", locations.size());
        List<RestrictionEntry> restrictions = RestrictionEntry.readEntries(resFile);
        if (restrictions == null) {
            log.info("Problem while reading restrictions file. Aborting.");
            return;
        }
        log.info("{} restriction entries read from input file", restrictions.size());


        if (!Downloader.downloadFile(PBF_LOCATION, DATA_FILE)) {
            log.info("Unable to obtain data file. Exiting");
            return;
        }
        log.info("Processing datasets");
        var datasets = FilterBridgesMain.processAll(restrictions, DATA_FILE, "data/network-", ".osm.pbf");
        log.info("Datasets: {}", datasets);
        log.info("Setting up routing engines");
        var hoppers = RoutingMain.createInstances(datasets, GH_CACHE_DIR, VEHICLE);
        log.info("Hoppers: {}", hoppers);
        var baseHopper = RoutingMain.createGraphHopperInstance(DATA_FILE, GH_CACHE_DIR+"/now", VEHICLE);
        log.info("Computing distances");
        var baseDistances = RoutingMain.computeDistances(locations, baseHopper, VEHICLE, RoutingMain.Weighting.SHORTEST);
        var distances = RoutingMain.computeDistances(locations, hoppers, VEHICLE, RoutingMain.Weighting.SHORTEST);
        log.info("Writing spreadsheets");
        SpreadsheetWriter.writeBigSpreadsheet(baseDistances, distances, new File(OUTPUT_PREFIX+"-all.xlsx"));
        log.info("Finished processing");
    }

}
