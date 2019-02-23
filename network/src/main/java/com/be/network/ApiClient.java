package com.be.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Levashkin Konstantin on 11/27/18
 */
public class ApiClient {

    @Getter
    @Setter
    private static String url;
    public static final int REST_API_TIMEOUT = 20;
    @SuppressWarnings("Lombok")
    @Getter @Setter private OkHttpClient httpClient;
    private static volatile ApiClient instance;
    @Getter @Setter private List<Interceptor> interceptors;
    @Getter @Setter private Cache cache;

    public static ApiClient getInstance() {
        ApiClient localInstance = instance;
        if (localInstance == null) {
            synchronized (ApiClient.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new ApiClient();
                }
            }
        }
        return localInstance;
    }

    public <T> T getService(Class<T> c) {
        String baseUrl = url + "";
        if (!baseUrl.endsWith("/"))
            baseUrl = baseUrl + "/";
        return getRetrofitBuilder(baseUrl, false).build().create(c);
    }

    private OkHttpClient getHttpClient() {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.addInterceptor(chain -> {
            Request.Builder builder = chain.request().newBuilder();
            builder.addHeader("X-Requested-With", "XMLHttpRequest");
            return chain.proceed(builder.build());
        });
        okHttpBuilder.connectTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS);
        okHttpBuilder.readTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS);
        okHttpBuilder.writeTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS);
        if (!interceptors.isEmpty()) {
            for (Interceptor interceptor:interceptors) {
                okHttpBuilder.addInterceptor(interceptor);
            }
        }
        if (cache != null) {
            okHttpBuilder.cache(cache);
        }
        OkHttpClient okHttpClient = okHttpBuilder.build();
        return okHttpClient;
    }

    private Retrofit.Builder getRetrofitBuilder(String path, boolean mock) {
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
        retrofitBuilder.baseUrl(path);
        retrofitBuilder.callbackExecutor(Executors.newSingleThreadExecutor());
        retrofitBuilder.addConverterFactory(GsonConverterFactory.create());
        retrofitBuilder.client(getHttpClient());
        return retrofitBuilder;
    }

}
