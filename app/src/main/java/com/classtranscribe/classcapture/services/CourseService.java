package com.classtranscribe.classcapture.services;

import com.classtranscribe.classcapture.models.Course;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public interface CourseService {

    @GET("/course")
    void listCourses(Callback<List<Course>> cb);

    @GET("/course/{id}")
    void getCourse(@Path("id") long courseId,Callback<Course> cb);

}
