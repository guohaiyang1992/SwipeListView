
package com.guohaiyang.swipelistview;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.guohaiyang.swipelistviewlib.SwipeListView;

import java.util.ArrayList;

public class SecondeActivity extends Activity implements SwipeListView.onDeleteLisener, AdapterView.OnItemClickListener {
    private SwipeListView mListView;
    private MyAdapter myAdapter;
    private ArrayList<String> mData = new ArrayList<String>() {
        {
            for (int i = 0; i < 50; i++) {
                add("hello world, hello android  " + i);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seconde);

        mListView = (SwipeListView) findViewById(R.id.id_listview);
        myAdapter = new MyAdapter();
        mListView.setAdapter(myAdapter);
        mListView.setDeleteLisener(this);
        mListView.setOnItemClickListener(this);

    }

    @Override
    public void onSelectDelete(int position) {
        Toast.makeText(this, "delete:" + position, Toast.LENGTH_SHORT).show();
        mData.remove(position);
//        myAdapter.notifyDataSetChanged();

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this, "clcik:" + position, Toast.LENGTH_SHORT).show();
    }

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = LayoutInflater.from(SecondeActivity.this).inflate(R.layout.adapter_layout, parent, false);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.tv);
            TextView delete = (TextView) convertView.findViewById(R.id.delete);

            tv.setText(mData.get(position));

            final int pos = position;

            return convertView;
        }
    }
}

