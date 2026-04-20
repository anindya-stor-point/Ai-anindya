package org.ai.tools.aivisionguide;

import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjection;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.DisplayManager;
import android.media.ImageReader;
import android.media.Image;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class NativeHelper {
    public static MediaProjectionManager mpm;
    public static MediaProjection mediaProjection;
    public static VirtualDisplay virtualDisplay;
    public static ImageReader imageReader;
    public static int mWidth;
    public static int mHeight;
    public static int mScreenDensity;

    public static WindowManager windowManager;
    public static FrameLayout floatingView;
    public static TextView tipText;
    public static View redBox;

    public static void requestCapture(Activity activity, int requestCode) {
        if (mpm == null) {
            mpm = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
        if (mpm != null) {
            Intent intent = mpm.createScreenCaptureIntent();
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public static void initProjection(Activity activity, int resultCode, Intent data) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        mScreenDensity = metrics.densityDpi;
        mWidth = metrics.widthPixels;
        mHeight = metrics.heightPixels;

        mediaProjection = mpm.getMediaProjection(resultCode, data);
        imageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                mWidth, mHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
    }

    public static void captureFrame(String path) {
        try {
            if (imageReader == null) return;
            Image image = imageReader.acquireLatestImage();
            if (image == null) return;

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * mWidth;

            Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();

            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight);
            FileOutputStream fos = new FileOutputStream(path);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos);
            fos.close();
            bitmap.recycle();
            croppedBitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showGuidance(Context context, float x_p, float y_p, String tip) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (windowManager == null) {
                        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    }

                    if (floatingView == null) {
                        floatingView = new FrameLayout(context);
                        int type = android.os.Build.VERSION.SDK_INT >= 26 ? 
                                   WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                                   WindowManager.LayoutParams.TYPE_PHONE;

                        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            type,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            PixelFormat.TRANSLUCENT
                        );
                        params.gravity = Gravity.TOP | Gravity.LEFT;

                        tipText = new TextView(context);
                        tipText.setTextColor(Color.WHITE);
                        tipText.setBackgroundColor(Color.argb(220, 30, 30, 30));
                        tipText.setTextSize(22);
                        tipText.setPadding(40, 40, 40, 40);
                        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        textParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                        textParams.bottomMargin = 150;
                        floatingView.addView(tipText, textParams);

                        redBox = new View(context);
                        GradientDrawable border = new GradientDrawable();
                        border.setColor(Color.TRANSPARENT);
                        border.setStroke(12, Color.RED);
                        redBox.setBackground(border);
                        
                        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(180, 180);
                        floatingView.addView(redBox, boxParams);

                        windowManager.addView(floatingView, params);
                    }

                    int dw = context.getResources().getDisplayMetrics().widthPixels;
                    int dh = context.getResources().getDisplayMetrics().heightPixels;

                    int cx = (int) ((x_p / 100.0) * dw);
                    int cy = (int) ((y_p / 100.0) * dh);
                    
                    FrameLayout.LayoutParams boxParams = (FrameLayout.LayoutParams) redBox.getLayoutParams();
                    boxParams.leftMargin = cx - 90; 
                    boxParams.topMargin = cy - 90;
                    redBox.setLayoutParams(boxParams);

                    tipText.setText(tip);
                    floatingView.setVisibility(View.VISIBLE);
                    
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (floatingView != null) {
                                floatingView.setVisibility(View.GONE);
                            }
                        }
                    }, 4000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
