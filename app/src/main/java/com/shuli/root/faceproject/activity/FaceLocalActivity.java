package com.shuli.root.faceproject.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Power;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.PeopleDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.bean.People;
import com.shuli.root.faceproject.camera.CameraManager;
import com.shuli.root.faceproject.camera.CameraPreview;
import com.shuli.root.faceproject.camera.CameraPreviewData;
import com.shuli.root.faceproject.face.FaceView;
import com.shuli.root.faceproject.retrofit.Api;
import com.shuli.root.faceproject.retrofit.ConnectUrl;
import com.shuli.root.faceproject.utils.FaceApi;
import com.shuli.root.faceproject.utils.MyUtil;
import com.shuli.root.faceproject.utils.SettingVar;
import com.shuli.root.faceproject.utils.SharedPreferencesUtil;
import com.shuli.root.faceproject.utils.SoundPoolUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import butterknife.ButterKnife;
import megvii.facepass.FacePassException;
import megvii.facepass.FacePassHandler;
import megvii.facepass.types.FacePassConfig;
import megvii.facepass.types.FacePassDetectionResult;
import megvii.facepass.types.FacePassFace;
import megvii.facepass.types.FacePassImage;
import megvii.facepass.types.FacePassImageRotation;
import megvii.facepass.types.FacePassImageType;
import megvii.facepass.types.FacePassModel;
import megvii.facepass.types.FacePassPose;
import megvii.facepass.types.FacePassRecognitionResult;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class FaceLocalActivity extends AppCompatActivity implements CameraManager.CameraListener, View.OnClickListener {

    private enum FacePassSDKMode {
        MODE_ONLINE,
        MODE_OFFLINE
    }

    private static FacePassSDKMode SDK_MODE = FacePassSDKMode.MODE_OFFLINE;

    private static final String DEBUG_TAG = "FacePassDemo";

    private static final int MSG_SHOW_TOAST = 1;

    private static final int DELAY_MILLION_SHOW_TOAST = 2000;

    /* 识别服务器IP */

    private static final String serverIP_offline = "10.104.44.50";//offline

    private static final String serverIP_online = "10.199.1.14";

    private static String serverIP;

    private static String url;

    /* 人脸识别Group */
    private static final String group_name = "face-pass-test-x";

    /* SDK 实例对象 */
    FacePassHandler mFacePassHandler;

    /* 相机实例 */
    private CameraManager manager;


    /* 相机预览界面 */
    private CameraPreview cameraView;

    private boolean isLocalGroupExist = false;

    /* 在预览界面圈出人脸 */
    private FaceView faceView;

    /* 相机是否使用前置摄像头 */
    private static boolean cameraFacingFront = true;

    private int cameraRotation;

    private static final int cameraWidth = 640;
    private static final int cameraHeight = 480;

    private int heightPixels;
    private int widthPixels;

    int screenState = 0;// 0 横 1 竖

    /* 网络请求队列*/
    RequestQueue requestQueue;

    FacePassModel trackModel;
    FacePassModel poseModel;
    FacePassModel blurModel;
    FacePassModel livenessModel;
    FacePassModel searchModel;
    FacePassModel detectModel;

    FrameLayout frameLayout;

    /*Toast 队列*/
    LinkedBlockingQueue<Toast> mToastBlockQueue;

    /*DetectResult queue*/
    ArrayBlockingQueue<byte[]> mDetectResultQueue;

    /*recognize thread*/
    RecognizeThread mRecognizeThread;

    private ImageView mFaceOperationBtn;
    private ImageView quit;
    /*图片缓存*/
    private FaceImageCache mImageCache;
    private ImageView mScanVerticalLineImageView;
    private Handler mAndroidHandler;

    private LinearLayout ll_face_success;
    private TextView tv_name;
    private TextView tv_num;
    private TextView face_success;
    private Handler handler = new Handler();
    private boolean isAuto = true;
    private Thread threadNet;
    private RelativeLayout layout_root;
    private AnimationDrawable frameAnimation1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageCache = new FaceImageCache();
        mToastBlockQueue = new LinkedBlockingQueue<>();
        mDetectResultQueue = new ArrayBlockingQueue<byte[]>(5);
        initAndroidHandler();
        if (FacePassSDKMode.MODE_ONLINE == SDK_MODE) {
            url = "http://" + serverIP_online + ":8080/api/service/recognize/v1";
            serverIP = serverIP_online;
        } else {
            serverIP = serverIP_offline;
        }

        /* 初始化界面 */
        initView();

        initFacePassSDK();

        initFaceHandler();
        /* 初始化网络请求库 */
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        mRecognizeThread = new RecognizeThread();
        mRecognizeThread.start();

        threadNet = new Thread(taskNet);
        threadNet.start();
    }

    private void initAndroidHandler() {

        mAndroidHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_SHOW_TOAST:
                        if (mToastBlockQueue.size() > 0) {
                            Toast toast = mToastBlockQueue.poll();
                            if (toast != null) {
                                toast.show();
                            }
                        }
                        if (mToastBlockQueue.size() > 0) {
                            removeMessages(MSG_SHOW_TOAST);
                            sendEmptyMessageDelayed(MSG_SHOW_TOAST, DELAY_MILLION_SHOW_TOAST);
                        }
                        break;
                }
            }
        };

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
                            Log.d(DEBUG_TAG, "FacePassHandler is null");
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
        initToast();
        /* 打开相机 */
        manager.open(getWindowManager(), cameraFacingFront, cameraWidth, cameraHeight);

        adaptFrameLayout();
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
            } else {
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


    /* 相机回调函数 */
    @Override
    public void onPictureTaken(CameraPreviewData cameraPreviewData) {

        /* 如果SDK实例还未创建，则跳过 */
        if (mFacePassHandler == null) {
            return;
        }
        /* 将相机预览帧转成SDK算法所需帧的格式 FacePassImage */

        FacePassImage image;
        try {
            image = new FacePassImage(cameraPreviewData.nv21Data, cameraPreviewData.width, cameraPreviewData.height, cameraRotation, FacePassImageType.NV21);
        } catch (FacePassException e) {
            e.printStackTrace();
            return;
        }

        /* 将每一帧FacePassImage 送入SDK算法， 并得到返回结果 */
        FacePassDetectionResult detectionResult = null;
        try {
            detectionResult = mFacePassHandler.feedFrame(image);
        } catch (FacePassException e) {
            e.printStackTrace();
        }

        if (detectionResult == null || detectionResult.faceList.length == 0) {
            /* 当前帧没有检出人脸 */
            faceView.clear();
            faceView.invalidate();
        } else {
            /* 将识别到的人脸在预览界面中圈出，并在上方显示人脸位置及角度信息 */
            showFacePassFace(detectionResult.faceList);
        }

        /*离线模式，将识别到人脸的，message不为空的result添加到处理队列中*/
        if (detectionResult != null && detectionResult.message.length != 0) {
            Log.d(DEBUG_TAG, "mDetectResultQueue.offer");
            mDetectResultQueue.offer(detectionResult.message);
            Log.d(DEBUG_TAG, "1 mDetectResultQueue.size = " + mDetectResultQueue.size());
        }

    }

    private class RecognizeThread extends Thread {

        boolean isInterrupt;

        @Override
        public void run() {
            while (!isInterrupt) {
                try {
                    Log.d(DEBUG_TAG, "2 mDetectResultQueue.size = " + mDetectResultQueue.size());
                    byte[] detectionResult = mDetectResultQueue.take();

                    Log.d(DEBUG_TAG, "mDetectResultQueue.isLocalGroupExist");
                    if (isLocalGroupExist) {
                        Log.d(DEBUG_TAG, "mDetectResultQueue.recognize");
                        FacePassRecognitionResult[] recognizeResult = mFacePassHandler.recognize(group_name, detectionResult);
                        if (recognizeResult != null && recognizeResult.length > 0) {
                            for (FacePassRecognitionResult result : recognizeResult) {
                                String faceToken = new String(result.faceToken);
                                showRecognizeResult(result.trackId, result.detail.searchScore, result.detail.livenessScore, !TextUtils.isEmpty(faceToken), faceToken);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (FacePassException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void interrupt() {
            isInterrupt = true;
            super.interrupt();
        }
    }

    private void showRecognizeResult(final long trackId, final float searchScore, final float livenessScore, final boolean isRecognizeOK, final String token) {
        mAndroidHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i("sss", "ID = " + trackId + (isRecognizeOK ? "识别成功" : "识别失败") + "\n");
                Log.i("sss", "识别分 = " + searchScore + "\n");
                Log.i("sss", "活体分 = " + livenessScore + "\n");

                if (isRecognizeOK) {
                    People people ;
                    try{
                         people = GreenDaoManager.getInstance().getSession().getPeopleDao().queryBuilder()
                                .where(PeopleDao.Properties.Face_token.eq(token)).build().unique();
                    }catch (Exception e){
                        e.printStackTrace();
                        people = null;
                    }
                    face_success.setVisibility(View.VISIBLE);
                    ll_face_success.setVisibility(View.VISIBLE);
                    face_success.setText("验证成功！");
                    setViewAnimal();
                    SoundPoolUtil.play(1);
                    // TODO: 2018/5/9 开门
                    Power.set_zysj_gpio_value(1,0);
                    if(people != null){
                        tv_name.setText(people.getName());
                        tv_num.setText(people.getGonghao());
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            face_success.setVisibility(View.INVISIBLE);
                            ll_face_success.setVisibility(View.GONE);

                            // TODO: 2018/5/9 关门
                            Power.set_zysj_gpio_value(1,1);
                        }
                    }, 2000);

                } else {
                    face_success.setText("验证失败！");
                    face_success.setVisibility(View.VISIBLE);

                    // SoundPoolUtil.play(2);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            face_success.setVisibility(View.INVISIBLE);
                        }
                    }, 2000);
                }
            }
        });
    }

    private void adaptFrameLayout() {
        SettingVar.isButtonInvisible = false;
        SettingVar.iscameraNeedConfig = false;
    }

    private void initToast() {
        SettingVar.isButtonInvisible = false;
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
        Log.i(DEBUG_TAG, "cameraRation: " + cameraRotation);
        cameraFacingFront = true;

        SettingVar.isSettingAvailable = true;
        SettingVar.isCross = false;
        SettingVar.faceRotation = 0;
        SettingVar.cameraPreviewRotation = 0;
        SettingVar.cameraFacingFront = true;

        if (SettingVar.isSettingAvailable) {
            cameraRotation = SettingVar.faceRotation;
            cameraFacingFront = SettingVar.cameraFacingFront;
        }


        Log.i("orientation", String.valueOf(windowRotation));
        final int mCurrentOrientation = getResources().getConfiguration().orientation;

        if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            screenState = 1;
        } else if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            screenState = 0;
        }
        setContentView(R.layout.activity_local);
        ButterKnife.bind(this);
        ll_face_success = findViewById(R.id.ll_face_success);
        face_success = findViewById(R.id.tv_result);
        tv_name = findViewById(R.id.tv_name);
        tv_num = findViewById(R.id.tv_num);
        mScanVerticalLineImageView = findViewById(R.id.scanVerticalLineImageView);
        mFaceOperationBtn = findViewById(R.id.btn_face_operation);
        mFaceOperationBtn.setOnClickListener(this);
        quit = findViewById(R.id.quit);
        quit.setOnClickListener(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        heightPixels = displayMetrics.heightPixels;
        widthPixels = displayMetrics.widthPixels;
        SettingVar.mHeight = heightPixels;
        SettingVar.mWidth = widthPixels;

        faceView = this.findViewById(R.id.fcview);
        SettingVar.cameraSettingOk = false;

        manager = new CameraManager();
        cameraView = findViewById(R.id.preview);
        manager.setPreviewDisplay(cameraView);
        frameLayout = findViewById(R.id.frame);
        /* 注册相机回调函数 */
        manager.setListener(this);

        //背景动画
        layout_root = findViewById(R.id.activity_main);
        layout_root.setBackgroundResource(R.drawable.bg_animation);
        frameAnimation1 = (AnimationDrawable) layout_root.getBackground();
        frameAnimation1.start();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        int[] location = new int[2];

        // getLocationInWindow方法要在onWindowFocusChanged方法里面调用
        // 个人理解是onCreate时，View尚未被绘制，因此无法获得具体的坐标点
        cameraView.getLocationInWindow(location);

        // 模拟的mPreviewView的左右上下坐标坐标
        int bottom = cameraView.getBottom();
        // 从上到下的平移动画
        Animation verticalAnimation = new TranslateAnimation(0, 0, -bottom , bottom);
        verticalAnimation.setDuration(3000); // 动画持续时间
        verticalAnimation.setRepeatCount(Animation.INFINITE); // 无限循环

        // 播放动画
        mScanVerticalLineImageView.setAnimation(verticalAnimation);
        verticalAnimation.startNow();
    }


    @Override
    protected void onStop() {
        SettingVar.isButtonInvisible = false;
        mToastBlockQueue.clear();
        mDetectResultQueue.clear();
        if (manager != null) {
            manager.release();
        }
        super.onStop();
    }

    @Override
    protected void onRestart() {
        faceView.clear();
        faceView.invalidate();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        mRecognizeThread.isInterrupt = true;

        mRecognizeThread.interrupt();
        if (requestQueue != null) {
            requestQueue.cancelAll("upload_detect_result_tag");
            requestQueue.cancelAll("handle_sync_request_tag");
            requestQueue.cancelAll("load_image_request_tag");
            requestQueue.stop();
        }

        if (manager != null) {
            manager.release();
        }
        if (mToastBlockQueue != null) {
            mToastBlockQueue.clear();
        }
        if (mAndroidHandler != null) {
            mAndroidHandler.removeCallbacksAndMessages(null);
        }

        if (mFacePassHandler != null) {
            mFacePassHandler.release();
        }
        super.onDestroy();
    }

    private void showFacePassFace(FacePassFace[] detectResult) {
        faceView.clear();
        for (FacePassFace face : detectResult) {
            boolean mirror = cameraFacingFront; /* 前摄像头时mirror为true */
            Matrix mat = new Matrix();
            int w = cameraView.getMeasuredWidth();
            int h = cameraView.getMeasuredHeight();

            int cameraHeight = manager.getCameraheight();
            int cameraWidth = manager.getCameraWidth();

            float left = 0;
            float top = 0;
            float right = 0;
            float bottom = 0;
            switch (cameraRotation) {
                case 0:
                    left = face.rect.left;
                    top = face.rect.top;
                    right = face.rect.right;
                    bottom = face.rect.bottom;
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraWidth : 0f, 0f);
                    mat.postScale((float) w / (float) cameraWidth, (float) h / (float) cameraHeight);
                    break;
                case 90:
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                    mat.postScale((float) w / (float) cameraHeight, (float) h / (float) cameraWidth);
                    left = face.rect.top;
                    top = cameraWidth - face.rect.right;
                    right = face.rect.bottom;
                    bottom = cameraWidth - face.rect.left;
                    break;
                case 180:
                    mat.setScale(1, mirror ? -1 : 1);
                    mat.postTranslate(0f, mirror ? (float) cameraHeight : 0f);
                    mat.postScale((float) w / (float) cameraWidth, (float) h / (float) cameraHeight);
                    left = face.rect.right;
                    top = face.rect.bottom;
                    right = face.rect.left;
                    bottom = face.rect.top;
                    break;
                case 270:
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                    mat.postScale((float) w / (float) cameraHeight, (float) h / (float) cameraWidth);
                    left = cameraHeight - face.rect.bottom;
                    top = face.rect.left;
                    right = cameraHeight - face.rect.top;
                    bottom = face.rect.right;
            }

            RectF drect = new RectF();
            RectF srect = new RectF(left, top, right, bottom);

            mat.mapRect(drect, srect);
            faceView.addRect(drect);
        }
        faceView.invalidate();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_face_operation:
                // 进入查询列表页面
                toOtherActivity();
                break;
            case R.id.quit:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("是否要退出?");
                //点击对话框以外的区域是否让对话框消失
                builder.setCancelable(true);
                //设置正面按钮
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
                //设置反面按钮
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                break;
        }
    }

    private void toOtherActivity() {
        if (manager != null) {
            manager.release();
        }
        finish();
        startActivity(new Intent(this, MainFragmentActivity.class));
    }


    /**
     * 根据facetoken下载图片缓存
     */
    private static class FaceImageCache implements ImageLoader.ImageCache {

        private static final int CACHE_SIZE = 6 * 1024 * 1024;

        LruCache<String, Bitmap> mCache;

        public FaceImageCache() {
            mCache = new LruCache<String, Bitmap>(CACHE_SIZE) {

                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getRowBytes() * value.getHeight();
                }
            };
        }

        @Override
        public Bitmap getBitmap(String url) {
            return mCache.get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            mCache.put(url, bitmap);
        }
    }

    /**
     * 绑定
     */
    public void bindGroupFaceToken(String token, String name,String gonghao) {
        if (mFacePassHandler == null) {
            return;
        }

        byte[] faceToken = token.getBytes();
        if (faceToken == null || faceToken.length == 0 || TextUtils.isEmpty(group_name)) {
            return;
        }

        if (TextUtils.isEmpty(name)){
            return;
        }
        if (TextUtils.isEmpty(gonghao)){
            return;
        }

        People people = GreenDaoManager.getInstance().getSession().getPeopleDao().queryBuilder().where(PeopleDao.Properties.Face_token.eq(faceToken)).unique();
        if(people != null){
            return;
        }
        try {
          boolean b  = mFacePassHandler.bindGroup(group_name, faceToken);
            if(b){
                PeopleDao peopleDao = GreenDaoManager.getInstance().getSession().getPeopleDao();
                peopleDao.insert(new People(name,gonghao,token));
            }

        } catch (Exception e) {
            e.printStackTrace();
            toast(e.getMessage());
        }
        return ;
    }

    Runnable taskNet = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if(isAuto){
                    boolean isNetAble = MyUtil.isNetworkAvailable(FaceLocalActivity.this);
                    if (isNetAble) {
                        requestInfo();
                    }
                    Log.i("sss", ">>>>>>>>>sleep>>>>>>>>>>>>>");
                    try {
                        Thread.sleep(20000);
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
                               JSONObject obj;
                               for (int i = 0;i < num;i++) {
                                   obj = array.optJSONObject(i);
                                   bindGroupFaceToken(obj.optString("faceToken"),obj.optString("userName"),obj.optString("workNum"));
                               }
                               SharedPreferencesUtil.save("count",jsonObject.optInt("maxUserId"),FaceLocalActivity.this);
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

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void setViewAnimal(){
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,0.8f,
                Animation.RELATIVE_TO_SELF,0f,
                Animation.RELATIVE_TO_SELF,0f,
                Animation.RELATIVE_TO_SELF,0f);
        translateAnimation.setDuration(200);
        translateAnimation.setRepeatCount(1);


        AlphaAnimation alphaAnimation = new AlphaAnimation(0.1f,1);
        alphaAnimation.setDuration(100);
        alphaAnimation.setRepeatCount(1);
        ll_face_success.setAnimation(alphaAnimation);
        tv_name.startAnimation(translateAnimation);
        tv_num.startAnimation(translateAnimation);

    }

}
