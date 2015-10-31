package com.uchennafokoye.mywindytrain;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by faithfulokoye on 10/31/15.
 */
public class CustomAdapter extends BaseAdapter{

//    String[] distance;
//    String[] next_arrival;

    Train[] trains;
    Context context;

    private static LayoutInflater inflater;

    public CustomAdapter(Context context, Train[] trains){

        this.context = context;
        inflater = LayoutInflater.from(context);
        this.trains = trains;

    }


    @Override
    public int getCount() {
        return trains.length;
    }

    @Override
    public Object getItem(int position){

        if (position >= trains.length && position < 0){
            return null;
        }
        return trains[position];
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    public class Holder {
        View v;
        TextView tv1;
        TextView tv2;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent){

        Holder holder = new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.fragment_train_list, null);
        holder.v = (View) rowView.findViewById(R.id.train_list_item_view);
        holder.tv1 = (TextView) rowView.findViewById(R.id.train_list_item_tv1);
        holder.tv2 = (TextView) rowView.findViewById(R.id.train_list_item_tv2);


        String color = ((Train) trains[position]).getTrainColor().color;
        String distance = trains[position].getDistance().toString();
        String next_arrival = trains[position].getTimeString();

        holder.v.setBackgroundColor(Color.parseColor(color));
        holder.tv1.setText("Distance: " + distance);
        holder.tv2.setText("Next Arrival: " + next_arrival);

        return rowView;
    }

}
