package com.uchennafokoye.mywindytrain;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.ListFragment;
import android.widget.ArrayAdapter;

import java.util.Date;


public class TrainListFragment extends ListFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {



        Geolocation g = new Geolocation(16.205753, 5.835114);
        Distance d = new Distance(3.5, "miles");
        Date n_a = new Date(1445959800);
        TrainColor t_c = new TrainColor("blue");

        Train t1 = new Train(t_c, g, d, n_a);

        g = new Geolocation(19.205753, 7.835114);
        d = new Distance(4.2, "miles");
        n_a = new Date(1445959800);
        t_c = new TrainColor("brown");

        Train t2 = new Train(t_c, g, d, n_a);

        g = new Geolocation(18.205753, 4.835114);
        d = new Distance(2.5, "miles");
        n_a = new Date(1445959800);
        t_c = new TrainColor("red");

        Train t3 = new Train(t_c, g, d, n_a);

        g = new Geolocation(12.205753, 5.835114);
        d = new Distance(3.5, "miles");
        n_a = new Date(1445959800);
        t_c = new TrainColor("green");

        Train t4 = new Train(t_c, g, d, n_a);

        Train[] trains = new Train[]{t1, t2, t3, t4};




        String[] times = new String[trains.length];
        for (int i = 0; i < times.length; i++){
            times[i] = trains[i].toString();
        }

        CustomAdapter adapter = new CustomAdapter(inflater.getContext(), trains);
        setListAdapter(adapter);


        return super.onCreateView(inflater, container, savedInstanceState);
    }

}
