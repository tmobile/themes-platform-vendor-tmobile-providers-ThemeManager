package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Awkwardly structured widget designed to simulate a ListView under various
 * focus states for theme preview. Assets are pulled directly from the current
 * theme and applied manually.
 */
public class PreviewWidget extends ListView {
    /* "Fake" item states to be drawn. */
    private Drawable mSelectorDrawable;
    Drawable mButtonDrawable;
    
    private final Rect mSelectorBounds = new Rect();
    private final Rect mSelectorPadding = new Rect();
    
    public PreviewWidget(Context context) {
        super(context);
        init(context);
    }

    public PreviewWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PreviewWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    
    private static Drawable getStateDrawableFromStateList(Drawable d, int[] state) {
        if (d instanceof StateListDrawable) {
            StateListDrawable sld = (StateListDrawable)d;
            int index = sld.getStateDrawableIndex(state);
            if (index >= 0) {
                return sld.getStateDrawable(index);
            }
        }
        return null;
    }
    
    private void init(Context context) {
        PreviewAdapter adapter = new PreviewAdapter(context);
        setAdapter(adapter);
        
        TypedArray listA = context.obtainStyledAttributes(null,
                android.R.styleable.AbsListView, android.R.attr.listViewStyle, 0);

        Drawable listSelector =
            listA.getDrawableWithContext(context, android.R.styleable.AbsListView_listSelector);
        if (listSelector != null) {
            mSelectorDrawable = getStateDrawableFromStateList(listSelector,
                    ENABLED_FOCUSED_SELECTED_STATE_SET);
            if (mSelectorDrawable != null) {
                mSelectorDrawable.getPadding(mSelectorPadding);
            }
        }
        
        listA.recycle();

        TypedArray buttonA = context.obtainStyledAttributes(null,
                android.R.styleable.View, android.R.attr.buttonStyle, 0);

        Drawable buttonBackground =
            buttonA.getDrawableWithContext(context, android.R.styleable.View_background);
        if (buttonBackground != null) {
            mButtonDrawable = getStateDrawableFromStateList(buttonBackground,
                    ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET);
        }

        buttonA.recycle();
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        
        if (mSelectorDrawable != null && getChildCount() > 0) {
            View sel = getChildAt(0);
            Rect r = mSelectorPadding;
            mSelectorBounds.set(sel.getLeft() - r.left, sel.getTop() - r.top,
                    sel.getRight() + r.right, sel.getBottom() + r.bottom);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mSelectorDrawable != null) {
            mSelectorDrawable.setBounds(mSelectorBounds);
            mSelectorDrawable.draw(canvas);
        }
        
        super.dispatchDraw(canvas);
    }

    class PreviewAdapter extends BaseAdapter {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private final ArrayList<ViewDescriptor> mViews = new ArrayList<ViewDescriptor>();

        public PreviewAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mViews.add(new ViewDescriptor(R.string.preview_element_1).overrideBackground(true));
            mViews.add(new ViewDescriptor(R.string.preview_element_2).showButton(true));
            mViews.add(new ViewDescriptor(R.string.preview_element_3).showCheckmark(true));
        }
        
        public int getCount() {
            return mViews.size();
        }

        public Object getItem(int position) {
            return mViews.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                view = mInflater.inflate(R.layout.preview_content_item, parent, false);
                holder = new ViewHolder(view.findViewById(R.id.title),
                        view.findViewById(R.id.button),
                        view.findViewById(R.id.checkmark));
                view.setTag(holder);
            } else {
                holder = (ViewHolder)view.getTag();
            }
            
            ViewDescriptor d = mViews.get(position);

            holder.title.setText(d.title);
            holder.button.setVisibility(d.button ? View.VISIBLE : View.GONE);
            holder.checkmark.setVisibility(d.checkmark ? View.VISIBLE : View.GONE);
            
            if (d.button == true && mButtonDrawable != null) {
                holder.button.setBackgroundDrawable(mButtonDrawable);
            }

            if (d.checkmark == true) {
                holder.checkmark.setChecked(true);
            }

            view.setEnabled(d.enabled);
            
            if (d.overrideBackground == true) {
                view.setBackgroundDrawable(new NullDrawable());
            }
            
            return view;
        }

        class ViewDescriptor {
            public String title;
            public boolean button = false;
            public boolean checkmark = false;
            public boolean overrideBackground = false;
            public boolean enabled = true;

            public ViewDescriptor(int id) {
                this(mContext.getResources().getString(id));
            }

            public ViewDescriptor(String title) {
                this.title = title;
            }

            public ViewDescriptor showButton(boolean show) {
                this.button = show;
                return this;
            }

            public ViewDescriptor showCheckmark(boolean show) {
                this.checkmark = show;
                return this;
            }
            
            public ViewDescriptor overrideBackground(boolean override) {
                this.overrideBackground = override;
                return this;
            }

            public ViewDescriptor setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }
        }
    }
    
    static class ViewHolder {
        public TextView title;
        public Button button;
        public CheckBox checkmark;

        public ViewHolder(View title, View button, View checkmark) {
            this.title = (TextView)title;
            this.button = (Button)button;
            this.checkmark = (CheckBox)checkmark;
        }
    }
    
    static class NullDrawable extends Drawable {
        @Override
        public void draw(Canvas canvas) {
            /* Do nothing. */
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }
}
