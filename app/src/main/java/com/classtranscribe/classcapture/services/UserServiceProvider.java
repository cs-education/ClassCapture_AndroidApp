package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.classtranscribe.classcapture.R;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import java.net.CookieManager;
import java.net.CookiePolicy;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by sourabhdesai on 1/15/16.
 */
public class UserServiceProvider {
    private static UserService ourInstance = null;

    public static UserService getInstance(Context context) {
        if (ourInstance != null) {
            return ourInstance;
        }

        // GSON converter with DateTime Type Adapter
        Gson gson = GSONUtils.getConfiguredGsonBuilder()
            .create();

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new DeviceIDRequestInterceptor(context));

        // Set cookie logic for OkHTTP client
        PersistentCookieStore cookieStore = new PersistentCookieStore(context);
        CookieManager cookieManager = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
        client.setCookieHandler(cookieManager);

        // Create rest adapter from RetroFit. Initialize endpoint
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(context.getString(R.string.api_base_url))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

        ourInstance = retrofit.create(UserService.class);

        return ourInstance;
    }
}
