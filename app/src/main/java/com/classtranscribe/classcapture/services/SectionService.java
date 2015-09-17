package com.classtranscribe.classcapture.services;


import com.classtranscribe.classcapture.models.Section;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public interface SectionService {

    @GET("/section")
    void listSections(Callback<List<Section>> cb);

    @GET("/section/{id}")
    void getSection(@Path("id") long courseId,Callback<Section> cb);

}
