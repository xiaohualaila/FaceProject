package com.shuli.root.faceproject.retrofit.api;

import org.json.JSONObject;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by xyuxiao on 2016/9/23.
 */
public interface BaseApi {

    @POST("face.php")
    Observable<JSONObject> login(
            @Query("username") String name,
            @Query("password") String secret
    );

    @POST("Api.php")
    Observable<JSONObject> uploadPhotoBase(
            @Query("deviceid") String deviceid,
            @Query("ticketid") String ticketid,
            @Query("xinCode") String xinCode,
            @Query("type") int type,
            @Part List<MultipartBody.Part> file
    );



}

