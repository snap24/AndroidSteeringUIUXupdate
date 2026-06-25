package com.teamclouday.androidsteering;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentSensitivity extends Fragment {

    private MainActivity activity;

    private SeekBar seekPitch;
    private SeekBar seekRoll;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_sensitivity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        if (activity == null) return;

        seekPitch = view.findViewById(R.id.seek_pitch_sensi);
        TextView tvPitch = view.findViewById(R.id.tv_pitch_sensi);
        if (seekPitch != null && tvPitch != null) {
            seekPitch.setProgress((int)(MainActivity.pitchSensitivity * 100));
            tvPitch.setText(String.format("Steering (Pitch) Sensitivity: %.1fx", MainActivity.pitchSensitivity));
            seekPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float sens = progress / 100f;
                    MainActivity.pitchSensitivity = sens;
                    tvPitch.setText(String.format("Steering (Pitch) Sensitivity: %.1fx", sens));
                    activity.getSharedPreferences("layout_positions", android.content.Context.MODE_PRIVATE)
                            .edit().putFloat("pitchSensitivity", sens).apply();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        seekRoll = view.findViewById(R.id.seek_roll_sensi);
        TextView tvRoll = view.findViewById(R.id.tv_roll_sensi);
        if (seekRoll != null && tvRoll != null) {
            seekRoll.setProgress((int)(MainActivity.rollSensitivity * 100));
            tvRoll.setText(String.format("Acceleration (Roll) Sensitivity: %.1fx", MainActivity.rollSensitivity));
            seekRoll.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float sens = progress / 100f;
                    MainActivity.rollSensitivity = sens;
                    tvRoll.setText(String.format("Acceleration (Roll) Sensitivity: %.1fx", sens));
                    activity.getSharedPreferences("layout_positions", android.content.Context.MODE_PRIVATE)
                            .edit().putFloat("rollSensitivity", sens).apply();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        android.view.MenuItem resetItem = menu.add(android.view.Menu.NONE, 999, android.view.Menu.NONE, "Reset Sensitivity");
        resetItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == 999) {
            if (seekPitch != null) seekPitch.setProgress(100);
            if (seekRoll != null) seekRoll.setProgress(100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
