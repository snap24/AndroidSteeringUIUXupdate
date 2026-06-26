package com.teamclouday.androidsteering;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.teamclouday.androidsteering.view.DraggableFrameLayout;
import com.teamclouday.androidsteering.view.PedalSliderView;
import com.teamclouday.androidsteering.view.SteeringWheelView;

public class FragmentControlSteeringWheel extends Fragment {

    private DraggableFrameLayout draggableRoot;
    private MainActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_control_steeringwheel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        if (activity == null) return;

        draggableRoot = view.findViewById(R.id.draggable_root_sw);
        
        if (draggableRoot != null) {
            draggableRoot.setLayoutKey("steering_wheel_advanced");
            draggableRoot.restoreSavedPositions();
            draggableRoot.setEditMode(activity.isEditMode);
        }

        setupControls();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupControls() {
        if (activity == null || getView() == null) return;
        View view = getView();

        // Sensor Toggle
        com.google.android.material.materialswitch.MaterialSwitch sensorSwitch = view.findViewById(R.id.switch_sensor);
        if (sensorSwitch != null) {
            sensorSwitch.setText("Tilt Steering");
            sensorSwitch.setChecked(activity.useSensorSteering);
            sensorSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                activity.useSensorSteering = isChecked;
                activity.getGlobalBuffer().setUpdatePitch(isChecked); // Master control for steering sensor
                if (isChecked) {
                    Toast.makeText(getContext(), "Tilt Steering: ON", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Tilt Steering: OFF", Toast.LENGTH_SHORT).show();
                    // Send a clean 0 to prevent a 'stuck' wheel
                    activity.getGlobalBuffer().addData(MotionStatus.SetSteerAngle, 0.0f);
                }
            });
        }

        // Steering Wheel
        SteeringWheelView sw = view.findViewById(R.id.steering_wheel_view);
        TextView tvSteer = view.findViewById(R.id.tv_steering_angle);
        if (sw != null) {
            sw.setOnSteeringChangeListener(angle -> {
                // If the sensor is OFF, then manual touch sends the data.
                if (!activity.useSensorSteering) {
                    activity.getGlobalBuffer().addData(MotionStatus.SetSteerAngle, -angle * 90f);
                }
                if (tvSteer != null) {
                    tvSteer.setText(String.format("STEER: %.0f°", angle * 90f));
                }
            });
        }

        // Pedals
        PedalSliderView gas = view.findViewById(R.id.slider_accel);
        if (gas != null) {
            gas.setLabel("Accelerator");
            gas.setAccentColor(0xFF00E676);
            gas.setOnPedalChangeListener(val -> activity.getGlobalBuffer().addData(MotionStatus.SetRTValue, val));
        }

        PedalSliderView brake = view.findViewById(R.id.slider_brake);
        if (brake != null) {
            brake.setLabel("Brake");
            brake.setAccentColor(0xFFFF5252);
            brake.setOnPedalChangeListener(val -> activity.getGlobalBuffer().addData(MotionStatus.SetLTValue, val));
        }

        PedalSliderView clutch = view.findViewById(R.id.slider_clutch);
        if (clutch != null) {
            clutch.setLabel("Clutch");
            clutch.setAccentColor(0xFFFFAB40);
            final boolean[] isClutchPressed = {false};
            clutch.setOnPedalChangeListener(val -> {
                boolean shouldPress = val >= 0.2f;
                if (shouldPress && !isClutchPressed[0]) {
                    isClutchPressed[0] = true;
                    activity.getGlobalBuffer().addData(MotionButton.HOME, true);
                } else if (!shouldPress && isClutchPressed[0]) {
                    isClutchPressed[0] = false;
                    activity.getGlobalBuffer().addData(MotionButton.HOME, false);
                }
            });
        }

        // Action Buttons
        view.findViewById(R.id.buttonA).setOnTouchListener(activity::touchA);
        view.findViewById(R.id.buttonB).setOnTouchListener(activity::touchB);
        view.findViewById(R.id.buttonX).setOnTouchListener(activity::touchX);
        view.findViewById(R.id.buttonY).setOnTouchListener(activity::touchY);

        // D-Pad
        view.findViewById(R.id.buttonUP).setOnTouchListener(activity::touchUP);
        view.findViewById(R.id.buttonDOWN).setOnTouchListener(activity::touchDOWN);
        view.findViewById(R.id.buttonLEFT).setOnTouchListener(activity::touchLEFT);
        view.findViewById(R.id.buttonRIGHT).setOnTouchListener(activity::touchRIGHT);

        // Top Buttons
        view.findViewById(R.id.buttonSTART).setOnTouchListener(activity::touchSTART);
        view.findViewById(R.id.buttonBACK).setOnTouchListener(activity::touchBACK);

        view.findViewById(R.id.buttonLB).setOnTouchListener(activity::touchLB);
        view.findViewById(R.id.buttonRB).setOnTouchListener(activity::touchRB);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupControls();
    }
}
