package com.shuli.root.faceproject.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.adapter.FaceTokenAdapter;
import com.shuli.root.faceproject.utils.FaceApi;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import megvii.facepass.FacePassException;
import megvii.facepass.FacePassHandler;
import megvii.facepass.types.FacePassConfig;
import megvii.facepass.types.FacePassImageRotation;
import megvii.facepass.types.FacePassModel;
import megvii.facepass.types.FacePassPose;

public class QueryActivity extends Activity {
    @BindView(R.id.lv_group_info)
    ListView lv_info;

    /* 人脸识别Group */
    private static final String group_name = "face-pass-test-x";

    /* SDK 实例对象 */
    FacePassHandler mFacePassHandler;


    FacePassModel trackModel;
    FacePassModel poseModel;
    FacePassModel blurModel;
    FacePassModel livenessModel;
    FacePassModel searchModel;
    FacePassModel detectModel;
    private int cameraRotation;
    private boolean isLocalGroupExist = false;
    private FaceTokenAdapter faceTokenAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_query);
        ButterKnife.bind(this);
        faceTokenAdapter = new FaceTokenAdapter();
        initView();

        initFacePassSDK();

        initFaceHandler();

        getGroupInfo();

        initData();
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
                        Log.d("sss", "start to build FacePassHandler");
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
                            Log.d("sss", "FacePassHandler is null");
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

      private void getGroupInfo() {


          faceTokenAdapter.setOnItemButtonClickListener(new FaceTokenAdapter.ItemButtonClickListener() {
              @Override
              public void onItemDeleteButtonClickListener(int position) {
                  if (mFacePassHandler == null) {
                      toast("FacePassHandle is null ! ");
                      return;
                  }

                  if (mFacePassHandler == null) {
                      toast("FacePassHandle is null ! ");
                      return;
                  }

                  try {
                      byte[] faceToken = faceTokenAdapter.getData().get(position).getFace_token().getBytes();
                      boolean b = mFacePassHandler.deleteFace(faceToken);
                      String result = b ? "success " : "failed";
                      toast("delete face " + result);
                      if (b) {
                          byte[][] faceTokens = mFacePassHandler.getLocalGroupInfo(group_name);
                          List<String> faceTokenList = new ArrayList<>();
                          if (faceTokens != null && faceTokens.length > 0) {
                              for (int j = 0; j < faceTokens.length; j++) {
                                  if (faceTokens[j].length > 0) {
                                      faceTokenList.add(new String(faceTokens[j]));
                                  }
                              }

                          }
                          faceTokenAdapter.setData(faceTokenList);
                          lv_info.setAdapter(faceTokenAdapter);
                      }
                  } catch (Exception e) {
                      e.printStackTrace();
                      toast(e.getMessage());
                  }
              }

              @Override
              public void onItemUnbindButtonClickListener(int position) {
                  /**
                   * 解绑
                   */
                  if (mFacePassHandler == null) {
                      toast("FacePassHandle is null ! ");
                      return;
                  }

                  try {
                      byte[] faceToken = faceTokenAdapter.getData().get(position).getFace_token().getBytes();
                      boolean b = mFacePassHandler.unBindGroup(group_name, faceToken);
                      String result = b ? "success " : "failed";
                      toast("unbind " + result);
                      if (b) {
                          byte[][] faceTokens = mFacePassHandler.getLocalGroupInfo(group_name);
                          List<String> faceTokenList = new ArrayList<>();
                          if (faceTokens != null && faceTokens.length > 0) {
                              for (int j = 0; j < faceTokens.length; j++) {
                                  if (faceTokens[j].length > 0) {
                                      faceTokenList.add(new String(faceTokens[j]));
                                  }
                              }

                          }
                          faceTokenAdapter.setData(faceTokenList);
                          lv_info.setAdapter(faceTokenAdapter);
                      }
                  } catch (Exception e) {
                      e.printStackTrace();
                      toast("unbind error!");
                  }

              }
          });

    }

    private void initData(){
        ArrayList<String> faceTokenList = new ArrayList<>();
        try {
            Intent intent = getIntent();
            faceTokenList = intent.getStringArrayListExtra("faceTokenList");
            faceTokenAdapter.setData(faceTokenList);
            lv_info.setAdapter(faceTokenAdapter);
        } catch (Exception e) {
            e.printStackTrace();
            toast("get local group info error!");
        }

    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    @OnClick(R.id.tv_back)
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_back:
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFacePassHandler != null) {
            mFacePassHandler.release();
        }
    }
}
