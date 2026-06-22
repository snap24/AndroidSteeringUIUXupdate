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
import io.github.controlwear.virtual.joystick.android.JoystickView;

public class FragmentControlPad extends Fragment {

    private DraggableFrameLayout draggableRoot;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_control_pad, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        
        draggableRoot = view.findViewById(R.id.draggable_root_pad);

        if (draggableRoot != null) {
            draggableRoot.setLayoutKey("pad_layout");
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

        view.findViewById(R.id.buttonHOME).setOnTouchListener(activity::touchHOME);

        view.findViewById(R.id.buttonSTART).setOnTouchListener(activity::touchSTART);
        view.findViewById(R.id.buttonBACK).setOnTouchListener(activity::touchBACK);

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

        view.findViewById(R.id.buttonUP).setOnTouchListener(activity::touchUP);
        view.findViewById(R.id.buttonDOWN).setOnTouchListener(activity::touchDOWN);
        view.findViewById(R.id.buttonLEFT).setOnTouchListener(activity::touchLEFT);
        view.findViewById(R.id.buttonRIGHT).setOnTouchListener(activity::touchRIGHT);

        JoystickView joystickLeft = view.findViewById(R.id.joystickLeft);
        if (joystickLeft != null) joystickLeft.setOnMoveListener(activity::moveLeftStick);
        
        JoystickView joystickRight = view.findViewById(R.id.joystickRight);
        if (joystickRight != null) joystickRight.setOnMoveListener(activity::moveRightStick);
    }
}
