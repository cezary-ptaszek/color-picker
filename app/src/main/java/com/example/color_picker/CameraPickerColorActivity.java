package com.example.color_picker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
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

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CameraPickerColorActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnClickListener {
    private static final double SECONDS_LAST_RECOGNIZE = 1;
    private TextView colorNameText;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private RoundPickerColorView roundPicker;
    private int mCenterX, mCenterY;
    private TextToSpeech mTTS;
    private Button speakButton;
    private String colorStr;
    private long lastRecognizeTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camer_picker_color);
        speakButton = findViewById(R.id.bt_apply);

        lastRecognizeTimestamp = System.currentTimeMillis() / 1000;

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
        float previewRate2 = (height / (float) width);


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
        Camera.Size size = camera.getParameters().getPreviewSize();
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if (image != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                int color = getColor(bmp, mCenterX, mCenterY);
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
        final String oldColorStr = colorStr;
        colorStr = rgbToColorString(red, green, blue);

        long currentTimestamp = System.currentTimeMillis() / 1000;
        final long lastRecognizeDiff = currentTimestamp - lastRecognizeTimestamp;
        lastRecognizeTimestamp = currentTimestamp;

        String textToSpeech = colorStr;
        if ((lastRecognizeDiff) <= SECONDS_LAST_RECOGNIZE * 1000 && !oldColorStr.equals(colorStr)) {
            textToSpeech = colorStr + " i " + oldColorStr;
        }

        final String textToSpeechAsFinal = textToSpeech;
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTTS.speak("Kolor " + textToSpeechAsFinal, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        if (!TextUtils.isEmpty(textToSpeech)) {
            colorNameText.setText(textToSpeech);
        }
        roundPicker.setColor(color);
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

    public static float getScreenRate(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float H = dm.heightPixels;
        float W = dm.widthPixels;

        return (H / W);
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
        ArrayList<ColorName> colorList = new ArrayList<ColorName>();
        colorList.add(new ColorName("Niebieski lód", 0xF0, 0xF8, 0xFF));
        colorList.add(new ColorName("Antyczna biel", 0xFA, 0xEB, 0xD7));
        colorList.add(new ColorName("Wodny", 0x00, 0xFF, 0xFF));
        colorList.add(new ColorName("Akwamaryn", 0x7F, 0xFF, 0xD4));
        colorList.add(new ColorName("Lazur", 0xF0, 0xFF, 0xFF));
        colorList.add(new ColorName("Beżowy", 0xF5, 0xF5, 0xDC));
        colorList.add(new ColorName("Jasny beż", 0xFF, 0xE4, 0xC4));
        colorList.add(new ColorName("Czarny", 0x00, 0x00, 0x00));
        colorList.add(new ColorName("Orzechowy", 0xFF, 0xEB, 0xCD));
        colorList.add(new ColorName("Niebieski", 0x00, 0x00, 0xFF));
        colorList.add(new ColorName("Niebiesko fioletowy", 0x8A, 0x2B, 0xE2));
        colorList.add(new ColorName("Brązowy", 0xA5, 0x2A, 0x2A));
        colorList.add(new ColorName("Palone drewno", 0xDE, 0xB8, 0x87));
        colorList.add(new ColorName("Niebieski cadet", 0x5F, 0x9E, 0xA0));
        colorList.add(new ColorName("Ziołowy", 0x7F, 0xFF, 0x00));
        colorList.add(new ColorName("Głęboka czerwień", 0xD2, 0x69, 0x1E));
        colorList.add(new ColorName("Koralowy", 0xFF, 0x7F, 0x50));
        colorList.add(new ColorName("Chabrowy", 0x64, 0x95, 0xED));
        colorList.add(new ColorName("Kukurydziany", 0xFF, 0xF8, 0xDC));
        colorList.add(new ColorName("Karmazynowy", 0xDC, 0x14, 0x3C));
        colorList.add(new ColorName("Cyjan", 0x00, 0xFF, 0xFF));
        colorList.add(new ColorName("Ciemnoniebieski", 0x00, 0x00, 0x8B));
        colorList.add(new ColorName("Ciemny cyjan", 0x00, 0x8B, 0x8B));
        colorList.add(new ColorName("Brązowo pomarańczowy", 0xB8, 0x86, 0x0B));
        colorList.add(new ColorName("Ciemnoszary", 0xA9, 0xA9, 0xA9));
        colorList.add(new ColorName("Ciemnozielony", 0x00, 0x64, 0x00));
        colorList.add(new ColorName("Ciemny Khaki", 0xBD, 0xB7, 0x6B));
        colorList.add(new ColorName("Ciemna Magenta", 0x8B, 0x00, 0x8B));
        colorList.add(new ColorName("Ciemna zielona oliwka", 0x55, 0x6B, 0x2F));
        colorList.add(new ColorName("Ciemna pomarańcza", 0xFF, 0x8C, 0x00));
        colorList.add(new ColorName("Ciemna Orchidea", 0x99, 0x32, 0xCC));
        colorList.add(new ColorName("Ciemnoczerwony", 0x8B, 0x00, 0x00));
        colorList.add(new ColorName("Ciemny łososiowy", 0xE9, 0x96, 0x7A));
        colorList.add(new ColorName("Ciemna zieleń morska", 0x8F, 0xBC, 0x8F));
        colorList.add(new ColorName("Ciemnoniebieski", 0x48, 0x3D, 0x8B));
        colorList.add(new ColorName("Lekki odcień szarości", 0x2F, 0x4F, 0x4F));
        colorList.add(new ColorName("Ciemny turkus", 0x00, 0xCE, 0xD1));
        colorList.add(new ColorName("Ciemny fiolet", 0x94, 0x00, 0xD3));
        colorList.add(new ColorName("Głęboki róż", 0xFF, 0x14, 0x93));
        colorList.add(new ColorName("Głęboki niebieski", 0x00, 0xBF, 0xFF));
        colorList.add(new ColorName("Ciemnoszary", 0x69, 0x69, 0x69));
        colorList.add(new ColorName("Niebieski dodger", 0x1E, 0x90, 0xFF));
        colorList.add(new ColorName("Palona cegła", 0xB2, 0x22, 0x22));
        colorList.add(new ColorName("Biała róża", 0xFF, 0xFA, 0xF0));
        colorList.add(new ColorName("Zielony las", 0x22, 0x8B, 0x22));
        colorList.add(new ColorName("Fuksja", 0xFF, 0x00, 0xFF));
        colorList.add(new ColorName("Gainsboro", 0xDC, 0xDC, 0xDC));
        colorList.add(new ColorName("Szarobiały", 0xF8, 0xF8, 0xFF));
        colorList.add(new ColorName("Złoty", 0xFF, 0xD7, 0x00));
        colorList.add(new ColorName("Ciemnopomarańczowy", 0xDA, 0xA5, 0x20));
        colorList.add(new ColorName("Szary", 0x80, 0x80, 0x80));
        colorList.add(new ColorName("Zielony", 0x00, 0x80, 0x00));
        colorList.add(new ColorName("Zielono żółty", 0xAD, 0xFF, 0x2F));
        colorList.add(new ColorName("Miodowy", 0xF0, 0xFF, 0xF0));
        colorList.add(new ColorName("Gorący róż", 0xFF, 0x69, 0xB4));
        colorList.add(new ColorName("Indyjski czerwony", 0xCD, 0x5C, 0x5C));
        colorList.add(new ColorName("Indygo", 0x4B, 0x00, 0x82));
        colorList.add(new ColorName("Kość słoniowa", 0xFF, 0xFF, 0xF0));
        colorList.add(new ColorName("Khaki", 0xF0, 0xE6, 0x8C));
        colorList.add(new ColorName("Lawendowy", 0xE6, 0xE6, 0xFA));
        colorList.add(new ColorName("Lawendowo czerwony", 0xFF, 0xF0, 0xF5));
        colorList.add(new ColorName("Zieleń trawnikowa", 0x7C, 0xFC, 0x00));
        colorList.add(new ColorName("Szyfon cytrynowy", 0xFF, 0xFA, 0xCD));
        colorList.add(new ColorName("Jasnoniebieski", 0xAD, 0xD8, 0xE6));
        colorList.add(new ColorName("Jasnokoralowy", 0xF0, 0x80, 0x80));
        colorList.add(new ColorName("Jasnobłękitny", 0xE0, 0xFF, 0xFF));
        colorList.add(new ColorName("Jasnożółty złoty", 0xFA, 0xFA, 0xD2));
        colorList.add(new ColorName("Jasnoszary", 0xD3, 0xD3, 0xD3));
        colorList.add(new ColorName("Jasnozielony", 0x90, 0xEE, 0x90));
        colorList.add(new ColorName("Jasnoróżowy", 0xFF, 0xB6, 0xC1));
        colorList.add(new ColorName("Lekko łososiowy", 0xFF, 0xA0, 0x7A));
        colorList.add(new ColorName("Jasnozielona zieleń", 0x20, 0xB2, 0xAA));
        colorList.add(new ColorName("Jasno błękitny", 0x87, 0xCE, 0xFA));
        colorList.add(new ColorName("Light Slate Gray", 0x77, 0x88, 0x99));
        colorList.add(new ColorName("Jasny stalowo niebieski", 0xB0, 0xC4, 0xDE));
        colorList.add(new ColorName("Jasnożółty", 0xFF, 0xFF, 0xE0));
        colorList.add(new ColorName("Limonkowy", 0x00, 0xFF, 0x00));
        colorList.add(new ColorName("Limonkowo zielony", 0x32, 0xCD, 0x32));
        colorList.add(new ColorName("Cielisty", 0xFA, 0xF0, 0xE6));
        colorList.add(new ColorName("Magenta", 0xFF, 0x00, 0xFF));
        colorList.add(new ColorName("Maroon", 0x80, 0x00, 0x00));
        colorList.add(new ColorName("Blady akwamaryn", 0x66, 0xCD, 0xAA));
        colorList.add(new ColorName("Bladoniebieski", 0x00, 0x00, 0xCD));
        colorList.add(new ColorName("Blada Orchidea", 0xBA, 0x55, 0xD3));
        colorList.add(new ColorName("Bladopurpurowy", 0x93, 0x70, 0xDB));
        colorList.add(new ColorName("Zieleń morska", 0x3C, 0xB3, 0x71));
        colorList.add(new ColorName("Niebieski łupkowy", 0x7B, 0x68, 0xEE));
        colorList.add(new ColorName("Wiosenna zieleń", 0x00, 0xFA, 0x9A));
        colorList.add(new ColorName("Turkusowy", 0x48, 0xD1, 0xCC));
        colorList.add(new ColorName("Fioletowo czerwony", 0xC7, 0x15, 0x85));
        colorList.add(new ColorName("Niebieski wieczorny", 0x19, 0x19, 0x70));
        colorList.add(new ColorName("Miętowy", 0xF5, 0xFF, 0xFA));
        colorList.add(new ColorName("Mglisty róż", 0xFF, 0xE4, 0xE1));
        colorList.add(new ColorName("Mokasynowy", 0xFF, 0xE4, 0xB5));
        colorList.add(new ColorName("Navajo White", 0xFF, 0xDE, 0xAD));
        colorList.add(new ColorName("Morski", 0x00, 0x00, 0x80));
        colorList.add(new ColorName("Stary koronkowy", 0xFD, 0xF5, 0xE6));
        colorList.add(new ColorName("Oliwkowy", 0x80, 0x80, 0x00));
        colorList.add(new ColorName("Oliwkowy", 0x6B, 0x8E, 0x23));
        colorList.add(new ColorName("Pomarańczowy", 0xFF, 0xA5, 0x00));
        colorList.add(new ColorName("Pomarańczowo czerwony", 0xFF, 0x45, 0x00));
        colorList.add(new ColorName("Orchidea", 0xDA, 0x70, 0xD6));
        colorList.add(new ColorName("Blady ciemno pomarańczowy", 0xEE, 0xE8, 0xAA));
        colorList.add(new ColorName("Blada zieleń", 0x98, 0xFB, 0x98));
        colorList.add(new ColorName("Blady turkus", 0xAF, 0xEE, 0xEE));
        colorList.add(new ColorName("Blady fioletowo czerwony", 0xDB, 0x70, 0x93));
        colorList.add(new ColorName("Papaya", 0xFF, 0xEF, 0xD5));
        colorList.add(new ColorName("Brzoskwiniowy ptyś", 0xFF, 0xDA, 0xB9));
        colorList.add(new ColorName("Peru", 0xCD, 0x85, 0x3F));
        colorList.add(new ColorName("Różowy", 0xFF, 0xC0, 0xCB));
        colorList.add(new ColorName("Śliwkowy", 0xDD, 0xA0, 0xDD));
        colorList.add(new ColorName("Niebieski proch", 0xB0, 0xE0, 0xE6));
        colorList.add(new ColorName("Fioletowy", 0x80, 0x00, 0x80));
        colorList.add(new ColorName("Czerwony", 0xFF, 0x00, 0x00));
        colorList.add(new ColorName("Różowo brązowy", 0xBC, 0x8F, 0x8F));
        colorList.add(new ColorName("Królewski niebieski", 0x41, 0x69, 0xE1));
        colorList.add(new ColorName("Brąz siodła", 0x8B, 0x45, 0x13));
        colorList.add(new ColorName("Łososiowy", 0xFA, 0x80, 0x72));
        colorList.add(new ColorName("Piaskowy brązowy", 0xF4, 0xA4, 0x60));
        colorList.add(new ColorName("Morska zieleń", 0x2E, 0x8B, 0x57));
        colorList.add(new ColorName("Muszelkowy", 0xFF, 0xF5, 0xEE));
        colorList.add(new ColorName("Sienna", 0xA0, 0x52, 0x2D));
        colorList.add(new ColorName("Srebrny", 0xC0, 0xC0, 0xC0));
        colorList.add(new ColorName("Niebieski (Niebo)", 0x87, 0xCE, 0xEB));
        colorList.add(new ColorName("Odcień niebieskiego", 0x6A, 0x5A, 0xCD));
        colorList.add(new ColorName("Popielaty", 0x70, 0x80, 0x90));
        colorList.add(new ColorName("Śnieżny", 0xFF, 0xFA, 0xFA));
        colorList.add(new ColorName("Wiosenna zieleń", 0x00, 0xFF, 0x7F));
        colorList.add(new ColorName("Stalowo niebieski", 0x46, 0x82, 0xB4));
        colorList.add(new ColorName("Dębnik", 0xD2, 0xB4, 0x8C));
        colorList.add(new ColorName("Cyraneczka", 0x00, 0x80, 0x80));
        colorList.add(new ColorName("Oset", 0xD8, 0xBF, 0xD8));
        colorList.add(new ColorName("Pomidorowy", 0xFF, 0x63, 0x47));
        colorList.add(new ColorName("Turkusowy", 0x40, 0xE0, 0xD0));
        colorList.add(new ColorName("Fioletowy", 0xEE, 0x82, 0xEE));
        colorList.add(new ColorName("Zbożowy", 0xF5, 0xDE, 0xB3));
        colorList.add(new ColorName("Biały", 0xFF, 0xFF, 0xFF));
        colorList.add(new ColorName("Biały dym", 0xF5, 0xF5, 0xF5));
        colorList.add(new ColorName("Dymny", 0xFF, 0xFF, 0x00));
        colorList.add(new ColorName("Żółto niebieski", 0x9A, 0xCD, 0x32));
        return colorList;
    }
}
