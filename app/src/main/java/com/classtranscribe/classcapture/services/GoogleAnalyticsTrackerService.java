package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.classtranscribe.classcapture.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by sourabhdesai on 11/23/15.
 */
public class GoogleAnalyticsTrackerService {

    private static Tracker defaultTracker;

    synchronized public static Tracker getDefaultTracker(Context context) {
        if (defaultTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            defaultTracker = analytics.newTracker(R.xml.global_tracker);
        }

        return defaultTracker;
    }

}
