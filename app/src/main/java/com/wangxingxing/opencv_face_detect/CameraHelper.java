package com.wangxingxing.opencv_face_detect;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;

public class CameraHelper implements Camera.PreviewCallback {

    private static final String TAG = "CameraHelper";
    //摄像头宽和高，有固定对的值
    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;
    //摄像头id   区分前后摄像头的
    private int mCameraId;
    private Camera mCamera;
    //图片数组
    private byte[] buffer;
    //摄像头预览回调
    private Camera.PreviewCallback mPreviewCallback;

    public CameraHelper(int cameraId) {
        this.mCameraId = cameraId;
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
//        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
//            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
//        } else {
//            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
//        }
//        stopPreview();
//        startPreview();
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        try {
            //获取摄像头Camera实例，并打开前置摄像头
            mCamera = Camera.open(mCameraId);
            //配置Camera属性，首先获取属性列表
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size pictureSize = parameters.getPictureSize();
            //设置预览数据格式为nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            //设置摄像头的宽和高
            parameters.setPreviewSize(WIDTH, HEIGHT);
            mCamera.setParameters(parameters);
            //初始化图片数组
            buffer = new byte[WIDTH * HEIGHT * 3 / 2];
            //添加数据缓存区并设置监听
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            //设置预览画面
            SurfaceTexture surfaceTexture = new SurfaceTexture(11);
            //离屏渲染
            mCamera.setPreviewTexture(surfaceTexture);
            //打开预览
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            //预览回调方法值为null
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放摄像头
            mCamera.release();
            //摄像头实例置null，方便被GC回收
            mCamera = null;
        }
    }

    /**
     * 获取摄像头id
     */
    public int getCameraId() {
        return mCameraId;
    }

    /**
     * 预览画面的回调
     * @param previewCallback
     */
    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.mPreviewCallback = previewCallback;
    }

    /**
     * 预览画面
     * @param data   预览的图片数据
     * @param camera 摄像头实例
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //data数据是倒的
        mPreviewCallback.onPreviewFrame(data, camera);
        camera.addCallbackBuffer(buffer);
    }
}
