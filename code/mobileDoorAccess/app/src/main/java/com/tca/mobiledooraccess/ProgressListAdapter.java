package com.tca.mobiledooraccess;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Stefan on 12.07.2015.
 */
public class ProgressListAdapter extends ArrayAdapter<String> {
    private Activity context;
    private String[] itemLabel;
    private int[] imgId;

    public ProgressListAdapter(Activity context, String[] itemLabel, int[] imgId) {
        super(context, R.layout.image_row_layout, itemLabel);

        this.context = context;
        this.itemLabel = itemLabel;
        this.imgId = imgId;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View row = inflater.inflate(R.layout.image_row_layout, null, true);

        ImageView rowIcon = (ImageView)row.findViewById(R.id.icon);
        TextView rowLabel = (TextView)row.findViewById(R.id.label);

        if (imgId[position] > 0) {
            rowIcon.setImageResource(imgId[position]);
        }
        rowLabel.setText(itemLabel[position]);

        return row;
    }
}
