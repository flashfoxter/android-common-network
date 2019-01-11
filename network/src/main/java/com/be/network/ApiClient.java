package com.be.network;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Levashkin Konstantin on 11/27/18
 */
public class ApiClient {

    public static final String url = null;

    public static final int REST_API_TIMEOUT = 20;

    //public static Converter.Factory converterFactory;

    public static <T> T getService(Class<T> c) {
        String baseUrl = url + "";
        if (!baseUrl.endsWith("/"))
            baseUrl = baseUrl + "/";
        return getRetrofitBuilder(baseUrl, false).build().create(c);
    }

    private static OkHttpClient getHttpClient() {
        return new OkHttpClient.Builder().addInterceptor(chain -> {
            Request.Builder builder = chain.request().newBuilder();
            builder.addHeader("X-Requested-With", "XMLHttpRequest");
            return chain.proceed(builder.build());
        })
                .connectTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    private static Retrofit.Builder getRetrofitBuilder(String path, boolean mock) {
        return new Retrofit.Builder()
                .baseUrl(path)
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .addConverterFactory(GsonConverterFactory.create())
                .client(getHttpClient());
    }

}
