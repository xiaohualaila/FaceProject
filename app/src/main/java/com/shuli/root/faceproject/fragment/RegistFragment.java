package com.shuli.root.faceproject.fragment;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.activity.FaceServerActivity;
import com.shuli.root.faceproject.activity.MainFragmentActivity;
import com.shuli.root.faceproject.base.BaseFragment;
import com.shuli.root.faceproject.bean.User;
import com.shuli.root.faceproject.retrofit.Api;
import com.shuli.root.faceproject.retrofit.ConnectUrl;
import com.shuli.root.faceproject.utils.ClearEditTextWhite;
import com.shuli.root.faceproject.utils.DataCache;
import com.shuli.root.faceproject.utils.SharedPreferencesUtil;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class RegistFragment extends BaseFragment {
    @BindView(R.id.ct_username)
    ClearEditTextWhite ct_username;
    @BindView(R.id.ct_secret)
    ClearEditTextWhite ct_secret;
    @BindView(R.id.ct_secret_again)
    ClearEditTextWhite ct_secret_again;
    @BindView(R.id.btn_login)
    Button btn_login;
    private DataCache mCache;
    public RegistFragment() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.layout_regist;
    }

    @Override
    protected void init() {
        mCache = new DataCache(getActivity());
    }

    @OnClick({R.id.btn_login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                String username = ct_username.getText().toString().trim();
                String secret = ct_secret.getText().toString().trim();
                String secret_again = ct_secret_again.getText().toString().trim();

                if (TextUtils.isEmpty(username)) {
                    showToastLong("用户名不能为空！");
                    return;
                } else if (TextUtils.isEmpty(secret)) {
                    showToastLong("密码不能为空！");
                    return;
                }
                if (TextUtils.isEmpty(secret_again)) {
                    showToastLong("确认密码不能为空！");
                    return;
                }
                if (!secret.equals(secret_again)) {
                    showToastLong("两次输入密码不一致！");
                    return;
                }
                upload(username,secret);
                break;
        }
    }
    private void upload(String username,String password){
//        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
//                .regist(username, password).enqueue(new Callback<JSONObject>() {
//            @Override
//            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
////                User user = new User();
////                mCache.saveUser(user);
//                // TODO: 2018/5/14 注册成功
//
//                Log.i("sss","response.body().toString() "+ response.body().toString());
//                startActivity(new Intent(getActivity(),FaceServerActivity.class));
//                getActivity().finish();
//                // TODO: 2018/5/14 注册失败
//            }
//
//            @Override
//            public void onFailure(Call<JSONObject> call, Throwable t) {
//
//                Log.i("sss",t.toString());
//            }
//        });
        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
            .regist(username, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<JSONObject>() {
                 @Override
                 public void call(JSONObject jsonObject) {
                     Log.i("sss", jsonObject.toString());
                     if (jsonObject != null) {
                         if (jsonObject.optBoolean("result")) {
                             JSONObject obj = jsonObject.optJSONObject("admin");
                             User user = new User();
                             user.setId(obj.optString("adminId"));
                             user.setLogin(obj.optString("account"));
                             user.setName(obj.optString("adminName"));
                             user.setToken(obj.optString("token"));
                             mCache.saveUser(user);
                             startActivity(new Intent(getActivity(), MainFragmentActivity.class));
                             getActivity().finish();
                         } else {
                             showToastLong(jsonObject.optString("errMsg"));
                         }
                     }
                 }
               }, new Action1<Throwable>() {
                   @Override
                   public void call(Throwable throwable) {
                       showToastLong(throwable.toString());
                   }
               }
            );
    }

}
