package wwcs2022.socialmemcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class Downloader {

    private static final Logger log = LoggerFactory.getLogger(Downloader.class);
    private static final int REPORT_INTERVAL_MS = 5000;

    public static boolean downloadFile(String sourceUrl, String targetFile) {
        File output = new File(targetFile);
        if (output.exists()) {
            log.info("File {} found. Skipping download.", targetFile);
            return true;
        }
        File dir = output.getParentFile();
        if (!dir.exists()) {
            log.info("Creating directory {}", dir);
            dir.mkdirs();
        }

        try {
            File temp = File.createTempFile("data_download_tmp", ".part", dir);
            temp.deleteOnExit();
            log.info("Downloading PBF data file from {} to temporary file {}", sourceUrl, dir);
            performDownload(sourceUrl, temp);
            Files.move(temp.toPath(), output.toPath());
            log.info("Download finished");
            return true;
        } catch (IOException e) {
            log.error("An error occurred while downloading the file", e);
            return false;
        }
    }

    private static void performDownload(String sourceUrl, File target) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new URL(sourceUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(target)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            long totalBytes = 0;
            long lastReport = 0;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytes += bytesRead;
                if (REPORT_INTERVAL_MS > 0 && System.currentTimeMillis() - lastReport >= REPORT_INTERVAL_MS) {
                    log.info("{} bytes downloaded", totalBytes);
                    lastReport = System.currentTimeMillis();
                }
            }
        }
    }

}
