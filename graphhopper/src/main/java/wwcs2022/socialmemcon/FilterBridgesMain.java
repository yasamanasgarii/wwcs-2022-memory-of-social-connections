package wwcs2022.socialmemcon;

import com.graphhopper.util.CustomModel;
import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FilterBridgesMain {

    private static final Logger log = LoggerFactory.getLogger(FilterBridgesMain.class);

    private static final String RESTRICTIONS_FILE = "restrictions.txt";

    public static void main(String [] args) throws IOException {
        List<RestrictionEntry> restrictions = RestrictionEntry.readEntries(new File(RESTRICTIONS_FILE));
        String input = "data/hungary-latest.osm.pbf";
        processAll(restrictions, input, "test-", ".osm.pbf");
    }

    public static Map<Integer,File> processAll(List<RestrictionEntry> restrictions, String input,
        // TODO: If interrupted this method will skip over the output files the next time you run this
                                               String outputPrefix, String outputPostfix) throws IOException {
        Map<Integer,File> result = new TreeMap<>();
        var map = getSubLists(restrictions);
        for (var entry : map.entrySet()) {
            int year = entry.getKey();
            var data = entry.getValue();
            String output = outputPrefix+year+outputPostfix;
            File file = new File(output);
            if (file.exists()) {
                log.info("File {} already exists, skipping processing", file);
                result.put(year, file);
                continue;
            }
            log.info("Processing {} restricted until year {}", input, year);
            process(input, output, data);
            log.info("Written processed output to {}", output);
            result.put(year, file);
        }
        return result;
    }

    private static Map<Integer, List<RestrictionEntry>> getSubLists(List<RestrictionEntry> restrictions) {
        Set<Integer> years = new TreeSet<>();
        for (RestrictionEntry restriction : restrictions) {
            years.add(restriction.getYear());
        }

        Map<Integer, List<RestrictionEntry>> result = new TreeMap<>();

        for (Integer year : years) {
            List<RestrictionEntry> yearRestrictions;
            yearRestrictions = restrictions.stream()
                    .filter(res -> res.getYear() >= year)
                    .collect(Collectors.toList());
            result.put(year, yearRestrictions);
        }
        return result;
    }


    public static void process(String inputFile, String outputFile, List<RestrictionEntry> restrictions)
            throws IOException {
        Set<Long> remove;
        try (InputStream is = new FileInputStream(inputFile)) {
            OsmIterator iterator = new PbfIterator(is, true);
            log.info("Searching for node coordinates to remove");
            remove = readNodeIds(iterator, restrictions);
            log.info("Found {} nodes to remove", remove.size());
        }
        try (InputStream is = new FileInputStream(inputFile);
             OutputStream os = new FileOutputStream(outputFile)) {
            log.info("Processing dataset");
            OsmIterator iterator = new PbfIterator(is, true);
            PbfWriter writer = new PbfWriter(os, true);
            long skipWays = 0;
            long skipNodes = 0;
            outerLoop:
            while (iterator.hasNext()) {
                EntityContainer container = iterator.next();
                OsmEntity entity = container.getEntity();
                if (entity instanceof OsmWay) {
                    OsmWay way = (OsmWay) entity;
                    for (int t=0; t < way.getNumberOfNodes(); t++) {
                        long id = way.getNodeId(t);
                        if (remove.contains(id)) {
                            skipWays++;
                            continue outerLoop;
                        }
                    }
                    writer.write(way);
                }
                else if (entity instanceof OsmNode) {
                    OsmNode node = (OsmNode) entity;
                    if (remove.contains(node.getId())) {
                        skipNodes++;
                        continue;
                    }
                    writer.write(node);
                }
                else if (entity instanceof OsmBounds) {
                    OsmBounds bounds = (OsmBounds) entity;
                    writer.write(bounds);
                }
                else if (entity instanceof OsmRelation) {
                    OsmRelation relation = (OsmRelation) entity;
                    writer.write(relation);
                }
                else {
                    log.warn("Entity of type {} was not recognized. Contents: {}",
                            entity.getClass().getCanonicalName(), entity);
                }
            }
            log.info("Done processing data. Skipped {} nodes and {} ways", skipNodes, skipWays);
        }
    }

    private static Set<Long> readNodeIds(OsmIterator iterator, List<RestrictionEntry> restrictions) {
        GeometryFactory fac = new GeometryFactory();
        List<Geometry> geometries = restrictions.stream()
                .map(re -> re.asFeature().getGeometry())
                .collect(Collectors.toList());
        Set<Long> result = new HashSet<>();
        while (iterator.hasNext()) {
            OsmEntity entity = iterator.next().getEntity();
            if (entity instanceof OsmNode) {
                OsmNode node = (OsmNode) entity;
                Coordinate c = new Coordinate(node.getLatitude(), node.getLongitude());
                if (geometries.stream().anyMatch(geom -> geom.contains(fac.createPoint(c)))) {
                    result.add(node.getId());
                    log.trace("Found node {}", node);
                }
            }
        }
        return result;
    }

}
