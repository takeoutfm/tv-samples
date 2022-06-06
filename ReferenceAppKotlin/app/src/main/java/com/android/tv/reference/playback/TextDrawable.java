package com.android.tv.reference.playback;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

// https://stackoverflow.com/questions/3972445/how-to-put-text-in-a-drawable

public class TextDrawable extends Drawable {
    private static final int DEFAULT_COLOR = Color.WHITE;
    private static final int DEFAULT_TEXT_SIZE = 15;
    private final Paint mPaint;
    private final CharSequence mText;
    private final int mIntrinsicWidth;
    private final int mIntrinsicHeight;

    public TextDrawable(Resources res, CharSequence text) {
        mText = text;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setTextAlign(Paint.Align.CENTER);
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                DEFAULT_TEXT_SIZE, res.getDisplayMetrics());
        mPaint.setTextSize(textSize);
        mPaint.setFakeBoldText(true);
        mIntrinsicWidth = (int) (mPaint.measureText(mText, 0, mText.length()) + .5);
        mIntrinsicHeight = mPaint.getFontMetricsInt(null);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();

//        mPaint.setColor(mHighlightColor);
//        mPaint.setStyle(Paint.Style.FILL);
//        mPaint.setStrokeWidth(1);
//        canvas.drawRect(0, 0, bounds.width(), bounds.height(), mPaint);

//        mPaint.setColor(DEFAULT_COLOR);
//                mPaint.setColorFilter(new PorterDuffColorFilter(DEFAULT_COLOR, PorterDuff.Mode.SRC_OUT));
//        canvas.drawColor(mHighlightColor, PorterDuff.Mode.SRC);
//        mPaint.setColor(Color.RED);
//        mPaint.setStrokeWidth(3);

//        mPaint.setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.CLEAR));
//        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
//        mPaint.setColor(DEFAULT_COLOR);
        canvas.drawText(mText, 0, mText.length(),
                bounds.centerX(), (float) (bounds.centerY() + (mIntrinsicHeight * .3)), mPaint);
    }

    @Override
    public int getOpacity() {
        return mPaint.getAlpha();
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        mPaint.setColorFilter(filter);
    }
}

