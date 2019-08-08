package io.taucoin.android_app.net;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@SuppressLint("StaticFieldLeak")
public class NetWorkManager {

    private static NetWorkManager mInstance;
    private static Retrofit retrofitMain;


    public static NetWorkManager getInstance() {
        if (mInstance == null) {
            synchronized (NetWorkManager.class) {
                if (mInstance == null) {
                    mInstance = new NetWorkManager();
                }
            }
        }
        return mInstance;
    }

    public void init() {

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.readTimeout(30, TimeUnit.SECONDS);
        okHttpClientBuilder.writeTimeout(30, TimeUnit.SECONDS);
        // Set request timeout
        okHttpClientBuilder.connectTimeout(30, TimeUnit.SECONDS);


        OkHttpClient okHttpClient = okHttpClientBuilder.build();

        // init main retrofit
        retrofitMain = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("http://mainnet.taucoin.io:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                // Configure callback libraries using RxJava
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

    }

    public static <T> T createMainApiService(Class<T> tClass) {
        return retrofitMain.create(tClass);
    }

}