package com.shuli.root.faceproject.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Power;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.bjw.bean.ComBean;
import com.bjw.utils.SerialHelper;
import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.PeopleDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.bean.People;
import com.shuli.root.faceproject.camera.CameraManager;
import com.shuli.root.faceproject.camera.CameraPreview;
import com.shuli.root.faceproject.camera.CameraPreviewData;
import com.shuli.root.faceproject.face.FaceView;
import com.shuli.root.faceproject.network.ByteRequest;
import com.shuli.root.faceproject.utils.FaceApi;
import com.shuli.root.faceproject.utils.SettingVar;
import com.shuli.root.faceproject.utils.SoundPoolUtil;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import WedoneBioVein.MessageFinger;
import WedoneBioVein.SdkMain;
import WedoneBioVein.UserData;
import WedoneBioVein.VeinMatchCaller;
import android_serialport_api.SerialPortFinder;
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
import megvii.facepass.types.FacePassRecognitionResultType;

import static java.lang.Thread.sleep;


public class FaceLocalActivity extends AppCompatActivity implements CameraManager.CameraListener, View.OnClickListener {

    private enum FacePassSDKMode {
        MODE_ONLINE,
        MODE_OFFLINE
    };

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

    private static final int cameraWidth = 1920;
    private static final int cameraHeight = 1080;

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

    private Handler mAndroidHandler;

    private TextView tv_name;
    private TextView tv_num;
    private TextView tv_result;
    private boolean isOpenDoor = false;

    private Handler handler = new Handler();

    //二维码部分
    private String code = "";
    private SerialPortFinder serialPortFinder;
    private SerialHelper serialHelper;
    private TextView tv_code,fingerTextView;
   //指静脉部分
    private SdkMain mSdkMain = null;
    UserData mRegUserData = new UserData(); //用于保存采集(注册)的模板
    UserData mAIUserData = new UserData(); //用于保存验证时通过自动学习生成的模板，在比对时也作为比对模板的一部分
    int mUserCnt = 0;

    int mMaxVeinDeviceNum = 20;
    int mVeinDevIdLen = 64;
    byte[][] mVeinDevIdList = null;
    int mVeinDevCnt = 0;


