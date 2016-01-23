package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by sourabhdesai on 10/6/15.
 * Thin layer over retrofit callbacks
 * Will simply send GoogleAnalytics a hit even on failure events
 * In essence, meant to replace the RetrofitErrorHandler that was lost in the upgrade to Retrofit v2.0
 */
public abstract class CustomCB<T> implements Callback<T> {

    private final Context context;
    private final String errMsgPrefix;

    public CustomCB(Context context, String errMsgPrefix) {
        this.context = context;
        this.errMsgPrefix = errMsgPrefix;
    }

    public CustomCB(Context context, Class<?> clazz) {
        this.context = context;
        this.errMsgPrefix = clazz.getSimpleName();
    }

    @Override
    public abstract void onResponse(Response<T> response, Retrofit retrofit);

    public abstract void onRequestFailure(Throwable t);

    @Override
    public void onFailure(Throwable t) {
        Tracker tracker = GoogleAnalyticsTrackerService.getDefaultTracker(this.context);
        tracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(this.errMsgPrefix + ": " + t.getMessage())
                .build());
        this.onRequestFailure(t);
    }
}
