package com.wangxingxing.opencv_face_detect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    static {
        System.loadLibrary("native-lib");
    }

    private TextView tvVersion;
    private SurfaceView surfaceView;

    private static final String TAG = "MainActivity";
    public static final int REQUEST_CODE_PERMISSION = 101;
    public static final String APP_FLOADER = "OpenCVFaceDetect";
    public static final String XML_MODEL_PATH = Environment.getExternalStorageDirectory() +
            File.separator + APP_FLOADER + File.separator + "lbpcascade_frontalface.xml";

    private String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private CameraHelper mCameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText(stringFromJNI());

        surfaceView = findViewById(R.id.surface_view);
        //给SurfaceView添加监听
        surfaceView.getHolder().addCallback(this);

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //释放跟踪器
        release();
        mCameraHelper.stopPreview();
    }

    /**
     * 获取OpenCV版本号
     * @return
     */
    public native String stringFromJNI();

    /**
     * 初始化opencv
     * @param model 训练的人脸模型
     */
    public native void init(String model);

    /**
     * 设置画布
     * @param surface 画布
     */
    public native void setSurface(Surface surface);

    /**
     * 处理摄像头的数据
     * @param data     图片数组
     * @param width    宽度
     * @param height   高度
     * @param cameraId 摄像头id 区分前后摄像头
     */
    public native void postData(byte[] data, int width, int height, int cameraId);

    /**
     * 收集样本
     * @param data
     * @param width
     * @param height
     * @param cameraId
     */
    public native void faceCollect(byte[] data, int width, int height, int cameraId);

    /**
     * 释放跟踪器
     */
    public native void release();


    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            for (String str : permissions) {
                if (ContextCompat.checkSelfPermission(this, str) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION);
                } else {
                    Log.i(TAG, "已经拥有的权限：" + str);
                    if (str.equals(Manifest.permission.CAMERA)) {
                      initCamera();
                    } else if (str.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        copyFile();
                    }
                }
            }
        }
    }

    private void initCamera() {
        mCameraHelper = new CameraHelper(cameraId);
        mCameraHelper.setPreviewCallback(this);

        //初始化opencv
        init(XML_MODEL_PATH);
        mCameraHelper.startPreview();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //授权成功
                Log.i(TAG, "授权成功");
                copyFile();
                initCamera();
            } else {
                //拒绝授权
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("申请权限")
                            .setPositiveButton("确定", (dialog1, which) ->
                                    ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION))
                            .setNegativeButton("取消", null)
                            .create()
                            .show();
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void copyFile() {
        FileUtils.getInstance(this).copyAssetsToSD("xml","OpenCVFaceDetect").setFileOperateCallback(new FileUtils.FileOperateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this,"复制asserts文件到sdcard OK！！",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailed(String error) {
                Toast.makeText(MainActivity.this,"复制asserts文件到sdcard error！！",Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mCameraHelper.switchCamera();
            cameraId = mCameraHelper.getCameraId();
            //做一些比如焦点的获取 手动调焦
        }
        return super.onTouchEvent(event);
    }

    /**
     * 摄像头工具类返回的回调
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        postData(data, CameraHelper.WIDTH, CameraHelper.HEIGHT, cameraId);
    }

    /**
     * SurfaceView创建时的回调
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder.getSurface().isValid()) {
            Log.i(TAG, "surfaceCreated: isValid=true");
        }
    }

    /**
     * SurfaceView 画面改变时的回调
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setSurface(holder.getSurface());
    }

    /**
     * SurfaceView被销毁时的回调
     * @param holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
    }

    /**
     * 点击返回按钮时，可以设置直接将采集的样本打包上传到服务器
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isCollect) {
                //如果在采集页面，退出时将采集的info图片上传到服务器
                //首先压缩采集的图片
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long statTime = System.currentTimeMillis();
                        try {
                            HttpAssist.ZipFolder(
                                    Environment.getExternalStorageDirectory() + File.separator + "info",
                                    Environment.getExternalStorageDirectory() + File.separator + "info.zip");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        long zipTime = System.currentTimeMillis();
                        Log.e("压缩文件耗时", "zipTime=" + (zipTime - statTime));
                        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "info.zip");
                        if (!file.exists()) {
                            return;
                        }
                        HttpAssist.uploadFile(file);
                        Log.e("上传文件耗时", "uploadFile=" + (System.currentTimeMillis() - zipTime));
                    }
                }).start();
            }
        }
        */
        return super.onKeyDown(keyCode, event);
    }
}
