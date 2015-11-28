package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.realm.RealmObject;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.GsonConverter;

/**
 * Created by sourabhdesai on 9/17/15.
 * Singleton wrapper over CourseService instance created by Retrofit api
 * Docs for retrofit showing how this is done:
 *  http://square.github.io/retrofit/#restadapter-configuration
 */
public class CourseServiceProvider {
    private static CourseService ourInstance = null;

    public static CourseService getInstance(final MainActivity mainActivity) {
        if (ourInstance != null) {
            return ourInstance;
        }

        // GSON converter with DateTime Type Adapter
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                // Realm docs say this: https://realm.io/docs/java/latest/#retrofit
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();

        // Create rest adapter from RetroFit. Initialize endpoint
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(mainActivity.getString(R.string.api_base_url))
                .setRequestInterceptor(new DeviceIDRequestInterceptor(mainActivity))
                // Want to send request errors to google analytics
                .setErrorHandler(new RetrofitErrorHandler(mainActivity, "Course"))
                .setConverter(new GsonConverter(gson))
                .build();

        ourInstance = restAdapter.create(CourseService.class);

        return ourInstance;
    }
}
