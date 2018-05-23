package com.shuli.root.faceproject.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;


import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.activity.FaceLocalActivity;
import com.shuli.root.faceproject.base.BaseFragment;
import com.shuli.root.faceproject.utils.ClearEditTextWhite;
import com.shuli.root.faceproject.utils.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by dhht on 16/9/29.
 */

public class AddFragment extends BaseFragment implements SurfaceHolder.Callback {
    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
    @BindView(R.id.faceTokenEt)
    TextView faceTokenEt;
    @BindView(R.id.ce_name)
    ClearEditTextWhite ce_name;
    @BindView(R.id.ce_gong_num)
    ClearEditTextWhite ce_gong_num;
    private Camera camera;
    private String filePath;
    private SurfaceHolder holder;
    private boolean isFrontCamera = true;//1是前置0是后置
    private int width = 800;
    private int height = 600;
    private int CammeraIndex;
    private OnFragmentInteractionListener mListener;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_add;
    }


//    SupportedPictureSizes : 1280x720
//    SupportedPictureSizes : 320x240
//    SupportedPictureSizes : 800x600
//    SupportedPreviewSizes : 640x480
//    SupportedPreviewSizes : 1280x720
//    SupportedPreviewSizes : 320x240
//    SupportedPreviewSizes : 800x600
    @Override
    protected void init() {
        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @OnClick({R.id.takePhoto,R.id.deleteFace,R.id.bindFace,R.id.toActivity,R.id.toQuery})
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.takePhoto:
                takePhoto();
                break;
            case R.id.deleteFace:
                deleteFace();
                break;
            case R.id.bindFace:
                bindGroupFaceToken();
                break;
            case R.id.toActivity:
                String token = faceTokenEt.getText().toString();
                if(!TextUtils.isEmpty(token)){
                    deleteFace();
                }
                closeCamera();
                startActivity(new Intent(getActivity(),FaceLocalActivity.class));
                getActivity().finish();
                break;
            case R.id.toQuery:
             mListener.toQueryActivity();
                break;
        }

    }


    private void bindGroupFaceToken() {
        String token = faceTokenEt.getText().toString();
        String name = ce_name.getText().toString();
        String gong_num = ce_gong_num.getText().toString();

        if(!TextUtils.isEmpty(token)){
            boolean b= mListener.bindGroupFaceToken(token,name,gong_num);
            if(b){
                uploadFinish();
            }
        }
    }

    private void deleteFace() {
       String token = faceTokenEt.getText().toString();
        if(!TextUtils.isEmpty(token)){
            boolean b= mListener.deleteFace(token);
            if(b){
                uploadFinish();
            }
        }
    }

    /**
     * 添加人脸
     */
    public void addFace() {
        String result = mListener.addFace(filePath);
        if(!TextUtils.isEmpty(result)){
            faceTokenEt.setText(new String(result));//获取到人脸特征
        }else {
            startCameraPreview();
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }


    private void takePhoto(){
        camera.takePicture(null, null, jpeg);
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


    private void uploadFinish() {
        faceTokenEt.setText("");
        ce_name.setText("");
        ce_gong_num.setText("");
        startCameraPreview();
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
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

    @Override
    public void onResume() {
        super.onResume();
        camera = openCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
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
                 CammeraIndex=FindFrontCamera();
                if(CammeraIndex==-1){
                    CammeraIndex=FindBackCamera();
                    isFrontCamera = false;
                }
                camera = Camera.open(CammeraIndex);
            } catch (Exception e) {
                camera = null;
                e.printStackTrace();
            }
        }
        return camera;
    }

    //寻找前置摄像头
    @TargetApi(9)
    private int FindFrontCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }
    //寻找后置摄像头
    @TargetApi(9)
    private int FindBackCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_BACK ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    private void startPreview() {
        Camera.Parameters para;
        if (null != camera) {
            para = camera.getParameters();
            //获取相机支持的大小
//            List<Camera.Size> pictureSizes = para.getSupportedPictureSizes();
//            int length = pictureSizes.size();
//            for (int i = 0; i < length; i++) {
//                Log.e("sss","SupportedPictureSizes : " + pictureSizes.get(i).width + "x" + pictureSizes.get(i).height);
//            }
//
//            List<Camera.Size> previewSizes = para.getSupportedPreviewSizes();
//            length = previewSizes.size();
//            for (int i = 0; i < length; i++) {
//                Log.e("sss","SupportedPreviewSizes : " + previewSizes.get(i).width + "x" + previewSizes.get(i).height);
//            }
        } else {
            return;
        }
        para.setPreviewSize(width, height);
        setPictureSize(para,800 , 600);
        para.setPictureFormat(ImageFormat.JPEG);//设置图片格式
        setCameraDisplayOrientation(CammeraIndex, camera);
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

    public void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
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
        para.setPictureSize(absWidth,absHeight);
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

    public static BitmapFactory.Options setOptions(BitmapFactory.Options opts) {
        opts.inJustDecodeBounds = false;
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = 1;
        return opts;
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        String addFace(String data);
        boolean deleteFace(String token);
        boolean bindGroupFaceToken(String token,String name,String gong_num);
        void toQueryActivity();
     }

}
