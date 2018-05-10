package com.shuli.root.faceproject.fragment;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.AccountDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.activity.FaceLocalActivity;
import com.shuli.root.faceproject.base.BaseFragment;
import com.shuli.root.faceproject.bean.Account;
import com.shuli.root.faceproject.utils.ClearEditTextWhite;
import butterknife.BindView;
import butterknife.OnClick;

public class RegistFragment extends BaseFragment {
    @BindView(R.id.ct_factor_secret)
    ClearEditTextWhite ct_factor_secret;
    @BindView(R.id.ct_username)
    ClearEditTextWhite ct_username;
    @BindView(R.id.ct_secret)
    ClearEditTextWhite ct_secret;
    @BindView(R.id.ct_secret_again)
    ClearEditTextWhite ct_secret_again;
    @BindView(R.id.btn_login)
    Button btn_login;
    public RegistFragment() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.layout_regist;
    }

    @Override
    protected void init() {

    }

    @OnClick({R.id.btn_login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                String factor_secret = ct_factor_secret.getText().toString().trim();
                String username = ct_username.getText().toString().trim();
                String secret = ct_secret.getText().toString().trim();
                String secret_again = ct_secret_again.getText().toString().trim();
                if (TextUtils.isEmpty(factor_secret)) {
                    showToastLong("出厂密码不能为空！");
                    return;
                } else {
                    if (!factor_secret.equals("123")) {
                        showToastLong("出厂密码错误！");
                        return;
                    }
                }
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

                Account account = GreenDaoManager.getInstance().getSession().getAccountDao().queryBuilder()
                        .where(AccountDao.Properties.Account_name.eq(username)).build().unique();
                if (account != null) {
                    showToastLong("账号重复！");
                    return;
                } else {
                    AccountDao accountDao = GreenDaoManager.getInstance().getSession().getAccountDao();
                    accountDao.insert(new Account(username, secret_again));
                    showToastLong("注册成功！");
                }
                startActivity(new Intent(getActivity(), FaceLocalActivity.class));
                getActivity().finish();
                break;
        }
    }
    

}
