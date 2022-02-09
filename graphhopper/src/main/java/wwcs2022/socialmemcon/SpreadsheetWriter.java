package wwcs2022.socialmemcon;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SpreadsheetWriter {

    private static final Logger log = LoggerFactory.getLogger(SpreadsheetWriter.class);

    public static void writeBigSpreadsheet(Map<String, Map<String,Double>> baseDistances,
                                            Map<Integer,Map<String, Map<String,Double>>> distances, File output) {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(output)) {
            writeDistancePairs(wb.createSheet("pairs"), baseDistances, distances);
            writeDistanceMatrix(wb.createSheet("matrix-base"), baseDistances);
            for (var entry : distances.entrySet()) {
                writeDistanceMatrix(wb.createSheet("matrix-"+entry.getKey()), entry.getValue());
            }
            wb.write(fos);
        }
        catch (IOException ex) {
            log.error("An exception occurred while generated the spreadsheet", ex);
        }
    }

    public static void writeSpreadsheet(Map<String, Map<String,Double>> distances, File output) {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(output)) {
            writeDistanceMatrix(wb.createSheet("matrix"), distances);
            writeDistancePairs(wb.createSheet("pairs"), distances);
            wb.write(fos);
        }
        catch (IOException ex) {
            log.error("An exception occurred while generated the spreadsheet", ex);
        }
    }

    private static void writeDistancePairs(Sheet sheet, Map<String, Map<String,Double>> baseDistances,
                                           Map<Integer,Map<String, Map<String,Double>>> distances) {
        int colIndex = 2;
        int rowIndex = 0;
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("From");
        row.createCell(1).setCellValue("To");
        row.createCell(colIndex++).setCellValue("Base Distance");
        for (Integer year : distances.keySet()) {
            row.createCell(colIndex++).setCellValue("Distance "+year);
        }
        for (var e1 : baseDistances.entrySet()) {
            String from = e1.getKey();
            for (var e2 : e1.getValue().entrySet()) {
                String to = e2.getKey();
                double distance = e2.getValue();
                row = sheet.createRow(rowIndex++);
                colIndex = 0;
                row.createCell(colIndex++).setCellValue(from);
                row.createCell(colIndex++).setCellValue(to);
                row.createCell(colIndex++).setCellValue(distance);
                for (Map<String,Map<String,Double>> map : distances.values()) {
                    double d = map.get(from).get(to);
                    row.createCell(colIndex++).setCellValue(d);
                }
            }
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


}
