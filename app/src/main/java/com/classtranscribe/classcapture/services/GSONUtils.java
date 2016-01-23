package com.classtranscribe.classcapture.services;

import android.content.Context;
import android.util.Log;

import com.classtranscribe.classcapture.models.Course;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.models.Section;
import com.classtranscribe.classcapture.models.User;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.gsonfire.GsonFireBuilder;
import io.gsonfire.PostProcessor;
import io.gsonfire.PreProcessor;
import io.realm.Realm;
import io.realm.RealmObject;

/**
 * Created by sourabhdesai on 1/15/16.
 */
public class GSONUtils {

    private static GsonBuilder instance;

    public static GsonBuilder getConfiguredGsonBuilder() {
        if (instance == null) {
            instance = getBaseGsonBuilder();
        }

        return instance;
    }

    public static GsonBuilder getBaseGsonBuilder() {
        return new GsonFireBuilder()
                .registerPreProcessor(Recording.class, new RecordingPreProcessor())
                .registerPostProcessor(Course.class, new RealmObjectSaver<Course>())
                .registerPostProcessor(Section.class, new RealmObjectSaver<Section>())
                .registerPostProcessor(User.class, new RealmObjectSaver<User>())
                .createGsonBuilder()
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
                });
    }

    public static class RealmObjectSaver<T extends RealmObject> implements PostProcessor<T> {

        @Override
        public void postDeserialize(T result, JsonElement src, Gson gson) {
            Realm realm = Realm.getDefaultInstance();
            try {
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(result);
                realm.commitTransaction();
            } finally {
                realm.close();
            }
        }

        @Override
        public void postSerialize(JsonElement result, T src, Gson gson) {
            // Does nothing
        }
    }

    private static class RecordingPreProcessor<Recording> implements PreProcessor<Recording> {
        @Override
        public void preDeserialize(Class<? extends Recording> clazz, JsonElement src, Gson gson) {
            JsonObject recordingObj = src.getAsJsonObject();
            // All recording objects should have a section property but this is just in case
            if (recordingObj.has("section")) {
                JsonElement sectionElement = recordingObj.get("section");

                if (sectionElement.isJsonObject()) {
                    // Replace the section field with just the ID
                    JsonObject sectionObj = sectionElement.getAsJsonObject();
                    long sectionID = sectionObj.get("id").getAsLong();
                    recordingObj.addProperty("section", sectionID);
                }
            }
        }
    }

}
