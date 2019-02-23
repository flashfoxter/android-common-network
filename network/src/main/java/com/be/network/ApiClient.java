package com.be.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
    private static volatile ApiClient instance;
    @Getter
    @Setter
    private List<Interceptor> interceptors;
    @Getter
    @Setter
    private Cache cache;

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

    public <T> T  getServiceLocal(Class<T> c) {
        String baseUrl = url + "";
        if (!baseUrl.endsWith("/"))
            baseUrl = baseUrl + "/";
        return getRetrofitBuilderLocal(baseUrl, false).build().create(c);
    }

    private OkHttpClient getHttpClientLocal() {
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
            for (Interceptor interceptor : interceptors) {
                okHttpBuilder.addInterceptor(interceptor);
            }
        }
        if (cache != null) {
            okHttpBuilder.cache(cache);
        }
        OkHttpClient okHttpClient = okHttpBuilder.build();
        return okHttpClient;
    }

    private Retrofit.Builder getRetrofitBuilderLocal(String path, boolean mock) {
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
        retrofitBuilder.baseUrl(path);
        retrofitBuilder.callbackExecutor(Executors.newSingleThreadExecutor());
        retrofitBuilder.addConverterFactory(GsonConverterFactory.create());
        retrofitBuilder.client(getHttpClientLocal());
        return retrofitBuilder;
    }

    @Deprecated
    public static <T> T getService(Class<T> c) {
        String baseUrl = url + "";
        if (!baseUrl.endsWith("/"))
            baseUrl = baseUrl + "/";
        return getRetrofitBuilder(baseUrl, false).build().create(c);
    }

    @Deprecated
    private static OkHttpClient getHttpClient() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request.Builder builder = chain.request().newBuilder();
            builder.addHeader("X-Requested-With", "XMLHttpRequest");
            return chain.proceed(builder.build());
        })
                .connectTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(REST_API_TIMEOUT, TimeUnit.SECONDS);


        addUnsafeTrust(httpClientBuilder);
        return httpClientBuilder.build();
    }

    @Deprecated
    private static Retrofit.Builder getRetrofitBuilder(String path, boolean mock) {
        return new Retrofit.Builder()
                .baseUrl(path)
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .addConverterFactory(GsonConverterFactory.create())
                .client(getHttpClient());
    }

    private static void addUnsafeTrust(OkHttpClient.Builder builder) {

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);
    }

}