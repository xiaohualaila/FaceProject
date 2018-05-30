package com.shuli.root.faceproject.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.PeopleDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.base.BaseAppCompatActivity;
import com.shuli.root.faceproject.bean.People;
import com.shuli.root.faceproject.fragment.AddFragment;
import com.shuli.root.faceproject.fragment.QueryFragment;
import com.shuli.root.faceproject.retrofit.Api;
import com.shuli.root.faceproject.retrofit.ConnectUrl;
import com.shuli.root.faceproject.utils.FaceApi;
import com.shuli.root.faceproject.utils.MyUtil;
import com.shuli.root.faceproject.utils.SharedPreferencesUtil;

import org.greenrobot.greendao.query.QueryBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import butterknife.BindView;
import megvii.facepass.FacePassException;
import megvii.facepass.FacePassHandler;
import megvii.facepass.types.FacePassAddFaceResult;
import megvii.facepass.types.FacePassConfig;
import megvii.facepass.types.FacePassImageRotation;
import megvii.facepass.types.FacePassModel;
import megvii.facepass.types.FacePassPose;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainFragmentActivity extends BaseAppCompatActivity implements AddFragment.OnFragmentInteractionListener,QueryFragment.OnQueryFragmentInteractionListener {
    @BindView(R.id.fl_content)
    FrameLayout fl_content;

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

    private Fragment mCurrentFrag;
    private FragmentManager fm;
    private Fragment cameraFragment;
    private Fragment queryFragment;

    private boolean isAuto = true;
    private Thread threadNet;
    @Override
    protected void init() {

        /* 初始化界面 */
        initView();

        initFacePassSDK();

        initFaceHandler();

        fm = getSupportFragmentManager();
        cameraFragment = new AddFragment();
        queryFragment = new QueryFragment();
        switchContent(cameraFragment);//切换页面
        threadNet = new Thread(taskNet);
        threadNet.start();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_fragment;
    }


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
        isAuto = false;
        super.onDestroy();
    }

    /**
     * 添加人脸
     * @param filePath
     * @return
     */
    @Override
    public String addFace(String filePath) {
        String result = "";
        if (mFacePassHandler == null) {
            toast("请检查网络或者人脸检测受限!");
            return result;
        }

        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            toast("图片不存在 !");
            return result;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        try {
            FacePassAddFaceResult facePassAddFaceResult = mFacePassHandler.addFace(bitmap);
            if (result != null) {
                if (facePassAddFaceResult.result == 0) {
                    toast("添加人脸成功！");
                    result = new String(facePassAddFaceResult.faceToken);//获取到人脸特征
                } else if (facePassAddFaceResult.result == 1) {
                    toast("没有人脸，请重拍！");
                } else {
                    toast("图片质量问题，请重拍！");
                }
            }
        } catch (FacePassException e) {
            e.printStackTrace();
            toast(e.getMessage());
        }
        return result;
    }

    /**
     * 删除
     */
    @Override
    public boolean deleteFace(String token) {
        boolean b = false;
        if (mFacePassHandler == null) {
            toast("请检查网络或者人脸检测受限!");
            return b;
        }
        try {
            byte[] faceToken = token.getBytes();
            b = mFacePassHandler.deleteFace(faceToken);
        } catch (FacePassException e) {
            e.printStackTrace();
            toast(e.getMessage());
        }

         String result = b ? "成功 " : "失败";
         toast("删除人脸 " + result +"!");

        return b;
    }

    /**
     * 绑定 人脸信息，并向数据库中保存人员信息
     */
    @Override
    public boolean bindGroupFaceToken(String token, String name,String gonghao) {
        boolean b = false;
        if (mFacePassHandler == null) {
            toast("请检查网络或者人脸检测受限! ");
            return b;
        }

        byte[] faceToken = token.getBytes();
        if (faceToken == null || faceToken.length == 0 || TextUtils.isEmpty(group_name)) {
            toast("没有人脸特征值！");
            return b;
        }

        if (TextUtils.isEmpty(name)){
            toast("姓名不能为空！");
            return b;
        }
        if (TextUtils.isEmpty(gonghao)){
            toast("工号不能为空！");
            return b;
        }
        People people = GreenDaoManager.getInstance().getSession().getPeopleDao().queryBuilder().where(PeopleDao.Properties.Face_token.eq(token)).unique();
        if(people != null){
            return b;
        }
        try {
             b = mFacePassHandler.bindGroup(group_name, faceToken);
            if(b){
                PeopleDao peopleDao = GreenDaoManager.getInstance().getSession().getPeopleDao();
                peopleDao.insert(new People(name,gonghao,token));
            }
            String result = b ? "成功 " : "失败";
            toast("绑定  " + result);

        } catch (Exception e) {
            e.printStackTrace();
            toast(e.getMessage());
            b = false;
        }
        return b;
    }

    /**
     * 从人脸底库中获取所有的人脸标识并查询与人脸标识对应的人员信息
     * @return 返回人员信息L集合
     */
    @Override
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
                        if(people != null){//将不等于null的对象添加
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

    /**
     * 解绑数据 通过传入的token,解绑成功并删除相应数据库的人员信息，
     * @param token
     * @return 如果解绑成功，重新获取人员信息列表
     */
    @Override
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
            String result = b ? "成功 " : "失败";
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
            toast("解绑失败!");
        }finally {
            return faceTokenList;
        }
    }

    /**
     * 解绑数据 通过传入的token,解绑成功并删除相应数据库的人员信息，
     * @param token
     */
    public void unbindDeleteData(String token) {
        /**
         * 解绑
         */
        if (mFacePassHandler == null) {
            toast("请检查网络或者人脸检测受限! ");
            return;
        }
        try {
            byte[] faceToken = token.getBytes();
            boolean b = mFacePassHandler.unBindGroup(group_name, faceToken);
            String result = b ? "成功 " : "失败";
            toast("解绑 " + result);
            if (b) {
                byte[][] faceTokens = mFacePassHandler.getLocalGroupInfo(group_name);
                String string;
                if (faceTokens != null && faceTokens.length > 0) {
                    for (int j = 0; j < faceTokens.length; j++) {
                        if (faceTokens[j].length > 0) {
                            string = new String(faceTokens[j]);
                            GreenDaoManager.getInstance().getSession().getPeopleDao()
                             .queryBuilder().where(PeopleDao.Properties.Face_token.eq(string)).unique();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("解绑失败!");
        }
    }

    Runnable taskNet = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if(isAuto){
                    boolean isNetAble = MyUtil.isNetworkAvailable(MainFragmentActivity.this);
                    if (isNetAble) {
                        requestInfo();
                    }
                    Log.i("sss", ">>>>>>>>>sleep>>>>>>>>>>>>>");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private void requestInfo(){
        Log.i("sss", ">>>>>>>>>>>>>>>>>>>>>>");
        int count = SharedPreferencesUtil.getIntByKey("count",this);
        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                .getFaceToken(count)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                               @Override
                               public void call(JSONObject jsonObject) {
                                   Log.i("sss", jsonObject.toString());
                                   if (jsonObject != null) {
                                       if (jsonObject.optBoolean("result")) {
                                           JSONArray array = jsonObject.optJSONArray("userList");
                                           int num = array.length();
                                           if(num > 0){
                                               JSONObject obj;
                                               for (int i = 0;i < num;i++) {
                                                   obj = array.optJSONObject(i);
                                                   bindGroupFaceToken(obj.optString("faceToken"),obj.optString("userName"),obj.optString("workNum"));
                                               }
                                               SharedPreferencesUtil.save("count",jsonObject.optInt("maxUserId"),MainFragmentActivity.this);
                                           }
                                           JSONArray deleteArray = jsonObject.optJSONArray("deletedUserList");
                                           int num2 = deleteArray.length();
                                           if(num2 > 0){
                                               for (int i = 0;i < num2; i++) {
                                                   String delete_str = deleteArray.optString(i);
                                                   unbindDeleteData(delete_str);
                                               }
                                           }
                                       }

                                   }
                               }
                           }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                   Log.i("sss", throwable.toString());
                               }
                           }
                );
    }


    @Override
    public void toQueryActivity() {
        switchContent(queryFragment);
    }

    @Override
    public void toCameraActivity() {
        switchContent(cameraFragment);
    }
}







