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
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Base64;

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

    public static boolean isLooping = false;

    public static void requestCapture(Activity activity, int requestCode) {
        if (mpm == null) {
            mpm = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
        if (mpm != null) {
            Intent intent = mpm.createScreenCaptureIntent();
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public static void startContinuousAnalysis(Context context, String apiKey, Intent staticDataIntent, int screenWidth, int screenHeight, int densityDpi) {
        if (isLooping) return;
        isLooping = true;
        
        mWidth = screenWidth;
        mHeight = screenHeight;
        mScreenDensity = densityDpi;

        if (mpm == null) {
            mpm = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        // -1 corresponds to Activity.RESULT_OK
        mediaProjection = mpm.getMediaProjection(-1, staticDataIntent);
        
        // Scale down to 720p to make memory copy and HTTP transit extremely fast
        int captureWidth = 720;
        int captureHeight = (int)((720.0 / mWidth) * mHeight);
        
        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2);
        
        virtualDisplay = mediaProjection.createVirtualDisplay("AIVisionGuide",
                captureWidth, captureHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        Thread analysisThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isLooping) {
                    try {
                        Thread.sleep(2000); // Poll every 2 seconds for real-time responsiveness without API flood
                        
                        Image image = imageReader.acquireLatestImage();
                        if (image == null) continue;

                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * captureWidth;

                        Bitmap bitmap = Bitmap.createBitmap(captureWidth + rowPadding / pixelStride, captureHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        image.close();

                        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, captureWidth, captureHeight);
                        
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos);
                        byte[] jpegData = bos.toByteArray();
                        bitmap.recycle();
                        croppedBitmap.recycle();

                        String base64Image = Base64.encodeToString(jpegData, Base64.NO_WRAP);
                        
                        // Async Fast HTTP Post
                        String result = analyzeWithGemini(apiKey, base64Image);
                        if (result != null) {
                            parseAndMakeOverlay(context, result);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        analysisThread.start();
    }

    private static String analyzeWithGemini(String apiKey, String base64Image) {
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);

            String prompt = "Role: Android UI Expert & Visual Guide.\\nTask: Analyze the screenshot and provide one critical next step for the user.\\nRules:\\n1. Find X and Y as percentages (0-100).\\n2. Give a short, helpful guidance tip in BENGALI (max 5-7 words).\\n3. Be specific: \\\"নীল বাটনটিতে ক্লিক করুন\\\", \\\"ব্যাকে যান\\\", \\\"সার্চ বারে লিখুন\\\".\\nOutput MUST be strictly valid JSON:\\n{\\n    \\\"x_p\\\": float,\\n    \\\"y_p\\\": float,\\n    \\\"tip\\\": \\\"Short Bengali Instruction\\\"\\n}";

            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"},{\"inline_data\":{\"mime_type\":\"image/jpeg\",\"data\":\"" + base64Image + "\"}}]}],\"generationConfig\":{\"response_mime_type\":\"application/json\"}}";

            OutputStream os = conn.getOutputStream();
            os.write(jsonPayload.getBytes("UTF-8"));
            os.close();

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                is.close();
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void parseAndMakeOverlay(Context context, String apiResponse) {
        try {
            JSONObject response = new JSONObject(apiResponse);
            String rawText = response.getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts")
                            .getJSONObject(0).getString("text");
            
            rawText = rawText.replace("```json", "").replace("```", "").trim();
            JSONObject output = new JSONObject(rawText);
            
            float xp = (float) output.getDouble("x_p");
            float yp = (float) output.getDouble("y_p");
            String tip = output.getString("tip");
            
            showGuidance(context, xp, yp, tip);
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
                        border.setStroke(14, Color.RED);
                        redBox.setBackground(border);
                        
                        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(180, 180);
                        floatingView.addView(redBox, boxParams);

                        windowManager.addView(floatingView, params);
                    }

                    int cx = (int) ((x_p / 100.0) * mWidth);
                    int cy = (int) ((y_p / 100.0) * mHeight);
                    
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
                    }, 2800);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
