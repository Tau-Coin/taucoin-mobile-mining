package io.taucoin.foundation.net.callback;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.taucoin.foundation.net.exception.ApiException;
import io.taucoin.foundation.net.exception.CodeException;
import io.taucoin.foundation.net.exception.FactoryException;

public abstract class MainObserver<T> implements Observer<T> {
    @Override
    public void onError(Throwable e) {
        try {
            ApiException error = FactoryException.analysisException(e);
            handleError(error.getDisplayMessage(), error.getCode());
        } catch (Throwable ex) {
            // never do anything!
        }
    }

    public void onError() {
        handleError("unknown_error", CodeException.UNKNOWN_ERROR);
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onNext(T t) {
        handleData(t);
    }

    public abstract void handleError(String msg, int msgCode);

    public abstract void handleData(T t);
}
