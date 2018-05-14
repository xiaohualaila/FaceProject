package com.shuli.root.faceproject.retrofit.api;

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.POST;
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


    @POST("face.php")
    Call<ResponseBody> regist(@Field("username") String name,
                              @Field("password") String secret);


}

