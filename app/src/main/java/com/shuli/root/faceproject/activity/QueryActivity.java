package com.shuli.root.faceproject.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;
import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.PeopleDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.adapter.FaceTokenAdapter;
import com.shuli.root.faceproject.base.BaseAppCompatActivity;
import com.shuli.root.faceproject.bean.People;
import com.shuli.root.faceproject.fragment.UpdatePeopleDialogFragment;
import com.shuli.root.faceproject.utils.FaceApi;
import java.util.ArrayList;
import butterknife.BindView;
import butterknife.OnClick;
import megvii.facepass.FacePassException;
import megvii.facepass.FacePassHandler;
import megvii.facepass.types.FacePassConfig;
import megvii.facepass.types.FacePassImageRotation;
import megvii.facepass.types.FacePassModel;
import megvii.facepass.types.FacePassPose;

public class QueryActivity extends BaseAppCompatActivity {
    @BindView(R.id.lv_group_info)
    ListView lv_info;
    private FaceTokenAdapter faceTokenAdapter;

    private static final String DEBUG_TAG = "FacePassDemo";

    /* 人脸识别Group */
    private static final String group_name = "face-pass-test-x";

    /* SDK 实例对象 */
    FacePassHandler mFacePassHandler;

    private boolean isLocalGroupExist = false;

    private int cameraRotation;

    FacePassModel trackModel;
    FacePassModel poseModel;
    FacePassModel blurModel;
    FacePassModel livenessModel;
    FacePassModel searchModel;
    FacePassModel detectModel;

    @Override
    protected void init() {

        /* 初始化界面 */
        initView();

        initFacePassSDK();

        initFaceHandler();

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_query;
    }


