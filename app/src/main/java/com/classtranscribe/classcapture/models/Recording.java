package com.classtranscribe.classcapture.models;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.alexbbb.uploadservice.UploadRequest;
import com.alexbbb.uploadservice.UploadService;
import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.classtranscribe.classcapture.services.RecordingService;
import com.classtranscribe.classcapture.services.RecordingServiceProvider;
import com.classtranscribe.classcapture.services.SettingsService;
import com.classtranscribe.classcapture.services.UploadServiceReceiver;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import io.realm.RealmObject;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

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

    private Date startTime;
    private Date endTime;
    private String filename;
    private long id = -1;
    private long section = -1; // Id for section that this recording is for
    private Date createdAt;
    private Date updatedAt;

    private String videoFilePath;

    private boolean isFromDatabase; // specifies whether this recording was constructed from a API response

    public Recording() {
        // does nothing
    }

    public Recording(Date startTime, Date endTime, long section) {
        this.isFromDatabase = false;
        this.startTime = startTime;
        this.endTime = endTime;
        this.section = section;
    }

    public Recording(Context context, Uri videoUri, long section) {
        this.isFromDatabase = false;
        this.videoFilePath = getFilePathFromURI(context, videoUri);
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
            String durationStr  = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String startDateStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);

            Log.d("yo!", "dateStr = " + startDateStr);

            TimeZone timeZone = TimeZone.getDefault();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS");
            dateFormat.setTimeZone(timeZone);

            long duration      = Long.parseLong(durationStr);
            long startDateTime = dateFormat.parse(startDateStr).getTime();
            Log.d("yo!", "startDateTime: " + startDateTime);
            Log.d("yo!", "duration: " + duration);
            this.startTime = new Date(startDateTime);
            this.endTime   = new Date(startDateTime + duration);
        } catch (ParseException e) {
            e.printStackTrace();
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

    @Override
    public boolean equals(Object other) {
        if (! (other instanceof Recording)) {
            return false;
        }

        Recording otherRecording = (Recording) other;

        if (this.id >= 0 && otherRecording.id >= 0) {
            return this.id == otherRecording.id;
        } else if (this.startTime != null && this.endTime != null && otherRecording.startTime != null && otherRecording.endTime != null) {
            return this.startTime.equals(otherRecording.startTime) && this.endTime.equals(otherRecording.endTime);
        }

        return false;
    }

    /**
     * Copies over all NON-null fields from input recording to this recording
     * @param other recording to copy fields over form
     */
    private void copy(Recording other) {
        this.startTime = other.startTime != null ? this.startTime : other.startTime;
        this.endTime = other.endTime != null ? this.endTime : other.endTime;
        this.filename = other.filename != null ? this.filename : other.filename;
        this.id = other.id >= 0 ? this.id : other.id;
        this.section = other.section >= 0 ? this.section : other.section; // Id for section that this recording is for
        this.createdAt = other.createdAt != null ? this.createdAt : other.createdAt;
        this.updatedAt = other.updatedAt != null ? this.updatedAt : other.updatedAt;

        this.videoFilePath = other.videoFilePath != null ? this.videoFilePath : other.videoFilePath;

        this.isFromDatabase =this.isFromDatabase || other.isFromDatabase; // specifies whether this recording was constructed from a API response
    }

    /**
     * Given a context, and video uri, will create a new Recording object on the backend
     * and will also upload the corresponding video file to it.
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
            public void onResponse(Response<Recording> response, Retrofit retrofit) {
                // Created new recording
                // Now upload the video recording with the filename given by the Recording in the response
                // Upload the video in a service
                Recording recording = response.body();
                recording.isFromDatabase = true;
                Recording.this.copy(recording); // update this recordings fields with response fields

                final String uploadID = String.valueOf(recording.id); // Can just make upload ID be the ID of the recording
                final String uploadURL = recording.getVideoURL(mainActivity);
                final String deviceID = SettingsService.getDeviceID(mainActivity); // to add to header
                final String deviceIDHeader = mainActivity.getString(R.string.device_id_header);
                final String videoParamName = mainActivity.getString(R.string.video_upload_tag);

                UploadRequest request = new UploadRequest(mainActivity, uploadID, uploadURL);
                request.addHeader(deviceIDHeader, deviceID); // add header, currently used on backend for security validation
                request.addFileToUpload(Recording.this.videoFilePath, videoParamName, recording.filename, VIDEO_MIME_TYPE);

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
                UploadServiceReceiver receiver = new UploadServiceReceiver(recording, mainActivity);
                receiver.register(mainActivity);
                // send the request
                try {
                    UploadService.startUpload(request);
                    cb.onResponse(response, retrofit); // bubble up success via cb
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.upload_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Throwable error) {
                cb.onFailure(error);
            }
        });
    }

    /**
     * Will create a new Recording entry in the database. Will update its own fields with the fields retrieved by API response
     * WILL NOT UPLOAD CORRESPONDING VIDEO. LOOK AT `uploadVideoSync` FOR THAT.
     * @param context
     * @throws IOException
     */
    public void uploadRecordingSync(Context context) throws IOException {
        if (!this.isValidForUpload()) {
            throw new IllegalStateException("Recording is not valid for upload: " + this);
        }

        RecordingService recordingService = RecordingServiceProvider.getInstance(context);

        Call<Recording> call = recordingService.newRecording(this);
        Response<Recording> response = call.execute();
        Recording dbRecording = response.body();
        dbRecording.isFromDatabase = true;

        this.copy(dbRecording); // copy response fields into this recording
    }

    /**
     * Will upload video file that is attached to this recording to the backend.
     * @param context
     * @throws IOException
     */
    public void uploadVideoSync(Context context) throws IOException {
        RecordingService recordingService = RecordingServiceProvider.getInstance(context);
        MediaType mediaType = MediaType.parse(VIDEO_MIME_TYPE);
        RequestBody body = RequestBody.create(mediaType, new File(this.videoFilePath));
        Call videoUploadCall = recordingService.uploadRecordingVideo(this.filename, body);
        Response uploadResponse = videoUploadCall.execute();

        if (!uploadResponse.isSuccess()) {
            throw new IOException("Request to upload video was met with response with status code " + uploadResponse.code());
        }
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
        return this.startTime != null && this.endTime != null && this.videoFilePath != null;
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

    /*
     * Vanilla getters and setters
     */

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSection() {
        return section;
    }

    public void setSection(long section) {
        this.section = section;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }

    /**
     * specifies whether this recording was constructed from a API response
     * @return flag representing whether object is from API or constructed on device
     */
    public boolean isFromDatabase() {
        return isFromDatabase;
    }

}
