
package io.taucoin.db.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    private static final Logger logger = LoggerFactory.getLogger("stateloader");

    public static void unzipFile(File in, String destDir) {
        if (in == null || destDir == null || destDir.isEmpty()) {
            return;
        }

        ZipInputStream zis = null;
        ZipEntry zipEntry = null;
        File destFile = null;
        FileOutputStream fos = null;
        byte[] buffer = new byte[5 * 1024];

        try {
            zis = new ZipInputStream(new FileInputStream(in));
            zipEntry = zis.getNextEntry();

            if (zipEntry != null) {
                destFile = new File(destDir, zipEntry.getName());
                fos = new FileOutputStream(destFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
        } catch (FileNotFoundException e) {
            // This should never happen.
            logger.error("Unzip fatal err:{}", e);
        } catch (IOException e) {
            logger.error("Unzip fatal err:{}", e);
        } finally {
            try {
                if (zis != null) {
                    zis.closeEntry();
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
