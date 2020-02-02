package com.example.color_picker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CameraPickerColorActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnClickListener {
    private TextView colorNameText;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private RoundPickerColorView roundPicker;
    private int mCenterX, mCenterY;
    private TextToSpeech mTTS;
    private Button speakButton;
    private String colorStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camer_picker_color);
        speakButton = findViewById(R.id.bt_apply);

        //Pasek stanu
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        SurfaceView mSurfaceView = findViewById(R.id.surfaceview);
        colorNameText = findViewById(R.id.tv_color);
        roundPicker = findViewById(R.id.view);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    private void initCamera(int width, int height) {

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale loc = new Locale("pol", "PL");
                    int result = mTTS.setLanguage(loc);
                    speakButton.setEnabled(true);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                    }
                }
            }
        });

        //parametry instancji kamery
        Camera.Parameters parameters = camera.getParameters();

        //pobranie obslugiwanych rozmiarów
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        float previewRate = getScreenRate(this);
//        float previewRate = (height / (float) width);

        //Ustawienie wielkości obrazka
        Camera.Size previewSize = CameraPreviewUtil.getInstance().getPreviewSize(previewSizes, previewRate, 800);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        mCenterX = previewSize.width / 2;
        mCenterY = previewSize.height / 2;

        camera.setParameters(parameters);
        camera.startPreview();
        camera.setDisplayOrientation(90);
    }

    @Override
    public void onClick(View v) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            int[] color = getColor(bmp, mCenterX, mCenterY);
            displayColor(color);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void displayColor(final int[] color) {
        int red = (color[0] & 0xff0000) >> 16;
        int green = (color[0] & 0x00ff00) >> 8;
        int blue = (color[0] & 0x0000ff);

        int redNext = (color[1] & 0xff0000) >> 16;
        int greenNext = (color[1] & 0x00ff00) >> 8;
        int blueNext = (color[1] & 0x0000ff);

        //rozpoznawanie
        colorStr = rgbToColorString(red, green, blue);
        String nextColorStr = rgbToColorString(redNext, greenNext, blueNext);

        System.out.println("KOLOR: " + nextColorStr);

        if(!colorStr.equals(nextColorStr)){
            colorStr = doubleColor(colorStr, nextColorStr);
        }

        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTTS.speak("Kolor " + colorStr, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        if (!TextUtils.isEmpty(colorStr)) {
            colorNameText.setText(colorStr);
        }
        roundPicker.setColor(color[0]);
    }


    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }


    private int[] getColor(Bitmap source, int intX, int intY) {
        int[] color = new int[2];
        if (intX < 0) intX = 0;
        if (intY < 0) intY = 0;
        if (intX >= source.getWidth()) {
            intX = source.getWidth() - 1;
        }
        if (intY >= source.getHeight()) {
            intY = source.getHeight() - 1;
        }

        color[0] = source.getPixel(intX, intY);
        intX-=20;
        color[1] = source.getPixel(intX, intY);

        source.recycle();
        return color;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open();
            camera.setPreviewCallback(this);
            camera.setPreviewDisplay(mSurfaceHolder);
        } catch (Exception e) {
            if (null != camera) {
                camera.release();
                camera = null;
            }
            e.printStackTrace();
            Toast.makeText(CameraPickerColorActivity.this, "Brak uprawnien kamery", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        initCamera(width, height);
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != camera) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public static float getScreenRate(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float H = dm.heightPixels;
        float W = dm.widthPixels;

        return (H / W);
    }

    public String doubleColor(String s1, String s2){
        String finalColor = s1.replace(s1.substring(s1.length()-1), "");
        s2 = Character.toLowerCase(s2.charAt(0)) + s2.substring(1);
        finalColor+="o" + s2;
        return finalColor;
    }

    public String rgbToColorString(int red, int green, int blue) {
        ArrayList<ColorName> colorList = initColorList();
        ColorName closestMatch = null;
        int minMSE = Integer.MAX_VALUE;
        int mse;
        for (ColorName c : colorList) {
            mse = c.computeMSE(red, green, blue);
            if (mse < minMSE) {
                minMSE = mse;
                closestMatch = c;
            }
        }

        if (closestMatch != null) {
            return closestMatch.getName();
        } else {
            return "No matched color name.";
        }
    }

    private ArrayList<ColorName> initColorList() {

        ArrayList<ColorName> colorList = new ArrayList<>();
        colorList.add(new ColorName("Czarny", 0x00, 0x00, 0x00));
        colorList.add(new ColorName("Biały", 0xFF, 0xFF, 0xFF));
        colorList.add(new ColorName("Szary", 0x80, 0x80, 0x80));
        colorList.add(new ColorName("Czerwony", 0xFF, 0x00, 0x00));
        colorList.add(new ColorName("Brązowy", 0x80, 0x00, 0x00));
        colorList.add(new ColorName("Żółty", 0xFF, 0xFF, 0x00));
        colorList.add(new ColorName("Jasnozielony", 0x00, 0xFF, 0x00));
        colorList.add(new ColorName("Zielony", 0x00, 0x80, 0x00));
        colorList.add(new ColorName("Jasnoniebieski", 0x00, 0xFF, 0xFF));
        colorList.add(new ColorName("Niebieski", 0x00, 0x00, 0xFF));
        colorList.add(new ColorName("Fioletowy", 0x6A, 0x0D, 0xAD));
        colorList.add(new ColorName("Pomarańczowy", 0xFF, 0xA5, 0x00));

        return colorList;
    }
}
