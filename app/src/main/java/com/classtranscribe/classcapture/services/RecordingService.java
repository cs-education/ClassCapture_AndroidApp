package com.classtranscribe.classcapture.services;

import com.classtranscribe.classcapture.models.Recording;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;

/**
 * Created by sourabhdesai on 6/19/15.
 */
public interface RecordingService {

    @GET("/recording")
    void listRecordings(Callback<List<Recording>> cb);

    @GET("/recording/{id}")
    void getRecording(@Path("id") long recordingId, Callback<Recording> cb);

    @POST("/recording")
    void newRecording(@Body Recording recording, Callback<Recording> cb);

    @Multipart
    @POST("/video/{videoname}")
    void uploadRecordingVideo(@Path("videoname") String videoname, @Part("video") TypedFile videoFile, Callback<Object> cb);
}
