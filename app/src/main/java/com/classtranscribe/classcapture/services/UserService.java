package com.classtranscribe.classcapture.services;

import com.classtranscribe.classcapture.models.User;

import java.util.HashMap;
import java.util.Map;

import retrofit.Call;
import retrofit.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by sourabhdesai on 1/13/16.
 */
public interface UserService {

    @GET("/api/user/:id")
    Call<User> getUser(@Path("id") long id);

    @GET("/api/user/me")
    Call<User> me();

    @POST("/api/user/login")
    Call<User> login(@Body HashMap<String, String> body);

    @POST("/api/user/logout")
    Call<Object> logout();
}
