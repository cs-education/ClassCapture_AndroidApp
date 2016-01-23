package com.classtranscribe.classcapture.services;

import com.classtranscribe.classcapture.models.Course;

import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public interface CourseService {

    @GET("/api/course")
    void listCourses(Callback<List<Course>> cb);

    @GET("/api/course/{id}")
    Call<Course> getCourse(@Path("id") long courseId);

}
