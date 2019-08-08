package io.taucoin.android_app.net;

import com.github.naturs.logger.Logger;
import com.google.gson.Gson;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.taucoin.foundation.net.exception.ApiException;
import io.taucoin.foundation.net.exception.CodeException;
import io.taucoin.foundation.net.exception.FactoryException;

public abstract class MainObserver<T> implements Observer<T> {
    @Override
    public void onError(Throwable e) {
        try {
            handleError(e.getMessage(), 1000);
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
        try {
            Logger.json(new Gson().toJson(t));
        }catch (Exception ignore){ }

    }

    public abstract void handleError(String msg, int msgCode);

    public abstract void handleData(T t);
}
