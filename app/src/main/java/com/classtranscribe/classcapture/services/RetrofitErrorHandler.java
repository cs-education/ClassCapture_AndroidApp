package com.classtranscribe.classcapture.services;

import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;

/**
 * Created by sourabhdesai on 10/6/15.
 */
public class RetrofitErrorHandler implements ErrorHandler {

    private final MainActivity mainActivity;
    private final String errMsgPrefix;

    public RetrofitErrorHandler(MainActivity mainActivity, String errMsgPrefix) {
        this.mainActivity = mainActivity;
        this.errMsgPrefix = errMsgPrefix;
    }

    @Override
    public Throwable handleError(RetrofitError cause) {
        Tracker tracker = this.mainActivity.getDefaultTracker();
        tracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(this.errMsgPrefix + ": " + cause.getUrl())
                .build());
        return cause;
    }
}
