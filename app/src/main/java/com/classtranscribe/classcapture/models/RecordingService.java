package com.classtranscribe.classcapture.models;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by sourabhdesai on 6/19/15.
 */
public interface RecordingService {

    @GET("/recording")
    void recordingList(Callback<List<Recording>> cb);

    @GET("/recording/{id}")
    Recording getRecording(@Path("id") long recordingId, Callback<Recording> cb);
}
