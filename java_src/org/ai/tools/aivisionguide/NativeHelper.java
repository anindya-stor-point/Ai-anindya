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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Base64;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;

import android.view.MotionEvent;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class NativeHelper {
    public static MediaProjectionManager mpm;
    public static MediaProjection mediaProjection;
    public static VirtualDisplay virtualDisplay;
    public static ImageReader imageReader;
    public static int mWidth;
    public static int mHeight;
    public static int mScreenDensity;

    public static WindowManager windowManager;
    public static FrameLayout floatingView;     // Red Box overlay
    public static LinearLayout floatingBubble;  // Menu Bubble (Mic + Stop)
    public static TextView tipText;
    public static View redBox;
    public static View scanLine;
    public static TextView statusBadge;

    public static TextToSpeech tts;
    public static Vibrator vibrator;
    
    public static volatile boolean isLooping = false;
    public static volatile boolean isProcessing = false;
    
    // Voice Command Variables
    public static SpeechRecognizer speechRecognizer;
    public static boolean isListening = false;
    public static String currentVoiceCommand = "";
    public static View micButtonView;
    public static Intent staticDataIntent;
    
    public static void requestCapture(Activity activity, int requestCode) {
        if (mpm == null) {
            mpm = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
        if (mpm != null) {
            Intent intent = mpm.createScreenCaptureIntent();
            activity.startActivityForResult(intent, requestCode);
        }
    }

    // Displays detailed errors directly to the user as a Toast
    public static void showDetailedError(Context context, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "AI Error: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    public static void startContinuousAnalysis(Context context, String apiKey, Intent staticDataIntent, int screenWidth, int screenHeight, int densityDpi) {
        if (isLooping) return;
        isLooping = true;
        
        mWidth = screenWidth;
        mHeight = screenHeight;
        mScreenDensity = densityDpi;

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("bn", "BD"));
            }
        });

        if (mpm == null) {
            mpm = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        try {
            mediaProjection = mpm.getMediaProjection(-1, staticDataIntent);
        } catch (Exception e) {
            showDetailedError(context, "Screen Capture Access Denied: " + e.getMessage());
            return;
        }
        
        showFloatingBubble(context);
        
        // Scale down to 540p to make memory copy and HTTP transit ULTRA FAST (~70% faster than 720p)
        int captureWidth = 540;
        int captureHeight = (int)((540.0 / mWidth) * mHeight);
        
        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2);
        
        virtualDisplay = mediaProjection.createVirtualDisplay("AIVisionGuide",
                captureWidth, captureHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        Thread analysisThread = new Thread(() -> {
            while (isLooping) {
                try {
                    // Poll very quickly. Image extraction is lightweight.
                    Thread.sleep(1800); 
                    
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
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos); // Fast compression
                    byte[] jpegData = bos.toByteArray();
                    bitmap.recycle();
                    croppedBitmap.recycle();

                    String base64Image = Base64.encodeToString(jpegData, Base64.NO_WRAP);
                    
                    // Consume Voice Command if present
                    String queryToProcess = "";
                    if (!currentVoiceCommand.isEmpty()) {
                        queryToProcess = currentVoiceCommand;
                        currentVoiceCommand = ""; // Clear after picking up
                    }

                    // Async Fast HTTP Post
                    isProcessing = true;
                    updateHUDStatus(true);
                    
                    String result = analyzeWithGemini(context, apiKey, base64Image, queryToProcess);
                    
                    isProcessing = false;
                    updateHUDStatus(false);
                    
                    if (result != null && isLooping) {
                        parseAndMakeOverlay(context, result);
                    }
                } catch (Exception e) {
                    showDetailedError(context, "Loop Error: " + e.getMessage());
                }
            }
        });
        analysisThread.start();
    }

    public static void stopContinuousAnalysis(Context context) {
        isLooping = false;
        
        // Stop the native Background Service
        try {
            Intent serviceIntent = new Intent(context, ScreenCaptureService.class);
            context.stopService(serviceIntent);
        } catch (Exception e) {}
        
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
                if (windowManager != null && floatingBubble != null) {
                    windowManager.removeView(floatingBubble);
                    floatingBubble = null;
                }
                if (windowManager != null && floatingView != null) {
                    windowManager.removeView(floatingView);
                    floatingView = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    private static void updateMicUI(boolean listening) {
        if (micButtonView != null) {
            GradientDrawable bg = (GradientDrawable) micButtonView.getBackground();
            bg.setColor(listening ? Color.parseColor("#F44336") : Color.parseColor("#2196F3")); // Red if listening, Blue if idle
        }
    }

    private static void toggleSpeechRecognition(Context context) {
        if (isListening) {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }
            isListening = false;
            updateMicUI(false);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            showDetailedError(context, "Voice recognition not supported on this device.");
            return;
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {
                    isListening = false;
                    updateMicUI(false);
                }
                @Override public void onError(int error) {
                    isListening = false;
                    updateMicUI(false);
                    showDetailedError(context, "Mic Error Code: " + error);
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        currentVoiceCommand = matches.get(0);
                        Toast.makeText(context, "Heard: " + currentVoiceCommand, Toast.LENGTH_SHORT).show();
                    }
                    isListening = false;
                    updateMicUI(false);
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD"); // Focus on Bengali
            speechRecognizer.startListening(intent);
            isListening = true;
            updateMicUI(true);
        } catch (Exception e) {
            showDetailedError(context, "Mic start failed: " + e.getMessage());
        }
    }

    private static void updateHUDStatus(boolean active) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (statusBadge != null) {
                statusBadge.setText(active ? "AI OBSERVING..." : "AI IDLE");
                statusBadge.setTextColor(active ? Color.parseColor("#00FF00") : Color.parseColor("#88FFFFFF"));
                if (scanLine != null) {
                    scanLine.setVisibility(active ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private static void showFloatingBubble(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (windowManager == null) {
                    windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                }

                // If already exists, remove it first to avoid "view already added" error
                if (floatingBubble != null) {
                    try {
                        windowManager.removeView(floatingBubble);
                    } catch (Exception e) {}
                    floatingBubble = null;
                }
                
                int type = android.os.Build.VERSION.SDK_INT >= 26 ? 
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                        WindowManager.LayoutParams.TYPE_PHONE;
                
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                );
                params.gravity = Gravity.TOP | Gravity.RIGHT;
                params.y = 200; // Position it slightly down from the top

                floatingBubble = new LinearLayout(context);
                floatingBubble.setOrientation(LinearLayout.VERTICAL);
                floatingBubble.setPadding(10, 10, 10, 10);
                // Semi-transparent dark background for the whole bubble container
                GradientDrawable bubbleBg = new GradientDrawable();
                bubbleBg.setColor(Color.parseColor("#44000000"));
                bubbleBg.setCornerRadius(20);
                floatingBubble.setBackground(bubbleBg);
                
                // Status Badge
                statusBadge = new TextView(context);
                statusBadge.setText("AI IDLE");
                statusBadge.setTextSize(9);
                statusBadge.setGravity(Gravity.CENTER);
                statusBadge.setTextColor(Color.parseColor("#88FFFFFF"));
                statusBadge.setPadding(10, 5, 10, 5);
                
                floatingBubble.addView(statusBadge, new LinearLayout.LayoutParams(-1, -2));

                // MIC BUTTON
                FrameLayout micLayout = new FrameLayout(context);
                micButtonView = new View(context);
                GradientDrawable micBg = new GradientDrawable();
                micBg.setShape(GradientDrawable.OVAL);
                micBg.setColor(Color.parseColor("#2196F3"));
                micBg.setStroke(4, Color.WHITE);
                micButtonView.setBackground(micBg);
                
                TextView micTxt = new TextView(context);
                micTxt.setText("MIC");
                micTxt.setTextColor(Color.WHITE);
                micTxt.setTextSize(12);
                micTxt.setGravity(Gravity.CENTER);
                
                micLayout.addView(micButtonView, new FrameLayout.LayoutParams(120, 120));
                micLayout.addView(micTxt, new FrameLayout.LayoutParams(120, 120));
                micLayout.setOnClickListener(v -> toggleSpeechRecognition(context));

                // STOP BUTTON
                FrameLayout stopLayout = new FrameLayout(context);
                View stopCircle = new View(context);
                GradientDrawable stopBg = new GradientDrawable();
                stopBg.setShape(GradientDrawable.OVAL);
                stopBg.setColor(Color.parseColor("#F44336")); // Red Stop
                stopBg.setStroke(4, Color.WHITE);
                stopCircle.setBackground(stopBg);
                
                TextView stopTxt = new TextView(context);
                stopTxt.setText("OFF");
                stopTxt.setTextColor(Color.WHITE);
                stopTxt.setTextSize(12);
                stopTxt.setGravity(Gravity.CENTER);

                stopLayout.addView(stopCircle, new FrameLayout.LayoutParams(120, 120));
                stopLayout.addView(stopTxt, new FrameLayout.LayoutParams(120, 120));
                stopLayout.setOnClickListener(v -> {
                    stopContinuousAnalysis(context);
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(launchIntent);
                    }
                });
                
                LinearLayout.LayoutParams lpItems = new LinearLayout.LayoutParams(120, 120);
                lpItems.setMargins(0, 10, 0, 10);
                lpItems.gravity = Gravity.CENTER_HORIZONTAL;
                
                floatingBubble.addView(micLayout, lpItems);
                floatingBubble.addView(stopLayout, lpItems);
                
                windowManager.addView(floatingBubble, params);
                
                // DRAGGABLE LOGIC
                floatingBubble.setOnTouchListener(new View.OnTouchListener() {
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialX = params.x;
                                initialY = params.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                                windowManager.updateViewLayout(floatingBubble, params);
                                return true;
                        }
                        return false;
                    }
                });

                Toast.makeText(context, "AI Vision Bubble Active", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                showDetailedError(context, "Bubble Render Fail: " + e.getMessage());
            }
        });
    }

    private static String analyzeWithGemini(Context context, String apiKey, String base64Image, String userQuery) {
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);

            String prompt = "Role: Advanced Android OS Vision AI. You are helping a user navigate their phone screen in real-time.\\n";
            if (!userQuery.isEmpty()) {
                prompt += "User Voice Intent: \\\"" + userQuery + "\\\". Focus all attention on fulfilling this specific request.\\n";
            } else {
                prompt += "Task: Contextual proactive guidance. Observe the current UI and propose the single most logical next action to help the user.\\n";
            }
            prompt += "Rules:\\n1. Precise Coordinates: Output X and Y as percentages (0-100) of the center of the UI element to interact with.\\n2. Advanced Bengali Support: Instruction must be short, helpful, and written in natural Bengali script.\\n3. No Yapping: Output ONLY raw JSON based on this schema:\\n{\\n  \\\"x_p\\\": float (0.0-100.0),\\n  \\\"y_p\\\": float (0.0-100.0),\\n  \\\"tip\\\": \\\"Bengali Instruction String\\\"\\n}";

            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"},{\"inline_data\":{\"mime_type\":\"image/jpeg\",\"data\":\"" + base64Image + "\"}}]}],\"generationConfig\":{\"response_mime_type\":\"application/json\", \"temperature\": 0.1}}";

            OutputStream os = conn.getOutputStream();
            os.write(jsonPayload.getBytes("UTF-8"));
            os.close();

            int code = conn.getResponseCode();
            if (code == 200) {
                InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                is.close();
                return response;
            } else {
                InputStream err = conn.getErrorStream();
                if (err != null) {
                    java.util.Scanner s = new java.util.Scanner(err).useDelimiter("\\A");
                    String errDesc = s.hasNext() ? s.next() : "Unknown API Exception";
                    showDetailedError(context, "API " + code + ": " + errDesc);
                } else {
                    showDetailedError(context, "API Request Failed with Code: " + code);
                }
            }
        } catch (Exception e) {
            showDetailedError(context, "Network Failure: " + e.getMessage());
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
            showDetailedError(context, "AI JSON Parse Error: Missing key or invalid format -> " + e.getMessage());
        }
    }

    public static void showGuidance(Context context, float x_p, float y_p, String tip) {
        new Handler(Looper.getMainLooper()).post(() -> {
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

                    scanLine = new View(context);
                    scanLine.setBackgroundColor(Color.parseColor("#4400FF00"));
                    scanLine.setVisibility(View.GONE);
                    floatingView.addView(scanLine, new FrameLayout.LayoutParams(-1, 10));

                    tipText = new TextView(context);
                    tipText.setTextColor(Color.WHITE);
                    tipText.setBackground(createRoundedBg(Color.argb(230, 10, 10, 10), 40));
                    tipText.setTextSize(20);
                    tipText.setGravity(Gravity.CENTER);
                    tipText.setPadding(60, 40, 60, 40);
                    FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    textParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    textParams.bottomMargin = 240;
                    floatingView.addView(tipText, textParams);

                    redBox = new View(context);
                    GradientDrawable border = new GradientDrawable();
                    border.setColor(Color.argb(30, 255, 0, 0));
                    border.setStroke(12, Color.RED);
                    border.setCornerRadius(30);
                    redBox.setBackground(border);
                    
                    FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(220, 220);
                    floatingView.addView(redBox, boxParams);

                    windowManager.addView(floatingView, params);
                }

                // Update Coordinates
                int cx = (int) ((x_p / 100.0) * mWidth);
                int cy = (int) ((y_p / 100.0) * mHeight);
                
                FrameLayout.LayoutParams boxParams = (FrameLayout.LayoutParams) redBox.getLayoutParams();
                boxParams.leftMargin = cx - 110; 
                boxParams.topMargin = cy - 110;
                redBox.setLayoutParams(boxParams);

                // Pulsing Animation for the Box
                redBox.setScaleX(0.8f); redBox.setScaleY(0.8f);
                redBox.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).withEndAction(() -> {
                    redBox.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).start();
                }).start();

                tipText.setText(tip);
                floatingView.setVisibility(View.VISIBLE);
                
                // Haptic Feedback & Voice Guidance
                if (vibrator != null) vibrator.vibrate(150);
                if (tts != null) tts.speak(tip, TextToSpeech.QUEUE_FLUSH, null, null);

                // Hide automatically after 3.5 seconds to give user time to read
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (floatingView != null) {
                        floatingView.setVisibility(View.GONE);
                    }
                }, 3500);
            } catch (Exception e) {
                showDetailedError(context, "Overlay Rendering Error: " + e.getMessage());
            }
        });
    }

    private static GradientDrawable createRoundedBg(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }
}
