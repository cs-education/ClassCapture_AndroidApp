package com.classtranscribe.classcapture.services;

import com.classtranscribe.classcapture.models.Recording;
import com.squareup.okhttp.RequestBody;

import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;

/**
 * Created by sourabhdesai on 6/19/15.
 */
public interface RecordingService {

    @GET("/api/recording")
    Call<List<Recording>> listRecordings();

    @GET("/api/recording/{id}")
    Call<Recording> getRecording(@Path("id") long recordingId);

    @POST("/api/recording")
    Call<Recording> newRecording(@Body Recording recording);

    @Multipart
    @POST("/api/video/{videoname}")
    void uploadRecordingVideo(@Path("videoname") String videoname, @Part("video") RequestBody videoFile, Callback<Object> cb);

    @Multipart
    @POST("/api/video/{videoname}")
    Call uploadRecordingVideo(@Path("videoname") String videoname, @Part("video") RequestBody videoFile);

}
