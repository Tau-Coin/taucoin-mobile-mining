package io.taucoin.android.service;

public enum CreateTaucoinResult {

    SUCCESSFUL,
    EXISTED,
    DB_CORRUPTED;

    public boolean isSuccessful() {
        return equals(SUCCESSFUL);
    }

    public boolean isExisted() {
        return equals(EXISTED);
    }

    public boolean isDBCorrupted() {
        return equals(DB_CORRUPTED);
    }
}
