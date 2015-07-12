package com.tca.mobiledooraccess;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Stefan on 12.07.2015.
 */
public class ProgressListAdapter extends ArrayAdapter<String> {
    static class ViewHolder {
        ImageView rowIcon;
        TextView rowLabel;
        TextView rowDescription;
    }

    private Activity context;
    private final LayoutInflater inflater;
    private ArrayList<ProgressStatusModel> items;

    public ProgressListAdapter(Activity context, ArrayList items) {
        super(context, R.layout.image_row_layout, items);
        inflater = LayoutInflater.from(context);

        this.context = context;
        this.items = items;
    }

    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;

        if (view == null) {
            view = inflater.inflate(R.layout.image_row_layout, null, true);
            holder = new ViewHolder();
            holder.rowIcon = (ImageView)view.findViewById(R.id.icon);
            holder.rowLabel = (TextView)view.findViewById(R.id.label);
            holder.rowDescription = (TextView)view.findViewById(R.id.description);

            view.setTag(holder);
        } else {
            // Fetch bindings from cache
            holder = (ViewHolder)view.getTag();
        }
//        LayoutInflater inflater = context.getLayoutInflater();
//        View row = inflater.inflate(R.layout.image_row_layout, null, true);
//
//        ImageView rowIcon = (ImageView)row.findViewById(R.id.icon);
//        TextView rowLabel = (TextView)row.findViewById(R.id.label);
//        TextView rowDescription = (TextView)row.findViewById(R.id.description);

        // Fill UI with supplied data
        ProgressStatusModel curItem = items.get(position);
        holder.rowIcon.setImageResource(curItem.iconId);
        holder.rowLabel.setText(curItem.label);
        holder.rowDescription.setText(curItem.description);

        return view;
    }

    public void setIconUntilPosition(int untilPosition, int resourceID) {
        int count = 0;
        for (ProgressStatusModel item : items) {
            if (count >= untilPosition) {
                break;
            }
            item.iconId = resourceID;

            count++;
        }
        notifyDataSetChanged();
    }
}
