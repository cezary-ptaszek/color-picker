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
import java.util.List;

import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

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
        formatRGBcolor = getString(R.string.rgb_color_format);
    }

    private void initCamera(int width, int height) {
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


    private void displayColor(int color) {
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);
        colorStr = String.format(formatRGBcolor, red, green, blue) + " color";

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


        mButtonSpeak.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                colorStr2 = colorStr;
                                                mTTS.speak(colorStr2, TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                        }
        );



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

    private Camera.Size size;
    private YuvImage image;
    private ByteArrayOutputStream stream;
    private Bitmap bmp;
    private String colorStr;
    private String colorStr2;

    private String formatRGBcolor;


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

}
