package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Widget used to optionally draw an emblem over another view.
 */
public class EmblemOverlay extends LinearLayout {
    public static final int POSITION_TOP_LEFT = 0;
    public static final int POSITION_TOP_RIGHT = 1;
    public static final int POSITION_BOTTOM_LEFT = 2;
    public static final int POSITION_BOTTOM_RIGHT = 3;

    private Drawable mEmblemDrawable;
    private int mEmblemOffsetX;
    private int mEmblemOffsetY;
    private boolean mDrawEmblem = true;
    private int mPosition;

    public EmblemOverlay(Context context) {
        super(context);
        init(context);
    }

    public EmblemOverlay(Context context, AttributeSet set) {
        this(context, set, 0);
    }

    public EmblemOverlay(Context context, AttributeSet set, int defStyle) {
        this(context, set, 0, 0);
    }
    
    public EmblemOverlay(Context context, AttributeSet set, int defStyleAttr, int defStyleRes) {
        super(context, set, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(set, R.styleable.EmblemOverlay,
                defStyleAttr, defStyleRes);

        setEmblem(a.getDrawable(R.styleable.EmblemOverlay_emblem));
        setPosition(a.getInt(R.styleable.EmblemOverlay_position, POSITION_BOTTOM_RIGHT));

        int offsetX = a.getDimensionPixelOffset(R.styleable.EmblemOverlay_offsetX, 0);
        int offsetY = a.getDimensionPixelOffset(R.styleable.EmblemOverlay_offsetY, 0);
        setEmblemOffset(offsetX, offsetY);

        a.recycle();

        init(context);
    }

    private void init(Context context) {    
    }

    public void setEmblem(Drawable d) {
        if (mEmblemDrawable != d) {
            mEmblemDrawable = d;
            if (d != null) {
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            }
            invalidate();
        }
    }
    
    public Drawable getEmblem() {
        return mEmblemDrawable;
    }

    public void setEmblemOffset(int x, int y) {
        mEmblemOffsetX = x;
        mEmblemOffsetY = y;
    }
    
    public int getEmblemOffsetX() {
        return mEmblemOffsetX;
    }
    
    public int getEmblemOffsetY() {
        return mEmblemOffsetY;
    }

    public void setPosition(int position) {
        if (position < POSITION_TOP_LEFT || position > POSITION_BOTTOM_RIGHT) {
            throw new IllegalArgumentException("position must be topLeft, topRight, bottomLeft, or bottomRight");
        }
        if (mPosition != position) {
            mPosition = position;
            invalidate();
        }
    }
    
    public int getPosition() {
        return mPosition;
    }

    public void setDrawEmblem(boolean draw) {
        if (draw != mDrawEmblem) {
            mDrawEmblem = draw;
            invalidate();
        }
    }
    
    public boolean isDrawingEmblem() {
        return mDrawEmblem;
    }
    
    public View getWrappedView() {
        if (getChildCount() == 0) { 
            return null;
        }
        return getChildAt(0);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() > 1) {
            throw new UnsupportedOperationException("EmblemOverlay can have only 1 child.");
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        
        View wrappedView = getWrappedView();
        
        if (wrappedView != null && mEmblemDrawable != null && mDrawEmblem == true) {
            /* We use the child view's dimensions to avoid drawing into
             * padding areas. */
            int selfw = wrappedView.getMeasuredWidth();
            int selfh = wrappedView.getMeasuredHeight();

            Rect emblemRect = mEmblemDrawable.getBounds();

            float dx = 0;
            float dy = 0;

            if (mPosition == POSITION_TOP_LEFT) {
                dx = mEmblemOffsetX + wrappedView.getPaddingLeft();
                dy = mEmblemOffsetY + wrappedView.getPaddingTop();
            } else if (mPosition == POSITION_TOP_RIGHT) {
                dx = selfw - emblemRect.width() - mEmblemOffsetX - wrappedView.getPaddingRight();
                dy = mEmblemOffsetY + wrappedView.getPaddingTop();
            } else if (mPosition == POSITION_BOTTOM_LEFT) {
                dx = mEmblemOffsetX + wrappedView.getPaddingLeft();
                dy = selfh - emblemRect.height() - mEmblemOffsetY - wrappedView.getPaddingBottom();
            } else if (mPosition == POSITION_BOTTOM_RIGHT) {
                dx = selfw - emblemRect.width() - mEmblemOffsetX - wrappedView.getPaddingRight();
                dy = selfh - emblemRect.height() - mEmblemOffsetY - wrappedView.getPaddingBottom();
            }

            canvas.save();
            canvas.translate(dx, dy);
            mEmblemDrawable.draw(canvas);
            canvas.restore();
        }
    }
}
