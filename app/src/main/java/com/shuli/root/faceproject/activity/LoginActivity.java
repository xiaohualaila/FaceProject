package com.shuli.root.faceproject.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.fragment.LoginFragment;
import com.shuli.root.faceproject.fragment.RegistFragment;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity implements LoginFragment.OnFragmentInteractionListener,RegistFragment.OnRegistFragmentInteractionListener {
    private Fragment mCurrentFrag;
    private FragmentManager fm;
    private Fragment loginFragment;
    private Fragment registFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        fm = getSupportFragmentManager();
        loginFragment = new LoginFragment();
        registFragment = new RegistFragment();

        switchContent(loginFragment);
    }



    /**
     * 动态添加fragment，不会重复创建fragment
     *
     * @param to 将要加载的fragment
     */
    public void switchContent(Fragment to) {
        if (mCurrentFrag != to) {
            if (!to.isAdded()) {// 如果to fragment没有被add则增加一个fragment
                if (mCurrentFrag != null) {
                    fm.beginTransaction().hide(mCurrentFrag).commit();
                }
                fm.beginTransaction()
                        .add(R.id.fl_content, to)
                        .commit();
            } else {
                fm.beginTransaction().hide(mCurrentFrag).show(to).commit(); // 隐藏当前的fragment，显示下一个
            }
            mCurrentFrag = to;
        }
    }

    @Override
    public void onFragmentInteraction() {
        switchContent(registFragment);
    }

    @Override
    public void onRegistFragmentInteraction() {
        switchContent(loginFragment);
    }
}
