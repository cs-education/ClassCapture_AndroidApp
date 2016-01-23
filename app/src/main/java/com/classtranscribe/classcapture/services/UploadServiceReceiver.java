package com.classtranscribe.classcapture.services;

import android.util.Log;

import com.alexbbb.uploadservice.AbstractUploadServiceReceiver;
import com.alexbbb.uploadservice.UploadService;
import com.classtranscribe.classcapture.controllers.fragments.VideoCaptureFragment;
import com.classtranscribe.classcapture.models.Recording;

import java.io.File;

/**
 * Created by sourabhdesai on 9/18/15.
 * Will handle events and do some post-upload cleanup for a single specified upload
 */
public class UploadServiceReceiver extends AbstractUploadServiceReceiver {
    private static final String LOG_TAG = UploadServiceReceiver.class.getName();

    Recording recording;
    VideoCaptureFragment.OnFragmentInteractionListener listener;

    public UploadServiceReceiver(Recording recording, VideoCaptureFragment.OnFragmentInteractionListener listener) {
        this.recording = recording;
        this.listener = listener;
    }


    @Override
    public void onProgress(String uploadId, int progress) {
    }

    @Override
    public void onError(String uploadId, Exception exception) {
        if (!uploadId.equals(String.valueOf(this.recording.getId()))) {
            return; // not for this specific receivers upload
        }

        this.listener.onVideoCaptureUploadFailure(exception, this.recording);
    }

    @Override
    public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage) {
        if (!uploadId.equals(String.valueOf(this.recording.getId()))) {
            return; // not for this specific receivers upload
        }

        Log.d(LOG_TAG, "Upload with ID " + uploadId + "finished:");
        Log.d(LOG_TAG, "Status: " + serverResponseCode + "\tMessage: " + serverResponseMessage);

        if (serverResponseCode >= 200 && serverResponseCode < 300) {
            this.listener.onVideoCaptureUploadSuccess(this.recording);

            // In a different thread, make sure the video will be deleted if it still exists
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File fileToDelete = new File(UploadServiceReceiver.this.recording.getVideoFilePath());
                    if (fileToDelete.exists()) {
                        fileToDelete.delete(); // delete the file
                    }
                }
            }).start();
        }


    }
}
