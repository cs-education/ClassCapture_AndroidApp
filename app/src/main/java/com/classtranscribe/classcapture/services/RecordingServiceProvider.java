package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.classtranscribe.classcapture.R;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import io.realm.RealmObject;
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
                // converts long course field into the course's ID as a long, even though its received as a JSON course
                .registerTypeAdapter(Long.TYPE, new LongIDDeserializer())
                .create();

        // Create rest adapter from RetroFit. Initialize endpoint
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(context.getString(R.string.api_base_url))
                .setRequestInterceptor(new DeviceIDRequestInterceptor(context))
                .setConverter(new GsonConverter(gson))
                .build();

        ourInstance = restAdapter.create(RecordingService.class);

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
