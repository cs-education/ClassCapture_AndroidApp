package com.classtranscribe.classcapture.models;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.controllers.adapters.RecordingServiceProvider;

import java.io.File;
import java.util.Date;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

/**
 * Created by sourabhdesai on 6/19/15.
 */

/**
 * Example JSON Recording representation:
     {
         startTime: "2015-05-28T01:31:37.073Z",
         endTime: "2016-05-28T01:31:37.073Z",
         filename: "1432776697073_1464399097073_1432776697179_752.mp4",
         id: 4,
         createdAt: "2015-05-28T01:31:37.182Z",
         updatedAt: "2015-05-28T01:31:37.182Z"
     }
 */
public class Recording {

    public Date startTime;
    public Date endTime;
    public String filename;
    public long id;
    public Date createdAt;
    public Date updatedAt;

    public transient Uri videoUri;

    public Recording() {
        // does nothing
    }

    public Recording(Date startTime, Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Recording(Context context, Uri videoUri) {
        this.videoUri = videoUri;

        // Query MediaStore for metadata on retrieved video.
        // Set instance variables according to metadata
        MediaMetadataRetriever metadataRetriever = null;
        try {
            // Extract relevant metadata from Video Uri
            metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(context, videoUri);

            // TODO: Need to figure out how to parse METADATA_KEY_DATE into java.util.Date.
            // Till then, need to backtrack from endtime given duration for start time
            String durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            //String dateStr     = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);

            long duration = Long.parseLong(durationStr);
            //long date     = Long.parseLong(dateStr);

            this.endTime   = new Date();
            this.startTime = new Date(this.endTime.getTime() - duration);
        } finally {
            metadataRetriever.release();
        }
    }

    /**
     * Sets endTime for Recording as the current set startTime plus the given number of milliseconds
     * @param recordingDurationMilli duration of recording in milliseconds
     */
    public void setDuration(int recordingDurationMilli) {
        if (this.startTime == null) {
            throw new IllegalStateException("Need to set Recording Start Time before calling setDuration()");
        }

        if (recordingDurationMilli <= 0) {
            throw new IllegalArgumentException("Duration argument must be >= 0. Given " + recordingDurationMilli);
        }

        this.endTime = plusMillis(this.startTime, recordingDurationMilli);
    }

    /**
     * Adds specified number of milliseconds to given datetime
     * Plus documentation on DateTime docs:
     * http://www.date4j.net/javadoc/hirondelle/date4j/DateTime.html
     * @param date
     * @param millisInFuture
     * @return new DateTime which is millisInFuture milliseconds after dateTime
     */
    public static Date plusMillis(Date date, int millisInFuture) {
        return new Date(date.getTime() + millisInFuture);
    }

    /**
     * @return duration of recording in milliseconds
     */
    public long getDurationMillis() {
        if (this.startTime == null || this.endTime == null) {
            throw new IllegalStateException("Need to set Recording Start & End Time before calling setDuration()");
        } else {
            return this.endTime.getTime() - this.startTime.getTime();
        }
    }

    /**
     * UI friendly, concise Metadata string
     * @return String representing Recording metadata
     */
    @Override
    public String toString() {
        // TODO: Format start and end times to make more human readable
        return this.startTime + " to " + this.endTime;
    }

    /**
     * Given a context, and video uri, will create a new Recording object on the backend
     * and attach the given video to it.
     * @param context
     * @param cb
     */
    public void uploadRecording(final Context context, final Callback<Recording> cb) {
        if (!this.isValidForUpload()) {
            throw new IllegalStateException("Recording is not valid for upload: " + this);
        }

        final RecordingService recordingService = RecordingServiceProvider.getInstance(context);

        recordingService.newRecording(this, new Callback<Recording>() {
            @Override
            public void success(final Recording recording, final Response newRecordingResponse) {
                // Created new recording
                // Now upload the video recording with the filename given by the Recording in the response
                System.err.println("videoUri.getPath()==" + getFilePathFromURI(context, Recording.this.videoUri));
                File videoFile = new File(getFilePathFromURI(context, Recording.this.videoUri));
                TypedFile typedVideoFile = new TypedFile("video/mp4", videoFile);
                // Upload the video
                recordingService.uploadRecordingVideo(recording.filename, typedVideoFile, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response uploadVidResponse) {
                        cb.success(recording, newRecordingResponse);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        cb.failure(error);
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                cb.failure(error);
            }
        });
    }

    public String getVideoURL(Context context) {
        return context.getString(R.string.api_base_url) + "/getVideo/" + this.filename;
    }

    /**
     * Used to validate object when they are ready to be POSTed to backend to create new entity
     * Should consider putting some of these into an abstract class
     * @return boolean representing whether this is safe for upload
     */
    public boolean isValidForUpload() {
        return this.startTime != null && this.endTime != null;
    }

    /**
     * From here: http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
     * uri.getPath() doesnt return actual file path for a video. Need to do these queries to retrieve it.
     * @param context
     * @param contentUri
     * @return String of actual filepath to video
     */
    public static String getFilePathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
