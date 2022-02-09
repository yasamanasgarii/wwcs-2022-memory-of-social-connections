package wwcs2022.socialmemcon;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import org.codehaus.commons.compiler.util.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class RoutingMain {

    private static final Logger log = LoggerFactory.getLogger(RoutingMain.class);

    public static Map<Integer, Map<String, Map<String,Double>>> computeDistances(
            List<LocationEntry> locations, Map<Integer,Producer<GraphHopper>> hoppers, String vehicle, Weighting w) {
        Map<Integer, Map<String, Map<String,Double>>> result = new TreeMap<>();
        for (var entry : hoppers.entrySet()) {
            result.put(entry.getKey(), computeDistances(locations, entry.getValue(), vehicle, w));
        }
        return result;
    }

    public static Map<String, Map<String,Double>> computeDistances(
            List<LocationEntry> locations, Producer<GraphHopper> prod, String vehicle, Weighting w) {
        GraphHopper hopper = prod.produce();
        var result = computeDistances(locations, hopper, vehicle, w);
        hopper.close();
        return result;
    }

    public static Map<String, Map<String,Double>> computeDistances(
            List<LocationEntry> locations, GraphHopper hopper, String vehicle, Weighting w) {
        Map<String,Map<String,Double>> result = new TreeMap<>();
        for (LocationEntry from : locations) {
            Map<String,Double> innerMap = new TreeMap<>();
            for (LocationEntry to : locations) {
                if (from != to) {
                    String profile = w.getProfileForVehicle(vehicle);
                    GHRequest req = new GHRequest(from.toPoint(), to.toPoint()).setProfile(profile);
                    GHResponse resp = hopper.route(req);
                    if (resp.hasErrors()) {
                        log.error("Errors in route computation: {}", resp.getErrors());
                    }
                    innerMap.put(to.getName(), resp.getBest().getDistance());
                }
            }
            result.put(from.getName(), innerMap);
        }
        return result;
    }


    public static Map<Integer, Producer<GraphHopper>> createInstances(Map<Integer, File> files, String cacheDirPrefix,
                                                                      String vehicle) {
        Map<Integer,Producer<GraphHopper>> result = new TreeMap<>();
        File cacheDir = new File(cacheDirPrefix);
        for (var entry : files.entrySet()) {
            String osmFile = entry.getValue().toString();
            int year = entry.getKey();
            String dir = new File(cacheDir, ""+year).toString();
            GraphHopper hopper = createGraphHopperInstance(osmFile, dir, vehicle);
            hopper.close();
            result.put(year, () -> createGraphHopperInstance(osmFile, dir, vehicle));
        }
        return result;
    }

    public static GraphHopper createGraphHopperInstance(String osmFile, String cacheDir, String vehicle) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        // specify where to store graphhopper files
        hopper.setGraphHopperLocation(cacheDir);

        // see docs/core/profiles.md to learn more about profiles
        List<Profile> profiles = new ArrayList<>();
        List<CHProfile> chProfiles = new ArrayList<>();
        for (Weighting w : Weighting.values()) {
            String profileName = w.getProfileForVehicle(vehicle);
            Profile profile = new Profile(profileName).setVehicle(vehicle).setWeighting(w.getWeighting()).setTurnCosts(false);
            CHProfile chProfile = new CHProfile(profileName);
            profiles.add(profile);
            chProfiles.add(chProfile);
        }
        hopper.setProfiles(profiles);
        // this enables speed mode for the profile we called car
        hopper.getCHPreparationHandler().setCHProfiles(chProfiles);

        // now this can take minutes if it imports or a few seconds for loading of course this is dependent on the area you import
        log.info("Creating GraphHopper instance");
        hopper.importOrLoad();
        log.info("GraphHopper instance loaded");
        return hopper;
    }

    public enum Weighting {
        FASTEST {
            public String getWeighting() {
                return "fastest";
            }
        },
        SHORTEST {
            public String getWeighting() {
                return "shortest";
            }
        };
        public abstract String getWeighting();

        public String getProfileForVehicle(String vehicle) {
            return vehicle+"_"+getWeighting();
        }
    }

}
