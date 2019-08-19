package io.taucoin.android.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class LoggerManager {

    private static final Logger logger = LoggerFactory.getLogger("loggerMgr");

    private static final String LOG_PATH = "/sdcard/tau-mobile/logs";

    // Remove old log files and just store latest 3 days' log.
    public static void removeOldLogFiles() {
        // Get latest three days.
        Calendar calendar = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyyMMdd");

        String today = df.format(calendar.getTime());
        calendar.add(Calendar.DATE, -1);
        String yesterday = df.format(calendar.getTime());
        calendar.add(Calendar.DATE, -1);
        String dayBeforeYesterday = df.format(calendar.getTime());

        logger.info("Today {}, yesterday {}, before yesterday {}",
                today, yesterday, dayBeforeYesterday);

        File f = new File(LOG_PATH);
        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        File[] files = f.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file == null || !file.isFile()) {
                continue;
            }

            String name = file.getName();

            if (name.length() == 16 && (name.endsWith("log"))) {
                String day = name.substring(4, 12);

                if (!today.equals(day) && !yesterday.equals(day)
                        && !dayBeforeYesterday.equals(day)) {
                    logger.info("Remove log file: {}", name);
                    file.delete();
                }
            }
        }
    }
}
