package com.uchennafokoye.mywindytrain;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by faithful.okoye on 11/21/15.
 */
public class CustomAdapterSpinner extends ArrayAdapter<String> {


    String[] colorArray;
    Context context;
    int resource;

    private static LayoutInflater inflater;

    public CustomAdapterSpinner(Context context, int resource) {
        super(context, resource);
        inflater = LayoutInflater.from(context);
        colorArray = TrainColor.TRAIN_COLORS_ARRAY;
        this.resource = resource;
        this.context = context;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent){

        return getView(position, convertView, parent);
    }


    @Override
    public int getCount() {
        return colorArray.length;
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    public class Holder {
        TextView tv1;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent){

        Holder holder = new Holder();
        View rowView;
        rowView = inflater.inflate(resource, parent, false);
        holder.tv1 = (TextView) rowView.findViewById(android.R.id.text1);

        String colorHex = TrainColor.getColor(colorArray[position]);
        holder.tv1.setText(colorArray[position]);
        holder.tv1.setTextColor(Color.parseColor(colorHex));
        holder.tv1.setTextSize(14);
        holder.tv1.setTypeface(Typeface.DEFAULT_BOLD);

        return rowView;
    }

}
