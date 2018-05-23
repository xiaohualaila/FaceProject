package com.shuli.root.faceproject.fragment;


import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.AccountDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.activity.FaceLocalActivity;
import com.shuli.root.faceproject.base.BaseFragment;
import com.shuli.root.faceproject.bean.Account;
import com.shuli.root.faceproject.utils.ClearEditTextWhite;
import com.shuli.root.faceproject.utils.SharedPreferencesUtil;
import butterknife.BindView;
import butterknife.OnClick;

public class LoginFragment extends BaseFragment {
    @BindView(R.id.ct_username)
    ClearEditTextWhite ct_username;
    @BindView(R.id.ct_secret)
    ClearEditTextWhite ct_secret;
    @BindView(R.id.checkbox)
    ImageView checkbox;
    private OnFragmentInteractionListener mListener;
    @Override
    protected int getLayoutId() {
        return R.layout.layout_login;
    }

    @Override
    protected void init() {
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
                Account account = GreenDaoManager.getInstance().getSession().getAccountDao().queryBuilder()
                        .where(AccountDao.Properties.Account_name.eq(username)).build().unique();
                if(account == null){
                    showToastLong("用户名不存在！");
                    return;
                }else {
                    if(!account.getAccount_secret().equals(password)){
                        showToastLong("密码错误！");
                        return;
                    }else {
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
                        startActivity(new Intent(getActivity(),FaceLocalActivity.class));
                        getActivity().finish();
                    }
                }
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
