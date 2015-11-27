package com.classtranscribe.classcapture.android_services;

import android.app.IntentService;
import android.content.Intent;

import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.services.UploadQueueProvider;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class VideoUploadIntentService extends IntentService {
    public static final String ACTION_UPLOAD = "com.classtranscribe.classcapture.android_services.action.UPLOAD";

    public VideoUploadIntentService() {
        super("VideoUploadIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                try {
                    this.handleActionUpload();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Handle action Upload in the provided background thread with the provided
     * parameters.
     *
     * Will pull Recording objects off of the Queue (Realm DB instance) and try to upload them
     */
    private void handleActionUpload() throws IOException {
        List<Recording> uploadQueue = UploadQueueProvider.getUploadQueue(this);
        Iterator<Recording> it = uploadQueue.iterator();
        while (it.hasNext()) {
            try {
                Recording dbRecording = it.next();
                // Only create the entry in the backend if its not already there. Check the fromDatabase flag for that.
                if (!dbRecording.isFromDatabase()) {
                    dbRecording.uploadRecordingSync(this);
                }
                // upload the video
                dbRecording.uploadVideoSync(this);
                it.remove(); // remove it from the list if the upload was succesfull
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        UploadQueueProvider.saveUploadQueue(this, uploadQueue);
    }
}
