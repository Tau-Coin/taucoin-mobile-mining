package io.taucoin.foundation.net.bean;

public class FileLoadingBean {
    private long total;
    private long progress;
    private long byteCount;

    public long getProgress() {
        return progress;
    }

    public long getTotal() {
        return total;
    }

    public long getByteCount() {
        return byteCount;
    }

    public FileLoadingBean(long total, long progress, long byteCount) {
        this.total = total;
        this.progress = progress;
        this.byteCount = byteCount;
    }
}
