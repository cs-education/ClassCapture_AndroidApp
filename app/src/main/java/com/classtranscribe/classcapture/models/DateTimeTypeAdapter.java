package com.classtranscribe.classcapture.models;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import hirondelle.date4j.DateTime;

/**
 * Created by sourabhdesai on 6/19/15.
 *
 * Copied from here: http://stackoverflow.com/questions/18786243/use-gson-with-date4j-datetime
 * This was referenced in the Date4j docs
 */
public final class DateTimeTypeAdapter extends TypeAdapter<DateTime> {
    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == java.sql.Date.class
                    ? (TypeAdapter<T>) new DateTimeTypeAdapter() : null;
        }
    };

    @Override
    public synchronized DateTime read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String s = in.nextString();
        return new DateTime(s);

    }

    @Override
    public synchronized void write(JsonWriter out, DateTime value) throws IOException {
        out.value(value == null ? null : value.toString());
    }
}