    private void initView() {
        int windowRotation = ((WindowManager) (getApplicationContext().getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getRotation() * 90;
        if (windowRotation == 0) {
            cameraRotation = FacePassImageRotation.DEG90;
        } else if (windowRotation == 90) {
            cameraRotation = FacePassImageRotation.DEG0;
        } else if (windowRotation == 270) {
            cameraRotation = FacePassImageRotation.DEG180;
        } else {
            cameraRotation = FacePassImageRotation.DEG270;
        }
        initFacePassSDK();

        initFaceHandler();

        faceTokenAdapter = new FaceTokenAdapter();
        ArrayList<People> list =  QueryData();
        if(list != null &&list.size()>0){
            faceTokenAdapter.setData(list);
            lv_info.setAdapter(faceTokenAdapter);
        }
        unbindButton();
    }

    private void unbindButton() {
        faceTokenAdapter.setOnItemButtonClickListener(new FaceTokenAdapter.ItemButtonClickListener() {
            @Override
            public void onItemDeleteButtonClickListener(int position) {
                final String token = faceTokenAdapter.getData().get(position).getFace_token();
                final People people = GreenDaoManager.getInstance().getSession().getPeopleDao().queryBuilder()
                        .where(PeopleDao.Properties.Face_token.eq(token)).build().unique();
                final UpdatePeopleDialogFragment dialog =  UpdatePeopleDialogFragment.getInstance(people.getName(),people.getGonghao());
                dialog.show(getSupportFragmentManager(), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name =dialog.et_name.getText().toString();
                        String gonghao =dialog.et_gonghao.getText().toString();
                        if(TextUtils.isEmpty(name)&&TextUtils.isEmpty(gonghao)){
                            dialog.tv_title.setText("姓名工号不能为空！");
                        }else {
                            people.setName(name);
                            people.setGonghao(gonghao);
                            GreenDaoManager.getInstance().getSession().getPeopleDao().save(people);
                            ArrayList<People> list = QueryData();
                            if(list != null &&list.size()>0) {
                                faceTokenAdapter.setData(list);
                                lv_info.setAdapter(faceTokenAdapter);
                            }
                            Toast.makeText(QueryActivity.this,"更新成功！",Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    }
                });
            }

            @Override
            public void onItemUnbindButtonClickListener(int position) {
                final String token = faceTokenAdapter.getData().get(position).getFace_token();

                AlertDialog.Builder builder = new AlertDialog.Builder(QueryActivity.this);
                builder.setTitle("解绑");
                builder.setMessage("确定是否要解绑?");
                //点击对话框以外的区域是否让对话框消失
                builder.setCancelable(true);
                //设置正面按钮
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<People> list = unbindData(token);
                        if(list != null &&list.size()>0) {

                            faceTokenAdapter.setData(list);
                            lv_info.setAdapter(faceTokenAdapter);
                        }else {
                            Toast.makeText(QueryActivity.this,"解绑失败！",Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                });
                //设置反面按钮
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                Dialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    @OnClick(R.id.tv_back)
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_back:
                finish();
                startActivity(new Intent(this, FaceLocalActivity.class));
                break;
        }
    }


    private void initFacePassSDK() {
        FacePassHandler.getAuth(FaceApi.authIP, FaceApi.apiKey, FaceApi.apiSecret);
        FacePassHandler.initSDK(getApplicationContext());
    }

    private void initFaceHandler() {

        new Thread() {
            @Override
            public void run() {
                while (true && !isFinishing()) {
                    if (FacePassHandler.isAvailable()) {
                        Log.d(DEBUG_TAG, "start to build FacePassHandler");
                         /* FacePass SDK 所需模型， 模型在assets目录下 */
                        trackModel = FacePassModel.initModel(getApplicationContext().getAssets(), "tracker.DT1.4.1.dingding.20180315.megface2.9.bin");
                        poseModel = FacePassModel.initModel(getApplicationContext().getAssets(), "pose.alfa.tiny.170515.bin");
                        blurModel = FacePassModel.initModel(getApplicationContext().getAssets(), "blurness.v5.l2rsmall.bin");
                        livenessModel = FacePassModel.initModel(getApplicationContext().getAssets(), "panorama.facepass.offline.180312.bin");
                        searchModel = FacePassModel.initModel(getApplicationContext().getAssets(), "feat.small.facepass.v2.9.bin");
                        detectModel = FacePassModel.initModel(getApplicationContext().getAssets(), "detector.mobile.v5.fast.bin");
                        /* SDK 配置 */
                        float searchThreshold = 75f;
                        float livenessThreshold = 70f;
                        boolean livenessEnabled = true;
                        int faceMinThreshold = 150;
                        FacePassPose poseThreshold = new FacePassPose(30f, 30f, 30f);
                        float blurThreshold = 0.2f;
                        float lowBrightnessThreshold = 70f;
                        float highBrightnessThreshold = 210f;
                        float brightnessSTDThreshold = 60f;
                        int retryCount = 2;
                        int rotation = cameraRotation;
                        String fileRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                        FacePassConfig config;
                        try {

                            /* 填入所需要的配置 */
                            config = new FacePassConfig(searchThreshold, livenessThreshold, livenessEnabled,
                                    faceMinThreshold, poseThreshold, blurThreshold,
                                    lowBrightnessThreshold, highBrightnessThreshold, brightnessSTDThreshold,
                                    retryCount, rotation, fileRootPath,
                                    trackModel, poseModel, blurModel, livenessModel, searchModel, detectModel);
                            /* 创建SDK实例 */
                            mFacePassHandler = new FacePassHandler(config);
                            checkGroup();
                        } catch (FacePassException e) {
                            e.printStackTrace();
                            Log.d(DEBUG_TAG, "请检查网络或者人脸检测受限！");
                            return;
                        }
                        return;
                    }
                    try {
                        /* 如果SDK初始化未完成则需等待 */
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        checkGroup();
        super.onResume();
    }


    private void checkGroup() {
        if (mFacePassHandler == null) {
            return;
        }
        String[] localGroups = mFacePassHandler.getLocalGroups();
        isLocalGroupExist = false;
        if (localGroups == null || localGroups.length == 0) {
            creatLocalGroup();
        }
        for (String group : localGroups) {
            if (group_name.equals(group)) {
                isLocalGroupExist = true;
            }else {
                creatLocalGroup();
            }
        }
    }

    private void creatLocalGroup() {
        boolean isSuccess = false;
        try {
            isSuccess = mFacePassHandler.createLocalGroup(group_name);
        } catch (FacePassException e) {
            e.printStackTrace();
        }
        if (isSuccess) {
            isLocalGroupExist = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        if (mFacePassHandler != null) {
            mFacePassHandler.release();
        }
        super.onDestroy();
    }

    public ArrayList<People> QueryData(){
        ArrayList<People> faceTokenList = new ArrayList<>();
        if (mFacePassHandler == null) {
            toast("请检查网络或者人脸检测受限! ");
            return faceTokenList;
        }

        try {
            byte[][] faceTokens = mFacePassHandler.getLocalGroupInfo(group_name);
            String face_token;
            if (faceTokens != null && faceTokens.length > 0) {
                for (int j = 0; j < faceTokens.length; j++) {
                    if (faceTokens[j].length > 0) {
                        face_token = new String(faceTokens[j]);
                        People people = GreenDaoManager.getInstance().getSession().getPeopleDao()
                                .queryBuilder().where(PeopleDao.Properties.Face_token.eq(face_token)).unique();
                        if(people != null){
                            faceTokenList.add(people);
                        } else {
                            //解绑无用的人脸标识
                            mFacePassHandler.unBindGroup(group_name, face_token.getBytes());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("get local group info error!");
        }finally {
            return faceTokenList;
        }
    }

    public ArrayList<People> unbindData(String token) {
        ArrayList<People> faceTokenList = new ArrayList<>();
        /**
         * 解绑
         */
        if (mFacePassHandler == null) {
            toast("请检查网络或者人脸检测受限! ");
            return faceTokenList;
        }

        try {
            byte[] faceToken = token.getBytes();
            boolean b = mFacePassHandler.unBindGroup(group_name, faceToken);
            String result = b ? "success " : "failed";
            toast("解绑 " + result);
            if (b) {
                byte[][] faceTokens = mFacePassHandler.getLocalGroupInfo(group_name);
                String string;
                if (faceTokens != null && faceTokens.length > 0) {
                    for (int j = 0; j < faceTokens.length; j++) {
                        if (faceTokens[j].length > 0) {
                            string = new String(faceTokens[j]);
                            People people = GreenDaoManager.getInstance().getSession().getPeopleDao()
                                    .queryBuilder().where(PeopleDao.Properties.Face_token.eq(string)).unique();
                            faceTokenList.add(people);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("绑定失败!");
        }finally {
            return faceTokenList;
        }
    }

}







