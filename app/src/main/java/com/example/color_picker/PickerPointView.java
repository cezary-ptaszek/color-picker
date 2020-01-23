package com.example.color_picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


public class PickerPointView extends View implements View.OnTouchListener {
    private Drawable bgDrawable;
    private int color;
    private int width, height;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PickerPointView(Context context) {
        this(context, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PickerPointView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PickerPointView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PickerPointView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.pickerpoint);
        bgDrawable = array.getDrawable(R.styleable.pickerpoint_background);
        color = array.getColor(R.styleable.pickerpoint_src, 0);
        width = getWidth() == 0 ? 144 : getWidth();
        height = getHeight() == 0 ? 144 : getHeight();
        mPaint.setColor(color);

        setOnTouchListener(this);
    }

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        bgDrawable.setBounds(0, 0, 144, 144);
        bgDrawable.draw(canvas);
        int radius = (Math.min(width, height) / 2 - 18 * 2);
        canvas.drawCircle(width / 2, width / 2, radius, mPaint);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return false;
    }
}
