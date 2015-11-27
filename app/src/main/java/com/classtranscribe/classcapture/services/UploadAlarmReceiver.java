package com.classtranscribe.classcapture.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.classtranscribe.classcapture.android_services.VideoUploadIntentService;

public class UploadAlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 62345; // Random but unique code
    public UploadAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Intent i = new Intent(context, VideoUploadIntentService.class);
        context.startService(i);
    }
}
