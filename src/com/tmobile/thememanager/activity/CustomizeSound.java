package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.activity.Customize.Customizations;
import com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.BaseThemeInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CustomizeSound extends GenericCustomize {
    public static final String EXTRA_PACK_NAME = "sound_pack_name";
    
    protected BaseAdapter initCustomAdapter() {
        return new SoundsAdapter(this);
    }
    
    public static void show(Activity context, Customizations customizations,
            ThemeItem baseTheme, int requestCode) {
        context.startActivityForResult(getShowIntent(context, customizations, baseTheme)
                .setClass(context, CustomizeSound.class), requestCode);
    }

    @Override
    protected void persistChanges() {
    }
    
    @Override
    protected void setResult(boolean ok) {
        Intent data = new Intent();
        if (ok) {
            CustomizableItem current = (CustomizableItem)mAdapter.getItem(selectedPos);
            data.putExtra(EXTRA_PACK_NAME, current.packageName);
        }
        setResult(ok ? RESULT_OK : RESULT_CANCELED, data);
    }

    private class SoundsAdapter extends BaseAdapter {
        private CustomizeSound context;
        private List<CustomizableItem> mItems;
        private final LayoutInflater mInflater;

        public SoundsAdapter(CustomizeSound context) {
            this.context = context;
            mInflater = LayoutInflater.from(context);
            List<String> packageNames = ThemeManager.getSoundPackages();
            mItems = new ArrayList<CustomizableItem>();
            for (String packageName : packageNames) {
                BaseThemeInfo info = ThemeManager.getInstalledThemePackage(packageName);
                if (info.isDrmProtected) continue;
                CustomizableItem item = new CustomizableItem();
                item.name = info.name;
                item.packageName = packageName;
                mItems.add(item);
            }
        }

        public int getCount() {
            return mItems.size();
        }

        public Object getItem(int position) {
            return mItems.get(position);
        }

        public long getItemId(int position) {
            context.setDirty(true);
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.personalize_row,
                        parent, false);
                holder = new ViewHolder(convertView.findViewById(R.id.p_row));
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            holder.getName().setText(mItems.get(position).name);
            return convertView;
        }

    }

    private class ViewHolder {
        private View view;
        private TextView name;

        public ViewHolder(View view) {
            this.view = view;
        }

        public TextView getName() {
            if (name == null) {
                name = (TextView) view.findViewById(android.R.id.title);
            }
            return name;
        }
    }

    private class CustomizableItem {
        public String name;
        public String packageName;
    }

}
