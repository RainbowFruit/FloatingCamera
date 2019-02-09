package com.example.myshh.camerapreviewtest;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;


public class FloatingViewService extends Service {
    private WindowManager mWindowManager;
    private View floatingView;
    boolean setBackCamera = true;
    boolean flagCanTakePicture = true;
    private Camera mCamera;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            mCamera.startPreview();
            if (pictureFile == null){
                System.out.println("Creating file failed");
                flagCanTakePicture = true;
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://"+ pictureFile)));
                flagCanTakePicture = true;
            } catch (Exception e){
                e.printStackTrace();
                System.out.println("Saving image failed");
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Inflate the floating view layout we created
        floatingView = LayoutInflater.from(this).inflate(R.layout.list_group, null);

        //Use different flag for older Android versions
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.START;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(floatingView, params);

        prepareCameraView(setBackCamera);

        Button btnMakePhoto = floatingView.findViewById(R.id.btnMakePhoto);
        btnMakePhoto.setOnClickListener(
                v -> {
                    // get an image from the camera
                    if (flagCanTakePicture) {
                        flagCanTakePicture = false;
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        Button btnClose = floatingView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(
                v -> {
                    releaseCamera();
                    stopSelf();
                }
        );

        Button btnSwitchCamera = floatingView.findViewById(R.id.btnSwitchCamera);
        btnSwitchCamera.setOnClickListener(
                v -> {
                    setBackCamera = !setBackCamera;
                    releaseCamera();
                    prepareCameraView(setBackCamera);
                }
        );

        Button btnSizeUp = floatingView.findViewById(R.id.btnSizeUp);
        btnSizeUp.setOnClickListener(v -> resizeLayout(9, 16));

        Button btnSizeDown = floatingView.findViewById(R.id.btnSizeDown);
        btnSizeDown.setOnClickListener(v -> resizeLayout(-9, -16));

        //Move view around screen
        floatingView.findViewById(R.id.linearLayout).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //Remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //Get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        if (Xdiff < 10 && Ydiff < 10) {
                            //stopSelf();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    private void prepareCameraView(boolean setBackCamera){
        mCamera = getCameraInstance(setBackCamera);
        Camera.Parameters cameraParameters = mCamera.getParameters();
        cameraParameters.setPictureSize(cameraParameters.getSupportedPictureSizes().get(0).width,
                cameraParameters.getSupportedPictureSizes().get(0).height);
        mCamera.setParameters(cameraParameters);
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = floatingView.findViewById(R.id.camera_preview);
        preview.removeAllViews();
        preview.addView(mPreview);
    }

    private void resizeLayout(int width, int height) {
        FrameLayout preview = floatingView.findViewById(R.id.camera_preview);
        ViewGroup.LayoutParams layoutParams = preview.getLayoutParams();
        layoutParams.height += 3*height;
        layoutParams.width += 3*width;
        preview.setLayoutParams(layoutParams);
        System.out.println(layoutParams.height + " " + layoutParams.width);
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(boolean setBackCamera){
        Camera c = null;
        try {
            c = Camera.open(setBackCamera ? 0 : 1); // attempt to get a Camera instance
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return c; //Returns null if camera is unavailable
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) mWindowManager.removeView(floatingView);
    }
}