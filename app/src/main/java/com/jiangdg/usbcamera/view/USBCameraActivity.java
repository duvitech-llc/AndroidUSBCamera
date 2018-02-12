package com.jiangdg.usbcamera.view;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jiangdg.usbcamera.utils.FileUtils;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.USBCameraManager;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class USBCameraActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent{
    @BindView(R.id.camera_view)
    public View mTextureView;
    @BindView(R.id.btn_capture_pic)
    public Button mBtnCapture;
    @BindView(R.id.btn_rec_video)
    public Button mBtnRecord;
    @BindView(R.id.btn_update_resolution)
    public Button mBtnUpdateResultion;
    @BindView(R.id.btn_restart_camera)
    Button mBtnRestartCamera;
    @BindView(R.id.btn_contrast)
    Button mBtnContrast;
    @BindView(R.id.btn_brightness)
    Button mBtnBrightness;

    private USBCameraManager mUSBManager;

    private CameraViewInterface mUVCCameraView;

    private boolean isRequest;
    private boolean isPreview;

    private USBCameraManager.OnMyDevConnectListener listener = new USBCameraManager.OnMyDevConnectListener() {
        //
        @Override
        public void onAttachDev(UsbDevice device) {
            if(mUSBManager == null || mUSBManager.getUsbDeviceCount() == 0){
                showShortMsg("No USB Camera Detected");
                return;
            }
            //
            if(! isRequest){
                isRequest = true;
                if(mUSBManager != null){
                    mUSBManager.requestPermission(0);
                }
            }
        }

        //
        @Override
        public void onDettachDev(UsbDevice device) {
            if(isRequest){
                // Turn off camera
                isRequest = false;
                mUSBManager.closeCamera();
                showShortMsg(device.getDeviceName()+" Already Allocated");
            }
        }

        // Connect
        @Override
        public void onConnectDev(UsbDevice device,boolean isConnected) {
            if(! isConnected) {
                showShortMsg("Connection Failed");
                isPreview = false;
            }else{
                isPreview = true;
            }
        }

        // Disconnect
        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("Connection Failed");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbcamera);
        ButterKnife.bind(this);
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
                if(!isPreview && mUSBManager.isCameraOpened()) {
                    mUSBManager.startPreview(mUVCCameraView, new AbstractUVCCameraHandler.OnPreViewResultListener() {
                        @Override
                        public void onPreviewResult(boolean result) {

                        }
                    });
                    isPreview = true;
                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
                if(isPreview && mUSBManager.isCameraOpened()) {
                    mUSBManager.stopPreview();
                    isPreview = false;
                }
            }
        });
        // Initialize usb engine
        mUSBManager = USBCameraManager.getInstance();
        mUSBManager.initUSBMonitor(this,listener);
        mUSBManager.createUVCCamera(mUVCCameraView);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mUSBManager == null)
            return;
        // start
        mUSBManager.registerUSB();
        mUVCCameraView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // stop
        if(mUSBManager != null){
            mUSBManager.unregisterUSB();
        }
        mUVCCameraView.onPause();
    }

    @OnClick({ R.id.btn_contrast,R.id.btn_brightness,R.id.btn_capture_pic, R.id.btn_rec_video,R.id.btn_update_resolution,R.id.btn_restart_camera})
    public void onViewClick(View view) {
        int vId = view.getId();
        switch (vId) {
            // Contrast
            case R.id.btn_contrast:
                if(mUSBManager == null || !mUSBManager.isCameraOpened())
                    return;
                int contrast = mUSBManager.getModelValue(USBCameraManager.MODE_CONTRAST);
                mUSBManager.setModelValue(USBCameraManager.MODE_CONTRAST,contrast++);
                break;
            // Brightness
            case R.id.btn_brightness:
                if(mUSBManager == null || !mUSBManager.isCameraOpened())
                    return;
                int brightness = mUSBManager.getModelValue(USBCameraManager.MODE_BRIGHTNESS);
                mUSBManager.setModelValue(USBCameraManager.MODE_BRIGHTNESS,brightness++);
                break;
            // Restart Camera
            case R.id.btn_restart_camera:

                break;
            // Switch Resolution
            case R.id.btn_update_resolution:
                if(mUSBManager == null || !mUSBManager.isCameraOpened())
                    return;
                mUSBManager.updateResolution(320, 240, new USBCameraManager.OnPreviewListener() {
                    @Override
                    public void onPreviewResult(boolean isSuccess) {
                        if(! isSuccess) {
                            showShortMsg("Format not supported");
                        }else {
                            showShortMsg("Switch To 320x240");
                        }
                    }
                });
                break;

            case R.id.camera_view:
                if(mUSBManager == null)
                    return;
//                mUSBManager.startCameraFoucs();
//                showShortMsg("Focue");
                List<Size> list = mUSBManager.getSupportedPreviewSizes();
                if(list == null) {
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for(Size size:list){
                    sb.append(size.width+"x"+size.height);
                    sb.append("\n");
                }
                showShortMsg(sb.toString());
                break;
            case R.id.btn_capture_pic:
                if(mUSBManager == null || ! mUSBManager.isCameraOpened()){
                    showShortMsg("Snapshot Error");
                    return;
                }
                String picPath = USBCameraManager.ROOT_PATH+System.currentTimeMillis()
                        +USBCameraManager.SUFFIX_PNG;
                mUSBManager.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        showShortMsg("Save："+path);
                    }
                });
                break;
            case R.id.btn_rec_video:
                if(mUSBManager == null || ! mUSBManager.isCameraOpened()){
                    showShortMsg("Camera not ON");
                    return;
                }

                if(! mUSBManager.isRecording()){
                    String videoPath = USBCameraManager.ROOT_PATH+System.currentTimeMillis();
                    FileUtils.createfile(FileUtils.ROOT_PATH+"test666.h264");
                    RecordParams params = new RecordParams();
                    params.setRecordPath(videoPath);
                    params.setRecordDuration(0);
                    params.setVoiceClose(false);
                    mUSBManager.startRecording(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                        @Override
                        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                            // type = 0,aac format audio stream
                            // type = 1,h264 format Video stream
                            if(type == 1){
                                FileUtils.putFileStream(data,offset,length);
                            }
                        }

                        @Override
                        public void onRecordResult(String videoPath) {
                            showShortMsg(videoPath);
                        }
                    });

                    mBtnRecord.setText("Start");
                } else {
                    FileUtils.releaseFile();
                    mUSBManager.stopRecording();
                    mBtnRecord.setText("Stop");
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mUSBManager != null){
            mUSBManager.release();
        }
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBManager.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if(canceled){
            showShortMsg("Cancel");
        }
    }

    public boolean isCameraOpened() {
        return mUSBManager.isCameraOpened();
    }
}
