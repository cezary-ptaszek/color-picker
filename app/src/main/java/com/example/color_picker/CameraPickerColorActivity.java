package com.example.color_picker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;


import java.util.Locale;
public class CameraPickerColorActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnClickListener {
    private SurfaceView mSurfaceView;
    private TextView mTv_color;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private RoundPickerColorView mColorDisplay;
    private int color;
    private int mCenterX, mCenterY;
    private TextToSpeech mTTS;
    private Button mButtonSpeak;
    private Camera.Size size;
    private YuvImage image;
    private ByteArrayOutputStream stream;
    private Bitmap bmp;
    private String colorStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camer_picker_color);

        mButtonSpeak = findViewById(R.id.bt_apply);

        //Pasek stanu
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        mSurfaceView = findViewById(R.id.surfaceview);
        mTv_color =  findViewById(R.id.tv_color);
        mColorDisplay = findViewById(R.id.view);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    private void initCamera(int width, int height) {

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.ENGLISH);
                    mButtonSpeak.setEnabled(true);

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
        float previewRate2 = (height/ (float)width);


        //Ustawienie wielkości obrazka
        Camera.Size previewSize = CameraPreviewUtil.getInstance().getPreviewSize(previewSizes, previewRate2, 800);
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        size = camera.getParameters().getPreviewSize();
        try {
            image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if (image != null) {
                stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                color = getColor(bmp, mCenterX, mCenterY);
                displayColor(color);
                stream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void displayColor(final int color) {
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);

        //rozpoznawanie
        colorStr = rgbToColorString(red, green, blue);

        mButtonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTTS.speak(colorStr, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        if (!TextUtils.isEmpty(colorStr))
            mTv_color.setText(colorStr);
        mColorDisplay.setColor(color);
    }


    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }


    private int getColor(Bitmap source, int intX, int intY) {
        int color;
        if (intX < 0) intX = 0;
        if (intY < 0) intY = 0;
        if (intX >= source.getWidth()) {
            intX = source.getWidth() - 1;
        }
        if (intY >= source.getHeight()) {
            intY = source.getHeight() - 1;
        }

        color = source.getPixel(intX, intY);

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

    public static float getScreenRate(Context context){
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float H = dm.heightPixels;
        float W = dm.widthPixels;

        return (H/W);
    }

    public String rgbToColorString(int red, int green, int blue){
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

    private ArrayList<ColorName> initColorList(){
        ArrayList<ColorName> colorList = new ArrayList<ColorName>();
        colorList.add(new ColorName("Alice Blue", 0xF0, 0xF8, 0xFF));
        colorList.add(new ColorName("Antique White", 0xFA, 0xEB, 0xD7));
        colorList.add(new ColorName("Aqua", 0x00, 0xFF, 0xFF));
        colorList.add(new ColorName("Aquamarine", 0x7F, 0xFF, 0xD4));
        colorList.add(new ColorName("Azure", 0xF0, 0xFF, 0xFF));
        colorList.add(new ColorName("Beige", 0xF5, 0xF5, 0xDC));
        colorList.add(new ColorName("Bisque", 0xFF, 0xE4, 0xC4));
        colorList.add(new ColorName("Black", 0x00, 0x00, 0x00));
        colorList.add(new ColorName("Blanched Almond", 0xFF, 0xEB, 0xCD));
        colorList.add(new ColorName("Blue", 0x00, 0x00, 0xFF));
        colorList.add(new ColorName("Blue Violet", 0x8A, 0x2B, 0xE2));
        colorList.add(new ColorName("Brown", 0xA5, 0x2A, 0x2A));
        colorList.add(new ColorName("Burly Wood", 0xDE, 0xB8, 0x87));
        colorList.add(new ColorName("Cadet Blue", 0x5F, 0x9E, 0xA0));
        colorList.add(new ColorName("Chartreuse", 0x7F, 0xFF, 0x00));
        colorList.add(new ColorName("Chocolate", 0xD2, 0x69, 0x1E));
        colorList.add(new ColorName("Coral", 0xFF, 0x7F, 0x50));
        colorList.add(new ColorName("Cornflower Blue", 0x64, 0x95, 0xED));
        colorList.add(new ColorName("Cornsilk", 0xFF, 0xF8, 0xDC));
        colorList.add(new ColorName("Crimson", 0xDC, 0x14, 0x3C));
        colorList.add(new ColorName("Cyan", 0x00, 0xFF, 0xFF));
        colorList.add(new ColorName("Dark Blue", 0x00, 0x00, 0x8B));
        colorList.add(new ColorName("Dark Cyan", 0x00, 0x8B, 0x8B));
        colorList.add(new ColorName("Dark Golden Rod", 0xB8, 0x86, 0x0B));
        colorList.add(new ColorName("Dark Gray", 0xA9, 0xA9, 0xA9));
        colorList.add(new ColorName("Dark Green", 0x00, 0x64, 0x00));
        colorList.add(new ColorName("Dark Khaki", 0xBD, 0xB7, 0x6B));
        colorList.add(new ColorName("Dark Magenta", 0x8B, 0x00, 0x8B));
        colorList.add(new ColorName("Dark Olive Green", 0x55, 0x6B, 0x2F));
        colorList.add(new ColorName("Dark Orange", 0xFF, 0x8C, 0x00));
        colorList.add(new ColorName("Dark Orchid", 0x99, 0x32, 0xCC));
        colorList.add(new ColorName("Dark Red", 0x8B, 0x00, 0x00));
        colorList.add(new ColorName("Dark Salmon", 0xE9, 0x96, 0x7A));
        colorList.add(new ColorName("Dark Sea Green", 0x8F, 0xBC, 0x8F));
        colorList.add(new ColorName("Dark Slate Blue", 0x48, 0x3D, 0x8B));
        colorList.add(new ColorName("Dark Slate Gray", 0x2F, 0x4F, 0x4F));
        colorList.add(new ColorName("Dark Turquoise", 0x00, 0xCE, 0xD1));
        colorList.add(new ColorName("Dark Violet", 0x94, 0x00, 0xD3));
        colorList.add(new ColorName("Deep Pink", 0xFF, 0x14, 0x93));
        colorList.add(new ColorName("Deep Sky Blue", 0x00, 0xBF, 0xFF));
        colorList.add(new ColorName("Dim Gray", 0x69, 0x69, 0x69));
        colorList.add(new ColorName("Dodger Blue", 0x1E, 0x90, 0xFF));
        colorList.add(new ColorName("Fire Brick", 0xB2, 0x22, 0x22));
        colorList.add(new ColorName("Floral White", 0xFF, 0xFA, 0xF0));
        colorList.add(new ColorName("Forest Green", 0x22, 0x8B, 0x22));
        colorList.add(new ColorName("Fuchsia", 0xFF, 0x00, 0xFF));
        colorList.add(new ColorName("Gainsboro", 0xDC, 0xDC, 0xDC));
        colorList.add(new ColorName("Ghost White", 0xF8, 0xF8, 0xFF));
        colorList.add(new ColorName("Gold", 0xFF, 0xD7, 0x00));
        colorList.add(new ColorName("Golden Rod", 0xDA, 0xA5, 0x20));
        colorList.add(new ColorName("Gray", 0x80, 0x80, 0x80));
        colorList.add(new ColorName("Green", 0x00, 0x80, 0x00));
        colorList.add(new ColorName("Green Yellow", 0xAD, 0xFF, 0x2F));
        colorList.add(new ColorName("Honey Dew", 0xF0, 0xFF, 0xF0));
        colorList.add(new ColorName("Hot Pink", 0xFF, 0x69, 0xB4));
        colorList.add(new ColorName("Indian Red", 0xCD, 0x5C, 0x5C));
        colorList.add(new ColorName("Indigo", 0x4B, 0x00, 0x82));
        colorList.add(new ColorName("Ivory", 0xFF, 0xFF, 0xF0));
        colorList.add(new ColorName("Khaki", 0xF0, 0xE6, 0x8C));
        colorList.add(new ColorName("Lavender", 0xE6, 0xE6, 0xFA));
        colorList.add(new ColorName("Lavender Blush", 0xFF, 0xF0, 0xF5));
        colorList.add(new ColorName("Lawn Green", 0x7C, 0xFC, 0x00));
        colorList.add(new ColorName("Lemon Chiffon", 0xFF, 0xFA, 0xCD));
        colorList.add(new ColorName("Light Blue", 0xAD, 0xD8, 0xE6));
        colorList.add(new ColorName("Light Coral", 0xF0, 0x80, 0x80));
        colorList.add(new ColorName("Light Cyan", 0xE0, 0xFF, 0xFF));
        colorList.add(new ColorName("Light Golden Rod Yellow", 0xFA, 0xFA, 0xD2));
        colorList.add(new ColorName("Light Gray", 0xD3, 0xD3, 0xD3));
        colorList.add(new ColorName("Light Green", 0x90, 0xEE, 0x90));
        colorList.add(new ColorName("Light Pink", 0xFF, 0xB6, 0xC1));
        colorList.add(new ColorName("Light Salmon", 0xFF, 0xA0, 0x7A));
        colorList.add(new ColorName("Light Sea Green", 0x20, 0xB2, 0xAA));
        colorList.add(new ColorName("Light Sky Blue", 0x87, 0xCE, 0xFA));
        colorList.add(new ColorName("Light Slate Gray", 0x77, 0x88, 0x99));
        colorList.add(new ColorName("Light Steel Blue", 0xB0, 0xC4, 0xDE));
        colorList.add(new ColorName("Light Yellow", 0xFF, 0xFF, 0xE0));
        colorList.add(new ColorName("Lime", 0x00, 0xFF, 0x00));
        colorList.add(new ColorName("Lime Green", 0x32, 0xCD, 0x32));
        colorList.add(new ColorName("Linen", 0xFA, 0xF0, 0xE6));
        colorList.add(new ColorName("Magenta", 0xFF, 0x00, 0xFF));
        colorList.add(new ColorName("Maroon", 0x80, 0x00, 0x00));
        colorList.add(new ColorName("Medium Aqua Marine", 0x66, 0xCD, 0xAA));
        colorList.add(new ColorName("Medium Blue", 0x00, 0x00, 0xCD));
        colorList.add(new ColorName("Medium Orchid", 0xBA, 0x55, 0xD3));
        colorList.add(new ColorName("Medium Purple", 0x93, 0x70, 0xDB));
        colorList.add(new ColorName("Medium Sea Green", 0x3C, 0xB3, 0x71));
        colorList.add(new ColorName("Medium Slate Blue", 0x7B, 0x68, 0xEE));
        colorList.add(new ColorName("Medium Spring Green", 0x00, 0xFA, 0x9A));
        colorList.add(new ColorName("Medium Turquoise", 0x48, 0xD1, 0xCC));
        colorList.add(new ColorName("Medium Violet Red", 0xC7, 0x15, 0x85));
        colorList.add(new ColorName("Midnight Blue", 0x19, 0x19, 0x70));
        colorList.add(new ColorName("Mint Cream", 0xF5, 0xFF, 0xFA));
        colorList.add(new ColorName("Misty Rose", 0xFF, 0xE4, 0xE1));
        colorList.add(new ColorName("Moccasin", 0xFF, 0xE4, 0xB5));
        colorList.add(new ColorName("Navajo White", 0xFF, 0xDE, 0xAD));
        colorList.add(new ColorName("Navy", 0x00, 0x00, 0x80));
        colorList.add(new ColorName("Old Lace", 0xFD, 0xF5, 0xE6));
        colorList.add(new ColorName("Olive", 0x80, 0x80, 0x00));
        colorList.add(new ColorName("Olive Drab", 0x6B, 0x8E, 0x23));
        colorList.add(new ColorName("Orange", 0xFF, 0xA5, 0x00));
        colorList.add(new ColorName("Orange Red", 0xFF, 0x45, 0x00));
        colorList.add(new ColorName("Orchid", 0xDA, 0x70, 0xD6));
        colorList.add(new ColorName("Pale Golden Rod", 0xEE, 0xE8, 0xAA));
        colorList.add(new ColorName("Pale Green", 0x98, 0xFB, 0x98));
        colorList.add(new ColorName("Pale Turquoise", 0xAF, 0xEE, 0xEE));
        colorList.add(new ColorName("Pale Violet Red", 0xDB, 0x70, 0x93));
        colorList.add(new ColorName("Papaya Whip", 0xFF, 0xEF, 0xD5));
        colorList.add(new ColorName("Peach Puff", 0xFF, 0xDA, 0xB9));
        colorList.add(new ColorName("Peru", 0xCD, 0x85, 0x3F));
        colorList.add(new ColorName("Pink", 0xFF, 0xC0, 0xCB));
        colorList.add(new ColorName("Plum", 0xDD, 0xA0, 0xDD));
        colorList.add(new ColorName("Powder Blue", 0xB0, 0xE0, 0xE6));
        colorList.add(new ColorName("Purple", 0x80, 0x00, 0x80));
        colorList.add(new ColorName("Red", 0xFF, 0x00, 0x00));
        colorList.add(new ColorName("Rosy Brown", 0xBC, 0x8F, 0x8F));
        colorList.add(new ColorName("Royal Blue", 0x41, 0x69, 0xE1));
        colorList.add(new ColorName("Saddle Brown", 0x8B, 0x45, 0x13));
        colorList.add(new ColorName("Salmon", 0xFA, 0x80, 0x72));
        colorList.add(new ColorName("Sandy Brown", 0xF4, 0xA4, 0x60));
        colorList.add(new ColorName("Sea Green", 0x2E, 0x8B, 0x57));
        colorList.add(new ColorName("Sea Shell", 0xFF, 0xF5, 0xEE));
        colorList.add(new ColorName("Sienna", 0xA0, 0x52, 0x2D));
        colorList.add(new ColorName("Silver", 0xC0, 0xC0, 0xC0));
        colorList.add(new ColorName("Sky Blue", 0x87, 0xCE, 0xEB));
        colorList.add(new ColorName("Slate Blue", 0x6A, 0x5A, 0xCD));
        colorList.add(new ColorName("Slate Gray", 0x70, 0x80, 0x90));
        colorList.add(new ColorName("Snow", 0xFF, 0xFA, 0xFA));
        colorList.add(new ColorName("Spring Green", 0x00, 0xFF, 0x7F));
        colorList.add(new ColorName("Steel Blue", 0x46, 0x82, 0xB4));
        colorList.add(new ColorName("Tan", 0xD2, 0xB4, 0x8C));
        colorList.add(new ColorName("Teal", 0x00, 0x80, 0x80));
        colorList.add(new ColorName("Thistle", 0xD8, 0xBF, 0xD8));
        colorList.add(new ColorName("Tomato", 0xFF, 0x63, 0x47));
        colorList.add(new ColorName("Turquoise", 0x40, 0xE0, 0xD0));
        colorList.add(new ColorName("Violet", 0xEE, 0x82, 0xEE));
        colorList.add(new ColorName("Wheat", 0xF5, 0xDE, 0xB3));
        colorList.add(new ColorName("White", 0xFF, 0xFF, 0xFF));
        colorList.add(new ColorName("White Smoke", 0xF5, 0xF5, 0xF5));
        colorList.add(new ColorName("Yellow", 0xFF, 0xFF, 0x00));
        colorList.add(new ColorName("Yellow Green", 0x9A, 0xCD, 0x32));
        return colorList;
    }
}
