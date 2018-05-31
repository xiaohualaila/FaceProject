package com.shuli.root.faceproject.retrofit.api;

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by xyuxiao on 2016/9/23.
 */
public interface BaseApi {

    @POST("adminManage/adminLogin")
    Observable<JSONObject> login(
            @Query("account") String name,
            @Query("password") String secret);

    @POST("adminManage/adminRegister")
    Observable<JSONObject> regist(
            @Query("account") String name,
            @Query("password") String secret);

    @POST("userManage/addUser")
    Observable<JSONObject> uploadFaceToken(
            @Query("faceToken") String faceToken,
            @Query("userName") String name,
            @Query("workNum") String gong_num,
            @Query("MAC") String mac_address);


    @POST("userManage/addUser")
    Observable<JSONObject> addFaceToken(
            @Query("faceToken") String faceToken,
            @Query("userName") String name,
            @Query("workNum") String gong_num,
            @Query("inputDeviceMAC") String mac_address);


    @POST("userManage/userSynchronize")
    Observable<JSONObject> getFaceToken(
            @Query("userId") int count,
            @Query("MAC") String mac_address
    );
}

