package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.ThemeItem;
import com.tmobile.thememanager.utils.ThemeUtilities;
import com.tmobile.thememanager.widget.AbstractDAOItemAdapter;
import com.tmobile.thememanager.widget.ThemeAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class StyleChooser extends Activity implements OnItemClickListener {
    private StyleAdapter mAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.style_chooser);

        ListView list = (ListView)findViewById(android.R.id.list);
        mAdapter = new StyleAdapter(this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        int existingPos = mAdapter.findExistingOrCurrentItem(this,
                (Uri)getIntent().getParcelableExtra(ThemeManager.EXTRA_THEME_EXISTING_URI));
        if (existingPos >= 0) {
            list.setItemChecked(existingPos, true);
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ThemeItem item = mAdapter.getTheme(position);
        setResult(RESULT_OK, new Intent().setData(item.getUri(this)));
        finish();
        ThemeUtilities.applyStyle(this, item);
    }

    private class StyleAdapter extends ThemeAdapter {
        public StyleAdapter(Activity context) {
            super(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView text = (TextView)view.findViewById(android.R.id.text1);

            ThemeItem item = getTheme(cursor.getPosition());
            text.setText(item.getStyleName());
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getInflater().inflate(android.R.layout.tmobile_list_item_label_checkable,
                    parent, false);
        }
    }
}
