package com.teamclouday.androidsteering;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentConnection extends Fragment
{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.frag_connection, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity)getActivity();
        if (activity == null || getView() == null) return;
        View view = getView();

        ConnectionMode mode = activity.getConnectionMode();
        RadioGroup group = view.findViewById(R.id.radioGroup);
        Button button = view.findViewById(R.id.buttonConnect);

        if (group == null || button == null) return;

        // Source Code Links
        View btnTeam = view.findViewById(R.id.btnTeamCloudDay);
        if (btnTeam != null) {
            btnTeam.setOnClickListener(v -> openUrl("https://github.com/teamclouday/AndroidSteering"));
        }

        View btnSnap = view.findViewById(R.id.btnSnap24);
        if (btnSnap != null) {
            btnSnap.setOnClickListener(v -> openUrl("https://github.com/snap24/AndroidSteeringUIUXupdate"));
        }

        View btnCredit = view.findViewById(R.id.btnBgCredit);
        if (btnCredit != null) {
            btnCredit.setOnClickListener(v -> openUrl("https://toppng.com/john3"));
        }

        button.setOnClickListener(activity::connectionButtonOnClick);

        if(mode == ConnectionMode.Bluetooth)
        {
            group.check(R.id.radioButtonBth);
        }
        else
        {
            group.check(R.id.radioButtonWifi);
        }

        boolean connected = activity.isConnected();
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(!connected);
        }

        if(connected)
        {
            button.setText(R.string.buttonDisconnect);
        }
        else
        {
            button.setText(R.string.buttonConnect);
        }
        activity.setRadioGroupCallback();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {}
    }
}
