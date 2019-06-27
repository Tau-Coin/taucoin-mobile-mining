package io.taucoin.datasource;

public class DBCorruptionException extends RuntimeException {

    private Exception exception;

    public DBCorruptionException(Exception exception) {
        this.exception = exception;
    }

    public DBCorruptionException(String message) {
        this.exception = new RuntimeException(message);
    }

    @Override
    public String getMessage() {
        return exception.getMessage();
    }

    @Override
    public String toString() {
        return exception.toString();
    }
}
