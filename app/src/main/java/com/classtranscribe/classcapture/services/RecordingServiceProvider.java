package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.classtranscribe.classcapture.R;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.squareup.okhttp.OkHttpClient;

import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by sourabhdesai on 6/23/15.
 * Singleton wrapper over RecordingService instance created by Retrofit api
 * Docs for retrofit showing how this is done:
 *  http://square.github.io/retrofit/#restadapter-configuration
 */
public class RecordingServiceProvider {
    private static RecordingService ourInstance = null;

    public static RecordingService getInstance(Context context) {
        if (ourInstance != null) {
            return ourInstance;
        }

        // GSON converter with DateTime Type Adapter
        Gson gson = GSONUtils.getConfiguredGsonBuilder().create();

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

        ourInstance = retrofit.create(RecordingService.class);

        return ourInstance;
    }

    private static class LongIDDeserializer implements JsonDeserializer<Long> {

        @Override
        public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                // We know that its probably just the Recording ID, so its already a long
                return json.getAsLong();
            } else if (json.isJsonObject()) {
                JsonObject courseObj = json.getAsJsonObject();
                return courseObj.get("id").getAsLong();
            }

            throw new IllegalStateException("Got JSON Element with unexpected format");
        }
    }
}
