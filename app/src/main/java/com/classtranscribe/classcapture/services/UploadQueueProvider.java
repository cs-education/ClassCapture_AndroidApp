package com.classtranscribe.classcapture.services;

import android.content.Context;
import android.content.SharedPreferences;

import com.classtranscribe.classcapture.models.Recording;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by sourabhdesai on 11/26/15.
 *
 * Layer over app's private filesystem storage of Recordings that need to be uploaded.
 *
 * Recordings that need to uploaded are put into the upload queue and
 * can be pulled off of it once they are uploaded
 */
public class UploadQueueProvider {

    public static final String UPLOAD_QUEUE_FILE_NAME = "upload_queue_file";

    public static synchronized void addToUploadQueue(Context context, Recording recording) throws IOException {
        // Add the recording to the upload queue
        List<Recording> uploadQueue = getUploadQueue(context);
        uploadQueue.add(recording);
        saveUploadQueue(context, uploadQueue);
    }

    public static synchronized void saveUploadQueue(Context context, List<Recording> newUploadQueue) throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            // Open the file that we will be writing the queue to
            fileOutputStream = context.openFileOutput(UPLOAD_QUEUE_FILE_NAME, Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(fileOutputStream));
            // Convert the queue to JSON tree form
            Gson recordingGson = GSONUtils.getConfiguredGsonBuilder().create();
            JsonElement jsonFormQueue = recordingGson.toJsonTree(newUploadQueue);
            // Write the JSON tree form of the queue to disk
            recordingGson.toJson(jsonFormQueue, writer);
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

    public static synchronized List<Recording> getUploadQueue(Context context) throws IOException {
        Gson recordingGson = GSONUtils.getConfiguredGsonBuilder().create();
        TypeAdapter<List<Recording>> queueAdapter = recordingGson.getAdapter(new TypeToken<List<Recording>>() {});

        FileInputStream queueInputStream = null;
        try {
            queueInputStream = context.openFileInput(UPLOAD_QUEUE_FILE_NAME);
            List<Recording> uploadQueue = queueAdapter.fromJson(new InputStreamReader(queueInputStream));
            return uploadQueue;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<Recording>();
        } finally {
            if (queueInputStream != null) {
                queueInputStream.close();
            }
        }
    }

    /**
     * Will delete the given recording from the recording list held in local storage
     * @param context curr context
     * @param recording recording to remove from recording list
     * @return Recording object that was deleted
     */
    public static synchronized Recording deleteFromRecordingQueue(Context context, Recording recording) throws IOException {
        List<Recording> uploadQueue = getUploadQueue(context);
        boolean wasRemoved = uploadQueue.remove(recording);

        if (!wasRemoved) {
            throw new IllegalArgumentException("Recording [" + recording + "] was not found in the upload queue and thus can't be removed");
        }

        return recording;
    }

    /**
     * Will delete the given recording from the recording list held in local storage
     * @param context curr context
     * @param id id for recording to remove from recording list
     * @return Recording object that was passed in
     */
    public static synchronized Recording deleteFromRecordingQueue(Context context, long id) throws IOException {
        List<Recording> uploadQueue = getUploadQueue(context);
        Iterator<Recording> it = uploadQueue.iterator();

        while (it.hasNext()) {
            Recording currRecording = it.next();
            if (currRecording.getId() == id) {
                it.remove();
                return currRecording;
            }
        }

        throw new IllegalArgumentException("Recording [id=" + id + "] was not found in the upload queue and thus can't be removed");
    }

}
