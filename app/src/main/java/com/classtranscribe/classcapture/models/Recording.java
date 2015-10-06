package com.classtranscribe.classcapture.models;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.alexbbb.uploadservice.ContentType;
import com.alexbbb.uploadservice.UploadRequest;
import com.alexbbb.uploadservice.UploadService;
import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.classtranscribe.classcapture.services.RecordingServiceProvider;
import com.classtranscribe.classcapture.services.RecordingService;
import com.classtranscribe.classcapture.services.SettingsService;
import com.classtranscribe.classcapture.services.UploadServiceReceiver;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Date;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

/**
 * Created by sourabhdesai on 6/19/15.
 * Recordings will not be saved in Realm, they need to be retrieved from backend via Retrofit HTTP Client Methods
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

    private static final String VIDEO_MIME_TYPE = "video/mp4";

    public Date startTime;
    public Date endTime;
    public String filename;
    public long id;
    public long section; // Id for section that this recording is for
    public Date createdAt;
    public Date updatedAt;

    public transient Uri videoUri;

    public Recording() {
        // does nothing
    }

    public Recording(Date startTime, Date endTime, long section) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.section = section;
    }

    public Recording(Context context, Uri videoUri, long section) {
        this.videoUri = videoUri;
        this.section = section;

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
     * @param mainActivity
     * @param cb
     */
    public void uploadRecording(final MainActivity mainActivity, final Callback<Recording> cb) {
        if (!this.isValidForUpload()) {
            throw new IllegalStateException("Recording is not valid for upload: " + this);
        }

        final RecordingService recordingService = RecordingServiceProvider.getInstance(mainActivity);

        recordingService.newRecording(this, new Callback<Recording>() {
            @Override
            public void success(final Recording recording, final Response newRecordingResponse) {
                // Created new recording
                // Now upload the video recording with the filename given by the Recording in the response
                // Upload the video in a service
                final String videoFilePath = getFilePathFromURI(mainActivity, Recording.this.videoUri);
                final String uploadID = String.valueOf(recording.id); // Can just make upload ID be the ID of the recording
                final String uploadURL = recording.getVideoURL(mainActivity);
                final String deviceID = SettingsService.getDeviceID(mainActivity); // to add to header
                final String deviceIDHeader = mainActivity.getString(R.string.device_id_header);
                final String videoParamName = mainActivity.getString(R.string.video_upload_tag);

                UploadRequest request = new UploadRequest(mainActivity, uploadID, uploadURL);
                request.addHeader(deviceIDHeader, deviceID); // add header, currently used on backend for security validation
                request.addFileToUpload(videoFilePath, videoParamName, recording.filename, VIDEO_MIME_TYPE);

                // Messages to display upon various events during upload
                request.setNotificationConfig(R.drawable.ic_launcher, mainActivity.getString(R.string.app_name),
                        mainActivity.getString(R.string.uploading), mainActivity.getString(R.string.upload_success),
                        mainActivity.getString(R.string.upload_error), false);

                // set the intent to perform when the user taps on the upload notification.
                // currently tested only with intents that launches an activity
                // if you comment this line, no action will be performed when the user taps on the notification
                Intent notifClickIntent = new Intent(mainActivity, MainActivity.class);
                request.setNotificationClickIntent(notifClickIntent);

                // set the maximum number of automatic upload retries on error
                request.setMaxRetries(1);

                // register a broadcast receiver for the request
                UploadServiceReceiver receiver = new UploadServiceReceiver(uploadID, videoFilePath);
                receiver.register(mainActivity);
                // send the request
                try {
                    UploadService.startUpload(request);
                    cb.success(recording, newRecordingResponse); // bubble up success via cb
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.upload_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                cb.failure(error);
            }
        });
    }

    /**
     * Will get URL to post the video for this recording to
     * @param context context of the application in its current state
     * @return HTTP url for video upload of this recording
     */
    public String getVideoURL(Context context) {
        if (this.filename == null) {
            throw new IllegalStateException("Recording that tried to be uploaded doesn't have a filename");
        }

        return context.getString(R.string.api_base_url) + "/video/" + this.filename;
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
