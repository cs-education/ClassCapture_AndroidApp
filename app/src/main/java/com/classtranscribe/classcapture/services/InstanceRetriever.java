package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;

import io.gsonfire.PostProcessor;
import io.gsonfire.PreProcessor;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by sourabhdesai on 1/15/16.
 * Interface for defining how to get an instance of any class T given an long ID
 */
public abstract class InstanceRetriever<T> implements RealmQueryUtils.IdGetter<T> {
    protected final Context context;
    public final Class<T> paramClass;

    protected InstanceRetriever(Context context, Class<T> paramClass) {
        this.context = context;
        this.paramClass = paramClass;
    }

    protected abstract T getLocalCopy(Context context, long id);
    public abstract Call<T> getInstanceFromAPI(Context context, long id) throws NoSuchMethodException;
    public abstract void saveLocalCopy(T instance);
    public abstract long getInstanceID(T instance);

    public synchronized T getInstance(long id) throws IOException, NoSuchMethodException {
        return this.getInstance(id, false);
    }

    public synchronized T getInstance(long id, boolean doAPIUpdate) throws IOException, NoSuchMethodException {

        T instance = this.getLocalCopy(this.context, id);

        if (instance == null) {
            Call<T> call = this.getInstanceFromAPI(this.context, id);
            Response<T> response = call.execute();
            if (response.isSuccess()) {
                instance = response.body();
                this.saveLocalCopy(instance);
            } else {
                int status = response.code();
                String msg = response.message();
                String excMsg = String.format("Request got Status %d with message: %s", status, msg);
                throw new RuntimeException(excMsg);
            }
        } else {
            if (doAPIUpdate) {
                this.updateFromAPIAsync(id);
            }
        }

        return instance;
    }

    public synchronized void updateFromAPIAsync(long id) throws NoSuchMethodException {
        Call<T> call = this.getInstanceFromAPI(this.context, id);

        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Response<T> response, Retrofit retrofit) {
                T latestInstance = response.body();
                InstanceRetriever.this.saveLocalCopy(latestInstance);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
