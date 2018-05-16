package com.shuli.root.faceproject.fragment;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.activity.FaceServerActivity;
import com.shuli.root.faceproject.base.BaseFragment;
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
        Api.getBaseApiWithGson(ConnectUrl.URL).regist(username, password).enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
//                User user = new User();
//                mCache.saveUser(user);
                    System.out.println(response.toString());

                // TODO: 2018/5/14 注册成功
                System.out.println(response.body().toString());
//                SharedPreferencesUtil.save("username", ct_username.getText().toString(), getActivity());
//                SharedPreferencesUtil.save("pwd", ct_secret.getText().toString(), getActivity());
                startActivity(new Intent(getActivity(),FaceServerActivity.class));
                getActivity().finish();
                // TODO: 2018/5/14 注册失败
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) {

                Log.i("sss",t.toString());
            }
        });
    }

}
