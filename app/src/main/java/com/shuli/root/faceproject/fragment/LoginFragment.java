package com.shuli.root.faceproject.fragment;


import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.shuli.root.faceproject.R;
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
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class LoginFragment extends BaseFragment {
    @BindView(R.id.ct_username)
    ClearEditTextWhite ct_username;
    @BindView(R.id.ct_secret)
    ClearEditTextWhite ct_secret;
    @BindView(R.id.checkbox)
    ImageView checkbox;
    private DataCache mCache;
    public LoginFragment() {

    }
    private OnFragmentInteractionListener mListener;
    @Override
    protected int getLayoutId() {
        return R.layout.layout_login;
    }

    @Override
    protected void init() {
        mCache = new DataCache(getActivity());
        String tag = SharedPreferencesUtil.getStringByKey("tag", getActivity());
        initLoginContent(tag);
    }

    private void initLoginContent(String tag) {
        if (tag != null) {
            checkbox.setImageResource(R.drawable.login_beixuan);

        } else {
            checkbox.setImageResource(R.drawable.login_weixuan);
        }
        String local_name = SharedPreferencesUtil.getStringByKey("username", getActivity());
        if (local_name != null) {
            ct_username.setText(local_name);
        }
        String local_pwd = SharedPreferencesUtil.getStringByKey("pwd", getActivity());
        if (local_pwd != null) {
            ct_secret.setText(local_pwd);
        }
    }


    @OnClick({R.id.checkbox, R.id.btn_login,R.id.tv_regist})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                String username = ct_username.getText().toString();
                String password = ct_secret.getText().toString();
    
                if(TextUtils.isEmpty(username)){
                    showToastLong("用户名不能为空！");
                       return;
                }else if(TextUtils.isEmpty(password)){
                    showToastLong("管理员密码不能为空！");
                       return;
                }
                upload(username,password);

                break;
            case R.id.checkbox:
                if (v.getTag().toString().equals("true")) {
                    v.setTag("false");
                    checkbox.setImageResource(R.drawable.login_weixuan);
                } else {
                    v.setTag("true");
                    checkbox.setImageResource(R.drawable.login_beixuan);
                }
                break;
            case R.id.tv_regist:
                mListener.onFragmentInteraction();
                break;
        }
    }

    private void upload(String username,String password){

        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                .login(username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                       @Override
                       public void call(JSONObject jsonObject) {
                           Log.i("sss",jsonObject.toString());
                           if (jsonObject.optBoolean("result")) {
                               JSONObject obj = jsonObject.optJSONObject("admin");
                               User user = new User();
                               user.setId(obj.optString("adminId"));
                               user.setLogin(obj.optString("account"));
                               user.setName(obj.optString("adminName"));
                               user.setToken(obj.optString("token"));
                               mCache.saveUser(user);
                               String flag = checkbox.getTag().toString();
                               SharedPreferencesUtil.save("checkbox_tag", flag, getActivity());
                               if (flag.equals("true")) {
                                   SharedPreferencesUtil.save("username", ct_username.getText().toString(), getActivity());
                                   SharedPreferencesUtil.save("pwd", ct_secret.getText().toString(), getActivity());
                                   SharedPreferencesUtil.save("tag", "true", getActivity());
                               } else {
                                   SharedPreferencesUtil.removeKey(getActivity(), "username");
                                   SharedPreferencesUtil.removeKey(getActivity(), "pwd");
                                   SharedPreferencesUtil.removeKey(getActivity(), "tag");
                               }
                               startActivity(new Intent(getActivity(),MainFragmentActivity.class));
                               getActivity().finish();
                           } else {
                               showToastLong(jsonObject.optString("errMsg"));
                           }
                               }
                           }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                   Log.i("sss",throwable.toString());

                               }
                           }
                );
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction();
    }
}
