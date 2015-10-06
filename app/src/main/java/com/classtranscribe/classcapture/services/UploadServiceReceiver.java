package com.classtranscribe.classcapture.services;

import android.util.Log;

import com.alexbbb.uploadservice.AbstractUploadServiceReceiver;
import com.alexbbb.uploadservice.UploadService;

import java.io.File;

/**
 * Created by sourabhdesai on 9/18/15.
 * Will handle events and do some post-upload cleanup for a single specified upload
 */
public class UploadServiceReceiver extends AbstractUploadServiceReceiver {

    final String filepath; // file that is being uploaded
    final String uploadID; // the specific upload that this receiver is for

    public UploadServiceReceiver(String uploadID, String filepath) {
        this.filepath = filepath;
        this.uploadID = uploadID;
    }


    @Override
    public void onProgress(String uploadId, int progress) {
    }

    @Override
    public void onError(String uploadId, Exception exception) {
    }

    @Override
    public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage) {
        if (!uploadId.equals(this.uploadID)) {
            return; // not for this receivers specific upload
        }

        Log.d("yo!", "Upload with ID " + uploadId + "finished:");
        Log.d("yo!", "Status: " + serverResponseCode + "\tMessage: " + serverResponseMessage);
        // In a different thread, make sure the video will be deleted if it still exists
        new Thread(new Runnable() {
            @Override
            public void run() {
                File fileToDelete = new File(UploadServiceReceiver.this.filepath);
                if (fileToDelete.exists()) {
                    fileToDelete.deleteOnExit();
                }
            }
        }).start();

    }
}
