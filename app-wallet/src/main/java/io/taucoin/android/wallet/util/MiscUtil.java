package io.taucoin.android.wallet.util;

import android.graphics.Paint;
import android.view.View;

public class MiscUtil {

    /**
     * measure View
     */
    public static int measure(int measureSpec, int defaultSize) {
        int result = defaultSize;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);

        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }

    public static String getPrecisionFormat(int precision) {
        return "%." + precision + "f";
    }

    public static <T> T[] reverse(T[] arrays) {
        if (arrays == null) {
            return null;
        }
        int length = arrays.length;
        for (int i = 0; i < length / 2; i++) {
            T t = arrays[i];
            arrays[i] = arrays[length - i - 1];
            arrays[length - i - 1] = t;
        }
        return arrays;
    }

    public static float measureTextHeight(Paint paint) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return (Math.abs(fontMetrics.ascent) - fontMetrics.descent);
    }
}