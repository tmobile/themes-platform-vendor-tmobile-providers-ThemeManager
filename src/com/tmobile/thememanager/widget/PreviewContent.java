package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class PreviewContent extends FrameLayout {
    private ViewGroup mPreviewWrapperView;
    private PreviewWidget mPreviewView;
    private final Rect mPreviewRect = new Rect();
    private final Paint mPreviewPaint = new Paint();
    private final Paint mShadowPaint = new Paint();

    public PreviewContent(Context context) {
        this(context, null);
        init(context);
    }

    public PreviewContent(Context context, AttributeSet set) {
        this(context, set, 0);
    }

    public PreviewContent(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.preview_content, this);

        mPreviewView = (PreviewWidget)findViewById(R.id.preview_widget);
        mPreviewWrapperView = (ViewGroup)mPreviewView.getParent();

        /* This causes the view to not be interactive, but we'll still force
         * it to display during method. */
        mPreviewWrapperView.setDrawingCacheEnabled(true);
        mPreviewWrapperView.setVisibility(View.INVISIBLE);

        /* Draw the shadow color using the focused, selected text color for
         * textAppearanceLarge. This color is believed most likely to have the
         * best contrast against the background and list selector. */
        TypedArray textA = context.obtainStyledAttributes(null,
                android.R.styleable.TextAppearance, android.R.attr.textAppearanceLarge, 0);

        ColorStateList primaryColor =
            textA.getColorStateList(android.R.styleable.TextAppearance_textColor);
        int shadowColor = primaryColor.getColorForState(ENABLED_FOCUSED_SELECTED_STATE_SET,
                primaryColor.getDefaultColor());
        mShadowPaint.setColor(shadowColor);
        mShadowPaint.setShadowLayer(12, 0, 0, shadowColor);

        textA.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        if (count == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else if (count > 0) {
            int selfw = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            int selfh = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

            setMeasuredDimension(selfw, selfh);

            mPreviewWrapperView.measure(MeasureSpec.makeMeasureSpec((int)(selfw * 0.70), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((int)(selfh * 0.70), MeasureSpec.AT_MOST));
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        
        View preview = mPreviewWrapperView;
        mPreviewRect.set(preview.getLeft(), preview.getTop(),
                preview.getRight(), preview.getBottom());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        /* It won't draw during our parent's dispatchDraw because it is
         * invisible. Force it now. */
        Bitmap bmp = mPreviewWrapperView.getDrawingCache();
        if (bmp != null) {
            canvas.drawRect(mPreviewRect, mShadowPaint);
            canvas.drawBitmap(bmp, null, mPreviewRect, mPreviewPaint);
        }
    }
}
