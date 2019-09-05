package io.taucoin.android.wallet.net.service;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface FileLoad {
    @GET("{pathParam}")
    Call<ResponseBody> loadFile(@Path("pathParam") String pathParam);

    @GET("{pathParam}")
    Call<ResponseBody> loadTagsFile(@Path("pathParam") String pathParam, @QueryMap Map<String,String> queryMap);
}
