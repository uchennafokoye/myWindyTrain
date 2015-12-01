package com.uchennafokoye.mywindytrain;


import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsIconFragment extends Fragment implements View.OnClickListener{

    ImageView imageButton;


    public SettingsIconFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings_icon, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        imageButton = (ImageView) getActivity().findViewById(R.id.settings_icon);
        imageButton.setOnClickListener(this);
    }

    public void onClick(final View v) {

        Intent intent = new Intent(getActivity(), MyPreferenceActivity.class);
        startActivity(intent);

    }


}