    Handler handlerFinger = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            fingerTextView.setText((CharSequence) msg.obj);
        }
    };
    ////
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
        InitOperations();
        doBtnEnumDevice();
        iniCodeView();
        Power.set_zysj_gpio_value(1,1);//do2底 开门
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
                                if (FacePassRecognitionResultType.RECOG_OK == result.facePassRecognitionResultType) {//成功显示对比的人脸图片
                                    getFaceImageByFaceToken(result.trackId, faceToken);
                                }

                                showRecognizeResult(result.trackId, result.detail.searchScore, result.detail.livenessScore, !TextUtils.isEmpty(faceToken),faceToken);
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
                Log.i("sss","ID = " + trackId + (isRecognizeOK ? "识别成功" : "识别失败") + "\n");
                Log.i("sss","识别分 = " + searchScore + "\n");
                Log.i("sss","活体分 = " + livenessScore + "\n");

                if(isRecognizeOK){
                    People people = GreenDaoManager.getInstance().getSession().getPeopleDao().queryBuilder()
                            .where(PeopleDao.Properties.Face_token.eq(token)).build().unique();

                    SoundPoolUtil.play(1);
                    Power.set_zysj_gpio_value(1,0);//do2底 开门
                    if(people != null){
                        tv_name.setText(people.getName());
                        tv_num.setText(people.getGonghao());
                    }
                    tv_result.setText("验证成功");
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tv_name.setText("");
                            tv_num.setText("");
                            tv_result.setText("");
                        Power.set_zysj_gpio_value(1,1);//do2高 关门
                        }
                    },2000);

                }else {
                    tv_result.setText("验证失败");
                   // SoundPoolUtil.play(2);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tv_result.setText("");
                        }
                    },2000);
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
        tv_name = findViewById(R.id.tv_name);
        tv_num = findViewById(R.id.tv_num);
        tv_result = findViewById(R.id.tv_result);
        //二维码
        tv_code = findViewById(R.id.tv_code);
        //指静脉
        fingerTextView = findViewById(R.id.fingerTextView);
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

        AssetManager mgr = getAssets();
         faceView =  this.findViewById(R.id.fcview);
        SettingVar.cameraSettingOk = false;

        manager = new CameraManager();
        cameraView = findViewById(R.id.preview);
        manager.setPreviewDisplay(cameraView);
        frameLayout =  findViewById(R.id.frame);
        /* 注册相机回调函数 */
        manager.setListener(this);
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
        serialHelper.close();

        doCloseDevice();
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

    public void showToast(CharSequence text, int duration, boolean isSuccess, Bitmap bitmap) {
        LayoutInflater inflater = getLayoutInflater();
        View toastView = inflater.inflate(R.layout.toast, null);
        LinearLayout toastLLayout = (LinearLayout) toastView.findViewById(R.id.toastll);
        if (toastLLayout == null) {
            return;
        }
        toastLLayout.getBackground().setAlpha(100);
        ImageView imageView = toastView.findViewById(R.id.toastImageView);
        TextView idTextView = toastView.findViewById(R.id.toastTextView);
        TextView stateView = toastView.findViewById(R.id.toastState);
        SpannableString s;
        if (isSuccess) {
            s = new SpannableString("验证成功");
            imageView.setImageResource(R.drawable.success);
        } else {
            s = new SpannableString("验证失败");
            imageView.setImageResource(R.drawable.success);
        }
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
        stateView.setText(s);
        idTextView.setText(text);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(duration);
        toast.setView(toastView);

        if (mToastBlockQueue.size() == 0) {
            mAndroidHandler.removeMessages(MSG_SHOW_TOAST);
            mAndroidHandler.sendEmptyMessage(MSG_SHOW_TOAST);
            mToastBlockQueue.offer(toast);
        } else {
            mToastBlockQueue.offer(toast);
        }
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
        startActivity(new Intent(this,MainFragmentActivity.class));
    }

    private void getFaceImageByFaceToken(final long trackId, String faceToken) {
        if (TextUtils.isEmpty(faceToken)) {
            return;
        }

        final String faceUrl = "http://" + serverIP + ":8080/api/image/v1/query?face_token=" + faceToken;

        final Bitmap cacheBmp = mImageCache.getBitmap(faceUrl);
        if (cacheBmp != null) {
            mAndroidHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(DEBUG_TAG, "getFaceImageByFaceToken cache not null");
                    showToast("ID = " + String.valueOf(trackId), Toast.LENGTH_SHORT, true, cacheBmp);
                }
            });
            return;
        } else {
            try {
                final Bitmap bitmap = mFacePassHandler.getFaceImage(faceToken.getBytes());
                mAndroidHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(DEBUG_TAG, "getFaceImageByFaceToken cache is null");
                        showToast("ID = " + String.valueOf(trackId), Toast.LENGTH_SHORT, true, bitmap);
                    }
                });
                if (bitmap != null) {
                    return;
                }
            } catch (FacePassException e) {
                e.printStackTrace();
            }

        }
        ByteRequest request = new ByteRequest(Request.Method.GET, faceUrl, new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeByteArray(response, 0, response.length, options);
                mImageCache.putBitmap(faceUrl, bitmap);
                showToast("ID = " + String.valueOf(trackId), Toast.LENGTH_SHORT, true, bitmap);
                Log.i(DEBUG_TAG, "getFaceImageByFaceToken response ");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(DEBUG_TAG, "image load failed ! ");
            }
        });
        request.setTag("load_image_request_tag");
        requestQueue.add(request);
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

    private void iniCodeView() {

        serialPortFinder = new SerialPortFinder();
        serialHelper = new SerialHelper() {
            @Override
            protected void onDataReceived(final ComBean comBean) {
//                Log.i("sss",FuncUtil.ByteArrToHex(comBean.bRec));
                String str = new String(comBean.bRec);
                     Log.i("sss","xxxxx " + str);
                String s = str.substring(str.length() - 1, str.length());
                if (s.equals("\n")) {
                    s = str.substring(0, str.length() - 1);
                    code = code + s;
                    if(code.contains("\n")){
                        int num = code.indexOf("\n");
                        code = code.substring(0,num);
                    }
                    if(code.equals("suriot -> open gate")){
                        isOpenDoor = true;
                        Power.set_zysj_gpio_value(1,0);//do2底 开门
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tv_code.setText(code);
                            Log.i("sss",code);
                            code = "";
                        }
                    });
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tv_code.setText("");
                            if(isOpenDoor){
                                Power.set_zysj_gpio_value(1,1);//do2高 关门
                                isOpenDoor = false;
                            }
                        }
                    },2000);

                } else {
                    code += str;
                }
            }
        };
        serialHelper.setPort("/dev/ttyS1");
        serialHelper.setBaudRate("9600");

        if(!serialHelper.isOpen()){
            try {
                serialHelper.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //初始化
    private void InitOperations(){
        if(null == mSdkMain){
            mSdkMain = new SdkMain();
            mVeinDevIdList = new byte[mMaxVeinDeviceNum][];
            for(int nCnt = 0; nCnt < mMaxVeinDeviceNum; nCnt++){
                mVeinDevIdList[nCnt] = new byte[mVeinDevIdLen];
            }
            mRegUserData.ClearData();
            mAIUserData.ClearData();
        }
    }
    //枚举设备按钮
    /**/
    public void doBtnEnumDevice() {
        /**
         * @返回 int: 0=正确，其他=错误代码
         */
        int retVal = mSdkMain.FV_EnumDevice(mVeinDevIdList);
        if(mSdkMain.FV_ERRCODE_SUCCESS == retVal){
            mVeinDevCnt = 0;
            String devId = null, msg = "";
            for(int devIdx = 0; devIdx < mMaxVeinDeviceNum; devIdx++){
                devId = new String(mVeinDevIdList[devIdx]);
                if(0 == devId.trim().length()) {
                    break;
                }
                mVeinDevCnt++;
                msg += "{Dev" + (devIdx + 1) + ":" + devId + "}";
            }

            if(0 >= mVeinDevCnt){
                DisplayNoticeMsg("不存在有效的指静脉设备！", 0);
                return;
            }else {
                Log.i("sss","枚举设备成功！" +msg);
                doBtnInitDevice();
            }
        }
        else{
            Log.i("sss","枚举初始化失败！");
        }
        return;
    }

    //初始化设备按钮
    public void doBtnInitDevice() {
        int retVal;

        String devId = null;
        for(int devIdx = 0; devIdx < mVeinDevCnt; devIdx++){
            devId = new String(mVeinDevIdList[devIdx]);
            if(0 == devId.trim().length()) {
                break;
            }

            retVal = mSdkMain.FV_InitDevice(mVeinDevIdList[devIdx]);//初始化设备
            if(mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                retVal = mSdkMain.FV_OpenDevice(mVeinDevIdList[devIdx]);//打开设备按钮
                if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                    if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                        DisplayNoticeMsg("设备初始化成功！", 0);
                    } else {
                        DisplayNoticeMsg("设备初始化失败！", 0);
                    }
                }
            }
        }
        return;
    }

    private  byte[] mergeBytes(byte[] part1, byte[] part2){
        int p1Len = (null == part1)?0:part1.length;
        int p2Len = (null == part2)?0:part2.length;

        if(0 == (p1Len + p2Len)){
            return null;
        }

        byte[] ret = new byte[p1Len + p2Len];
        for(int i = 0; i < p1Len; i++){
            ret[i] = part1[i];
        }
        for(int i = 0; i < p2Len; i++){
            ret[p1Len + i] = part2[i];
        }
        SdkMain.DebugStringPrint("mergeBytes:p1Len=" + p1Len + ",p2Len=" + p2Len);
        return ret;
    }

    public  void DisplayNoticeMsg(String msg, int action){
        Message msgObj = new Message();
        msgObj.obj = msg;
        handlerFinger.sendMessage(msgObj);
        return;
    }

    /**
     * Wedone: 等待手指传感器变为某种指定的状态，等待的时间为nInterval*nTimes
     *
     * @参数(IN)  byte bFingerStatus: 等待的状态；0：手指已经移开，3：手指已经放置好。
     * @参数(IN)  int nTimes: 检测的次数，必须大于0。
     * @参数(IN)  int nInterval: 每次检测的间隔，单位为毫秒，建议在500 - 1000毫秒之间。
     * @调用 public
     * @返回 boolean: true=成功的等到了指定的状态：
     *             false=没有等到指定的状态就超时了
     */
    public boolean WaitFingerStatus(byte[] devId, byte bFingerStatus, int nTimes, int nInterval, String msghdr){
        if((0 >= nTimes) || (200 >= nInterval) || (1000 < nInterval)){
            return false;
        }
        String msg = "";
        byte[] fingerStatus = new byte[1];
        int retVal;
        if(null != msghdr){
            msg = msghdr;
        }
        else if(0 == bFingerStatus){
            msg = "请移开手指！";
        }
        else if(0x03 == bFingerStatus){
            msg = "请放手指！";
        }
        for(int nCnt = 0; nCnt < nTimes; nCnt++){
            //  DisplayNoticeMsg(msg, 0);
            retVal = mSdkMain.FV_FingerDetect(devId, fingerStatus);
            if (mSdkMain.FV_ERRCODE_SUCCESS != retVal) {
                //  DisplayNoticeMsg("检测手指放置状态失败，错误码=" + retVal + "!\r\n", 0);
                return false;
            }
            if(bFingerStatus == fingerStatus[0]){
                return true;
            }
            try {
                sleep(nInterval, 0);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
            if(0 == (nCnt%(1000/nInterval))) {
                msg += "*";
            }
        }
        return false;
    }


    //注册1根手指(3次)按钮
    public void OnClickBtnRegister(View v) {
        if (0 >= mVeinDevCnt) {

            Log.i("sss","不存在有效的指静脉设备，请先进行枚举设备操作！");
            DisplayNoticeMsg("不存在有效的指静脉设备，请先进行枚举设备操作！", 0);
            return;
        }
        if (mRegUserData.D_USER_TEMPLATE_NUM <= mRegUserData.GetTemplateNum()) {
            Log.i("sss","单个用户最多只能注册20个模板！\n");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retVal;
                String devId = null, msg = "";
                int readCnt = 0; //读取模板次数的计数器
                int retryCnt = 0;
                byte[] featureData = new byte[SdkMain.FV_CONST_FEATURE_LEN];
                int devIdx = 0; //Wedone:指定在第一台设备上进行操作

                devId = new String(mVeinDevIdList[devIdx]);
                if (0 == devId.trim().length()) {//Wedone:无效的设备ID，则直接返回
                    return;
                }

                //清除上次注册的数据
                mRegUserData.ClearData();
                mAIUserData.ClearData();

                while(true) {
                    if(6 <= retryCnt)break; //已经重试了6次，结束采集
                    retryCnt++;
                    //Wedone:检测手指，直到检测到手指已经放好才进行后续读取静脉特征的操作
                    boolean isWaitSuccess = true;
                    msg = "正在{" + devId + "}上采集第" + (readCnt + 1) + "个静脉特征:";
                    DisplayNoticeMsg("采集第" + (readCnt + 1)  + "个静脉特征，"+"请放入手指！", 0);
                    SoundPoolUtil.play(1);
                    isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x03, 20, 500, msg + "请放手指!");
                    if (!isWaitSuccess) {
                        DisplayNoticeMsg("没有发现手指", 0);
                        SoundPoolUtil.play(7);
                        handler.postDelayed(runnable, 1000);
                        return;
                    }

                    //Wedone:读取指静脉特征模板数据
                    retVal = mSdkMain.FV_GrabFeature(mVeinDevIdList[devIdx], featureData, (byte) 0);
                    if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                        //  DisplayNoticeMsg("读取成功！", 0);
                    } else {
                        DisplayNoticeMsg("读取失败！", 0);
                        SoundPoolUtil.play(6);
                        handler.postDelayed(runnable, 1000);
                        break; //Wedone: 采集过程中发生错误，则直接退出
                    }

                    //Wedone:检测手指，直到检测到手指已经移开才进行后续读操作，确保每次采集都是重新放置了手指而不是手指一直放着不动
                    msg = "读取完成第" + (readCnt + 1) + "个静脉特征:";
                    DisplayNoticeMsg("请移开手指！", 0);
                    SoundPoolUtil.play(2);
                    isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x00, 20, 500, msg + "请移开手指!");
                    if (!isWaitSuccess) {
                        DisplayNoticeMsg("没有移开手指！", 0);
                        handler.postDelayed(runnable, 1000);
                        return;
                    }

                    //Wedone:确认采集的指静脉特征数据是否有效
                    retVal = VeinMatchCaller.FvmIsValidFeature(featureData, (byte)0x01);
                    if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                        DisplayNoticeMsg("错误：指静脉特征数据无效！\r\n", 0);
                        SoundPoolUtil.play(3);
                        continue;
                    }

                    //Wedone:调用FV_GrabFeature返回成功的话，第二个参数的缓冲区中就保存了所读取的模板数据，
                    if (0 == readCnt) {
                        //采集完成第一个静脉特征，生成对应的用户信息
                        mUserCnt++;
                        byte bUserId[] = new byte[mRegUserData.D_USER_HDR_USERID_LEN];
                        byte bUserName[] = new byte[mRegUserData.D_USER_HDR_USERNAME_LEN];

                        mRegUserData.SetUid((long) mUserCnt);

                        bUserId[0] = 'I';
                        bUserId[1] = 'D';
                        bUserId[2] = (byte) (0x30 + (mUserCnt % 10000) / 1000);
                        bUserId[3] = (byte) (0x30 + (mUserCnt % 1000) / 100);
                        bUserId[4] = (byte) (0x30 + (mUserCnt % 100) / 10);
                        bUserId[5] = (byte) (0x30 + (mUserCnt % 10));
                        mRegUserData.SetUserId(bUserId, (short) 6);

                        bUserName[0] = 'U';
                        bUserName[1] = 'S';
                        bUserName[2] = 'E';
                        bUserName[3] = 'R';
                        bUserName[4] = (byte) (0x30 + (mUserCnt % 10000) / 1000);
                        bUserName[5] = (byte) (0x30 + (mUserCnt % 1000) / 100);
                        bUserName[6] = (byte) (0x30 + (mUserCnt % 100) / 10);
                        bUserName[7] = (byte) (0x30 + (mUserCnt % 10));
                        mRegUserData.SetUserName(bUserName, (short) 8);
                    }
                    if(0 < readCnt){ //Wedone:之前已经有采集的特征，把当前采集的静脉特征与之前采集的进行验证是否属于同一根手指
                        byte[] regTemplateData = mRegUserData.TemplateData();
                        //Wedone: 注册过程中，检测采集的静脉特征是否属于同一根手指
                        retVal = VeinMatchCaller.FvmIsSameFinger(featureData,//本次采集的指静脉特征值
                                regTemplateData,  //包含之前采集的指静脉特征值数据
                                (byte)readCnt, //第二个参数中包含的指静脉特征值的个数
                                (byte)0x03); //加密方式，当前请固定为3
                        if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                            DisplayNoticeMsg("错误：注册过程中采集的特征必须属于同一根手指！\r\n", 0);
                            SoundPoolUtil.play(4);
                            continue;
                        }
                    }
                    //Wedone：采集的特征值符合要求，保存到本地缓冲区中
                    mRegUserData.AddTemplateData(featureData, (short) featureData.length);
                    readCnt++;

                    if(3 <= readCnt){
                        DisplayNoticeMsg("采集指静脉特征成功！已采集" + mRegUserData.GetTemplateNum() + "个特征模板\r\n", 0);
                        SoundPoolUtil.play(5);
                        handler.postDelayed(runnable, 1000);
                        // TODO: 2018/4/18 将获取到的模板数组上传服务器
                        byte[] regTemplateData = mRegUserData.TemplateData(); //获取注册采集的特征数据

//                        MessageFinger messageFinger = new MessageFinger();
//                        messageFinger.setRegTemplateData(regTemplateData);
//                        messageFinger.setName("xxxxx");
//                        Gson gson = new Gson();
//                        String postInfoStr = gson.toJson(messageFinger);
//                        Log.i("sss","postInfoStr" + postInfoStr);
                        //upload(postInfoStr);

                        break; //采集完成3个有效模板，则结束采集
                    }
                }
            }
        }).start();
        return;
    }

    //验证手指按钮
    public void OnClickBtnIdentify(View v) {
        if (0 >= mVeinDevCnt) {
            DisplayNoticeMsg("不存在有效的指静脉设备，请先进行枚举设备操作！", 0);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retVal;
                String devId = null, msg = "";
                byte[] featureData = new byte[SdkMain.FV_CONST_FEATURE_LEN];
                int devIdx = 0; //Wedone:指定在第一台设备上进行操作

                devId = new String(mVeinDevIdList[devIdx]);
                if (0 == devId.trim().length()) {//Wedone:无效的设备ID，则直接返回
                    return;
                }

                //Wedone:检测手指，直到检测到手指已经放好才进行后续读取静脉特征的操作
                boolean isWaitSuccess = true;
                msg = "正在{" + devId + "}上进行验证：";
                DisplayNoticeMsg("请放手指!", 0);
                isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x03, 20, 500, msg + "请放手指!");
                if (!isWaitSuccess) {
                    return;
                }

                //Wedone:读取指静脉特征模板数据
                retVal = mSdkMain.FV_GrabFeature(mVeinDevIdList[devIdx], featureData, (byte) 0);
                if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                    msg += "成功";
                    DisplayNoticeMsg("读取成功！", 0);
                } else {
                    msg += "err=" + retVal;
                    DisplayNoticeMsg("失败！", 0);
                    return; //Wedone: 采集过程中发生错误，则直接退出
                }

                //Wedone:检测手指，直到检测到手指已经移开才进行后续读操作，确保每次采集都是重新放置了手指而不是手指一直放着不动
                msg = "采集验证模板成功:";
                DisplayNoticeMsg("请移开手指!", 0);
                isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x00, 20, 500, msg + "请移开手指!");
                if (!isWaitSuccess) {
                    return;
                }

                //Wedone:确认采集的指静脉特征数据是否有效
                retVal = VeinMatchCaller.FvmIsValidFeature(featureData, (byte)0x01);
                if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                    DisplayNoticeMsg("错误：指静脉特征数据无效！\r\n", 0);
                    return;
                }
                /////////////////

                int regTemplateCnt = mRegUserData.GetTemplateNum();
                int aiTemplateCnt = mAIUserData.GetTemplateNum();
                byte[] regTemplateData = mRegUserData.TemplateData(); //获取注册采集的特征数据
                byte[] aiTemplateData = mAIUserData.TemplateData(); //获取AI自学习的特征数据
                byte[] aiTemplateBuff = new byte[UserData.D_USER_TEMPLATE_SIZE*3]; //准备3个模板大小的缓冲区用于自动学习
                byte[] mergeTemplateData = mergeBytes(regTemplateData, aiTemplateData); //把注册时采集的特征数据和AI自学习的数据合并起来验证
                if(mergeTemplateData == null){
                    DisplayNoticeMsg("没有注册的指静脉！！！", 0);
                    return;
                }
                byte securityLevel = 4;
                int[] diff = new int[1];
                int[] AIDataLen = new int[1];
                diff[0] = 10000;
                AIDataLen[0] = UserData.D_USER_TEMPLATE_SIZE*3;
                SdkMain.DebugStringPrint("指静脉比对:regTemplateCnt=" + regTemplateCnt + ",aiTemplateCnt=" + aiTemplateCnt);
                //Wedone: 调用静脉特征值比对接口进行比对，各个参数意义如下说明
                retVal = VeinMatchCaller.FvmMatchFeature(featureData, //采集的用于验证的指静脉特征值
                        mergeTemplateData, //包含注册的特征值+AI自学习的特征值数据
                        (byte) (regTemplateCnt + aiTemplateCnt), //第二个参数的比对特征值的个数，包含注册+AI自学习的特征值个数
                        (byte) 0x03, //加密方式，当前请固定为3
                        securityLevel, //比对时使用的安全级别， 1:1场景：范围[6-10],建议值为6，1:N场景：范围[1-5],建议值为4
                        diff, //用于返回比对结果的差异度值
                        aiTemplateBuff, //用于输出比对时自动学习生成的特征数据
                        AIDataLen); //输入时初始化值为学习缓冲区的大小，验证通过返回时为学习成功的数据长度
                if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                    DisplayNoticeMsg("验证失败！！！", 0);
                    return;
                }
                //Wedone：验证通过，并且返回的AI学习缓冲区的数据长度大于0，保存AI学习的数据
                if(0 < AIDataLen[0]){
                    mAIUserData.ClearData();
                    mAIUserData.SetTemplateData(aiTemplateBuff, (short)AIDataLen[0]);
                }
                DisplayNoticeMsg("验证通过！差异度=" + diff[0] + "，学习数据长度=" + AIDataLen[0], 0);

            }
        }).start();
        return;
    }

    //关闭设备按钮
    public void doCloseDevice() {
        if(0 >= mVeinDevCnt){
            return;
        }

        int retVal;
        String devId = null, msg = "";
        for(int devIdx = 0; devIdx < mVeinDevCnt; devIdx++){
            devId = new String(mVeinDevIdList[devIdx]);
            if(0 == devId.trim().length()) {
                break;
            }
            retVal = mSdkMain.FV_CloseDevice(mVeinDevIdList[devIdx]);
            if(mSdkMain.FV_ERRCODE_SUCCESS == retVal){
                msg += "{" + devId + ":成功}";
            }
            else{
                msg += "{" + devId + ":err=" + retVal + "}";
            }
        }
        Log.i("sss","初始化设备：" + msg);

        return;
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            fingerTextView.setText("");
        }
    };


}
