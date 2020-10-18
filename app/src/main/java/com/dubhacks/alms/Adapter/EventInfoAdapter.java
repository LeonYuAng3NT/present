package com.dubhacks.alms.Adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dubhacks.alms.R;

public class EventInfoAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final String[] itemname;
    private final Integer[] imgid;

    public EventInfoAdapter(Activity context, String[] itemname, Integer[] imgid) {
        super(context, R.layout.event, itemname);
        this.context = context;
        this.itemname = itemname;
        this.imgid = imgid;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.event, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.eventInfo);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        txtTitle.setText(itemname[position]);
        imageView.setImageResource(imgid[position]);

        return rowView;

    };
}
