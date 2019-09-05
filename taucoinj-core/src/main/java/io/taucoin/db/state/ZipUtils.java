
package io.taucoin.db.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.GZIPInputStream;

public class ZipUtils {

    private static final Logger logger = LoggerFactory.getLogger("stateloader");

    public static void unzipFile(File in, String destDir, String destFileName) {
        if (in == null || destDir == null || destDir.isEmpty()
                || destFileName == null || destFileName.isEmpty()) {
            return;
        }

        GZIPInputStream zis = null;
        File destFile = null;
        FileOutputStream fos = null;
        byte[] buffer = new byte[5 * 1024];

        try {
            zis = new GZIPInputStream(new FileInputStream(in));

            destFile = new File(destDir, destFileName);
            fos = new FileOutputStream(destFile);

            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            // This should never happen.
            logger.error("Unzip fatal err:{}", e);
        } catch (IOException e) {
            logger.error("Unzip fatal err:{}", e);
        } finally {
            try {
                if (zis != null) {
                    zis.close();
                }

                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.error("Unzip fatal err:{}", e);
            }
        }
    }
}
