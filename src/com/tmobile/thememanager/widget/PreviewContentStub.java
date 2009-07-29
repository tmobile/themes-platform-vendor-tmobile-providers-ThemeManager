package com.tmobile.thememanager.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/**
 * View utility which is similar in nature to ViewStub, allowing the caller to
 * re-inflate the referenced layout many times.  This can be useful when the
 * referenced views must be fully reconstructed on demand, such as with live
 * theme preview.
 */
public class PreviewContentStub extends FrameLayout {
    private LayoutInflater mInflater;
    private int mLayoutResource = 0;
    
    private boolean mDoInflate = false;

    private OnInflateListener mInflateListener;
    
    public PreviewContentStub(Context context) {
        super(context);
        initialize(context);
    }

    public PreviewContentStub(Context context, int layoutResource) {
        this(context);
        mLayoutResource = layoutResource;
    }

    public PreviewContentStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewContentStub(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.ViewStub,
                defStyle, 0);

        mLayoutResource = a.getResourceId(android.R.styleable.ViewStub_layout, 0);

        a.recycle();

        initialize(context);
    }

    private void initialize(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    /**
     * Returns the layout resource that will be used by {@link #setVisibility(int)} or
     * {@link #inflate()} to replace this StubbedView
     * in its parent by another view.
     *
     * @return The layout resource identifier used to inflate the new View.
     *
     * @see #setLayoutResource(int)
     * @see #setVisibility(int)
     * @see #inflate()
     * @attr ref android.R.styleable#ViewStub_layout
     */
    public int getLayoutResource() {
        return mLayoutResource;
    }

    /**
     * Specifies the layout resource to inflate when this StubbedView becomes visible or invisible
     * or when {@link #inflate()} is invoked. The View created by inflating the layout resource is
     * used to replace this StubbedView in its parent.
     * 
     * @param layoutResource A valid layout resource identifier (different from 0.)
     * 
     * @see #getLayoutResource()
     * @see #setVisibility(int)
     * @see #inflate()
     * @attr ref android.R.styleable#ViewStub_layout
     */
    public void setLayoutResource(int layoutResource) {
        mLayoutResource = layoutResource;
    }

    /**
     * Specifies the inflate listener to be notified after this PreviewContentStub successfully
     * inflated its layout resource.
     *
     * @param inflateListener The OnInflateListener to notify of successful inflation.
     */
    public void setOnInflateListener(OnInflateListener inflateListener) {
        mInflateListener = inflateListener;
    }
    
    /**
     * Listener used to receive a notification after a PreviewContentStub has successfully
     * inflated its layout resource.
     */
    public static interface OnInflateListener {
        /**
         * Invoked after a PreviewContentStub successfully inflated its layout resource.
         *
         * @param stub The PreviewContentStub that initiated the inflation.
         */
        void onPostInflate(PreviewContentStub stub);
        
        /**
         * Invoked just prior to PreviewContentStub inflating its layout resource.
         */
        void onPreInflate(PreviewContentStub stub);
    }
    
    public void postInflate() {
        /* When the next measure pass comes in, we'll go ahead and re-inflate.
         * We defer as an optimization in case many calls to postInflate occur
         * before the next layout pass. */
        mDoInflate = true;

        if (getChildCount() > 0) {
            removeAllViews();
        } else {
            requestLayout();
        }
    }

    private void inflate() {
        if (mLayoutResource == 0) {
            throw new IllegalArgumentException("PreviewContentStub must have a valid layoutResource");
        }
        
        mDoInflate = false;

        if (mInflateListener != null) {
            mInflateListener.onPreInflate(this);
        }

        mInflater.inflate(mLayoutResource, this);

        if (mInflateListener != null) {
            mInflateListener.onPostInflate(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDoInflate == true) {
            inflate();
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
