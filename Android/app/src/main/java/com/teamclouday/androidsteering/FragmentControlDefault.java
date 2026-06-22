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

public class FragmentControlDefault extends Fragment {

    private DraggableFrameLayout draggableRoot;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_control_default, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        
        draggableRoot = view.findViewById(R.id.draggable_root_def);

        if (draggableRoot != null) {
            draggableRoot.setLayoutKey("default_layout");
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

        view.findViewById(R.id.buttonA).setOnTouchListener(activity::touchA);
        view.findViewById(R.id.buttonB).setOnTouchListener(activity::touchB);
        view.findViewById(R.id.buttonX).setOnTouchListener(activity::touchX);
        view.findViewById(R.id.buttonY).setOnTouchListener(activity::touchY);
    }
}
