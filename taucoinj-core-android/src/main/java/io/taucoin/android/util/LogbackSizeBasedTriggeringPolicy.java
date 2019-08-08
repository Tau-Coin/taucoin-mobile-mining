package io.taucoin.android.util;

import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;

import java.io.File;

public class LogbackSizeBasedTriggeringPolicy<E> extends
        SizeBasedTriggeringPolicy<E> {

    @Override
    public boolean isTriggeringEvent(File activeFile, E event) {
        return activeFile.length() >= FileSize.valueOf(getMaxFileSize())
                .getSize();
    }
}
