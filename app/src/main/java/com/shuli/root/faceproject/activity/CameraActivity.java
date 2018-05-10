package com.shuli.root.faceproject.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.AccountDao;
import com.shuli.faceproject.greendaodemo.greendao.gen.PeopleDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.bean.Account;
import com.shuli.root.faceproject.bean.People;
import com.shuli.root.faceproject.utils.ClearEditTextWhite;
import com.shuli.root.faceproject.utils.FaceApi;
import com.shuli.root.faceproject.utils.FileUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import megvii.facepass.FacePassException;
import megvii.facepass.FacePassHandler;
import megvii.facepass.types.FacePassAddFaceResult;
import megvii.facepass.types.FacePassConfig;
import megvii.facepass.types.FacePassImageRotation;
import megvii.facepass.types.FacePassModel;
import megvii.facepass.types.FacePassPose;


/**
 * Created by dhht on 16/9/29.
 */

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
    @BindView(R.id.takePhoto)
    TextView takePhoto;
    @BindView(R.id.faceTokenEt)
    TextView faceTokenEt;
    @BindView(R.id.ce_name)
    ClearEditTextWhite ce_name;
    private Camera camera;
    private String filePath;
    private SurfaceHolder holder;
    private boolean isFrontCamera = true;
    private int width = 1280;
    private int height = 960;//之前是设置640*480


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

    private boolean isLocalGroupExist = false;
    private int cameraRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        initView();

        initFacePassSDK();

        initFaceHandler();

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

    @OnClick({R.id.takePhoto,R.id.deleteFace,R.id.bindFace,R.id.toActivity,R.id.toQuery})
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.takePhoto:
                camera.takePicture(null, null, jpeg);
                break;
            case R.id.deleteFace:
                deleteFace();
                break;
            case R.id.bindFace:
                  bindGroupFaceToken();
                break;
            case R.id.toActivity:

                closeCamera();
                startActivity(new Intent(CameraActivity.this,FaceLocalActivity.class));
                finish();
                break;
            case R.id.toQuery:
                toQueryActivity();
                break;
        }

    }

    public void toQueryActivity(){
            if (mFacePassHandler == null) {
                toast("FacePassHandle is null ! ");
                return;
            }

            try {
                byte[][] faceTokens = mFacePassHandler.getLocalGroupInfo(group_name);
                ArrayList<String> faceTokenList = new ArrayList<>();
                if (faceTokens != null && faceTokens.length > 0) {
                    for (int j = 0; j < faceTokens.length; j++) {
                        if (faceTokens[j].length > 0) {
                            faceTokenList.add(new String(faceTokens[j]));
                        }
                    }

                }

                Intent intent = new Intent(this,QueryActivity.class);
                intent.putStringArrayListExtra("faceTokenList",faceTokenList);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                toast("get local group info error!");
            }
    }


    private Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            filePath = FileUtil.getPath() + File.separator + FileUtil.getTime() + ".jpeg";
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.postRotate(0);
            BitmapFactory.Options factory = new BitmapFactory.Options();
            factory = setOptions(factory);
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length, factory);
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
                bm1.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                bm.recycle();
                bm1.recycle();
                stopPreview();
                addFace();
            }
        }
    };

    public static BitmapFactory.Options setOptions(BitmapFactory.Options opts) {
        opts.inJustDecodeBounds = false;
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = 1;
        return opts;
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
        if (mFacePassHandler != null) {
            mFacePassHandler.release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException exception) {

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    private Camera openCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
            } catch (Exception e) {
                camera = null;
                e.printStackTrace();
            }
        }
        return camera;
    }

    private void startPreview() {
        Camera.Parameters para;
        if (null != camera) {
            para = camera.getParameters();
//            获取相机分辨率
//            List<Camera.Size> pictureSizes = para.getSupportedPictureSizes();
//            int length = pictureSizes.size();
//            for (int i = 0; i < length; i++) {
//                Log.i("sss","SupportedPictureSizes : " + pictureSizes.get(i).width + "x" + pictureSizes.get(i).height);
//            }
        } else {
            return;
        }
        para.setPreviewSize(width, height);
        setPictureSize(para, 1280, 960);
        para.setPictureFormat(ImageFormat.JPEG);//设置图片格式
        setCameraDisplayOrientation(isFrontCamera ? 0 : 1, camera);
        camera.setParameters(para);
        camera.startPreview();
    }

    /* 停止预览 */
    private void stopPreview() {
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* 开始预览预览 */
    private void startCameraPreview() {
        if (camera != null) {
            try {
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        rotation = 0;
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void setPictureSize(Camera.Parameters para, int width, int height) {
        int absWidth = 0;
        int absHeight = 0;
        List<Camera.Size> supportedPictureSizes = para.getSupportedPictureSizes();
        for (Camera.Size size : supportedPictureSizes) {
            if (Math.abs(width - size.width) < Math.abs(width - absWidth)) {
                absWidth = size.width;
            }
            if (Math.abs(height - size.height) < Math.abs(height - absHeight)) {
                absHeight = size.height;
            }
        }
        para.setPictureSize(absWidth, absHeight);
    }

    private void closeCamera() {
        if (null != camera) {
            try {
                camera.setPreviewDisplay(null);
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 添加人脸
     */
    public void addFace() {
        if (mFacePassHandler == null) {
            toast("FacePassHandle is null ! ");
            return;
        }

        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            toast("图片不存在 ！");
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        try {
            FacePassAddFaceResult result = mFacePassHandler.addFace(bitmap);
            if (result != null) {
                if (result.result == 0) {
                    toast("添加人脸成功！");
                    faceTokenEt.setText(new String(result.faceToken));//获取到人脸特征


                } else if (result.result == 1) {
                    toast("没有人脸");
                    uploadFinish();
                } else {
                    toast("质量问题！");
                    uploadFinish();
                }
            }
        } catch (FacePassException e) {
            e.printStackTrace();
            toast(e.getMessage());
        }
    }

    /**
     * 删除
     */
        public void deleteFace(){
            if (mFacePassHandler == null) {
                toast("FacePassHandle is null ! ");
                return;
            }
            boolean b = false;
            try {
                byte[] faceToken = faceTokenEt.getText().toString().getBytes();
                b = mFacePassHandler.deleteFace(faceToken);
            } catch (FacePassException e) {
                e.printStackTrace();
                toast(e.getMessage());
            }

            String result = b ? "success " : "failed";
            toast("delete face " + result);
            if(b){
                uploadFinish();
            }
            Log.d("sss", "delete face  " + result);

        }

    //绑定
     public void bindGroupFaceToken(){
            if (mFacePassHandler == null) {
                toast("FacePassHandle is null ! ");
                return;
            }

            byte[] faceToken = faceTokenEt.getText().toString().getBytes();
            if (faceToken == null || faceToken.length == 0 || TextUtils.isEmpty(group_name)) {
                toast("没有人脸特征值！");
                return;
            }
            String name = ce_name.getText().toString();
            if (TextUtils.isEmpty(name)){
                toast("姓名不能为空！");
                return;
            }
            try {
                boolean b = mFacePassHandler.bindGroup(group_name, faceToken);
                if(b){
                    PeopleDao peopleDao = GreenDaoManager.getInstance().getSession().getPeopleDao();
                    peopleDao.insert(new People(name,"",faceTokenEt.getText().toString()));
                }
                String result = b ? "success " : "failed";
                toast("绑定  " + result);
                uploadFinish();
            } catch (Exception e) {
                e.printStackTrace();
                toast(e.getMessage());
            }
        }

    private void uploadFinish() {
        faceTokenEt.setText("");
        ce_name.setText("");
        startCameraPreview();
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }

    }


    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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


}
