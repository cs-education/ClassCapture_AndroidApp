package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.services.RecordingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

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
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();

        // Create rest adapter from RetroFit. Initialize endpoint
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(context.getString(R.string.api_base_url))
                .setConverter(new GsonConverter(gson))
                .build();

        ourInstance = restAdapter.create(RecordingService.class);

        return ourInstance;
    }
}
