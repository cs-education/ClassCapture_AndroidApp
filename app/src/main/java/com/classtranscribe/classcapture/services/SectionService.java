package com.classtranscribe.classcapture.services;


import com.classtranscribe.classcapture.models.Section;

import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public interface SectionService {

    @GET("/api/section")
    Call<List<Section>> listSections();

    @GET("/api/section/{id}")
    Call<Section> getSection(@Path("id") long courseId);

}
