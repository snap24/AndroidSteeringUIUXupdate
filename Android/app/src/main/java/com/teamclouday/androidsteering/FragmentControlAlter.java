package com.teamclouday.androidsteering;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.teamclouday.androidsteering.view.DraggableFrameLayout;

public class FragmentControlAlter extends Fragment {

    private DraggableFrameLayout draggableRoot;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_control_alter, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        
        draggableRoot = view.findViewById(R.id.draggable_root_alt);

        if (draggableRoot != null) {
            draggableRoot.setLayoutKey("alter_layout");
            draggableRoot.restoreSavedPositions();
            draggableRoot.setEditMode(activity.isEditMode);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || getView() == null) return;
        View view = getView();

        com.google.android.material.materialswitch.MaterialSwitch sensorSwitch = view.findViewById(R.id.switch_sensor_pitch);
        if (sensorSwitch != null) {
            sensorSwitch.setChecked(activity.useSensorPitch);
            sensorSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                activity.useSensorPitch = isChecked;
                activity.getGlobalBuffer().setUpdateRoll(isChecked); // Roll is Pitch/Accel in engine
                if (isChecked) {
                    Toast.makeText(getContext(), "Roll Sensor: ON", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Roll Sensor: OFF", Toast.LENGTH_SHORT).show();
                    // Send a clean 0 to ensure the PC doesn't get stuck if toggled off mid-tilt
                    activity.getGlobalBuffer().addData(MotionStatus.SetAccAngle, 0.0f);
                }
            });
        }

        view.findViewById(R.id.buttonLB).setOnTouchListener(activity::touchLB);
        view.findViewById(R.id.buttonRB).setOnTouchListener(activity::touchRB);

        view.findViewById(R.id.buttonLT).setOnTouchListener(activity::touchLT);
        view.findViewById(R.id.buttonRT).setOnTouchListener(activity::touchRT);
        
        View pbLT = view.findViewById(R.id.progressBarLT);
        if (pbLT != null) pbLT.setOnTouchListener(activity::touchLT);
        
        View pbRT = view.findViewById(R.id.progressBarRT);
        if (pbRT != null) pbRT.setOnTouchListener(activity::touchRT);

        view.findViewById(R.id.buttonA).setOnTouchListener(activity::touchA);
        view.findViewById(R.id.buttonB).setOnTouchListener(activity::touchB);
        view.findViewById(R.id.buttonX).setOnTouchListener(activity::touchX);
        view.findViewById(R.id.buttonY).setOnTouchListener(activity::touchY);
    }
}
