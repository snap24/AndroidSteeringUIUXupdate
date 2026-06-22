package com.teamclouday.androidsteering;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.math.MathUtils;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;

import java.util.Objects;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Iterator;

enum ControllerMode {
    None, Default, Alter, GamePad, SteeringWheel
}

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_EXPORT = 101;
    private static final int REQUEST_CODE_IMPORT = 102;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    private Motion serviceMotion;
    private Connection serviceConnection;
    private Thread threadConnect;
    private Thread threadDisconnect;

    private ControllerMode controllerMode;
    private volatile boolean LTPressed = false;
    private volatile boolean RTPressed = false;

    private float LTRatio = 0.0f;
    private Runnable releaseLTRunnable;
    private float RTRatio = 0.0f;
    private Runnable releaseRTRunnable;

    private float displayY;
    private final float SLIDE_DISTANCE_DP = 75.0f; 
    private float slideDistancePx;
    public boolean isEditMode = false;
    public boolean useSensorSteering = false; 
    public boolean useSensorPitch = false;

    public static float pitchSensitivity = 1.0f;
    public static float rollSensitivity = 1.0f;

    private final Handler handlerUpdateUI = new Handler(Looper.getMainLooper());
    private final Runnable runnableUpdateUI = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            try {
                if (controllerMode == ControllerMode.Default || controllerMode == ControllerMode.Alter) {
                    com.teamclouday.androidsteering.view.SensorCrosshairView crosshair = findViewById(R.id.sensorCrosshair);
                    if (crosshair != null) {
                        float progressHorizontal = (serviceMotion.readPitch() + 90.0f) / 180.0f * 100.0f;
                        // If Accel is OFF (Layout 2 when toggled off), lock the dot at center (50%)
                        float progressVertical = 50f;
                        if (controllerMode == ControllerMode.Default || (controllerMode == ControllerMode.Alter && useSensorPitch)) {
                            progressVertical = (180.0f - serviceMotion.readRoll()) / 360.0f * 100.0f;
                        }
                        crosshair.setRollPitch(progressHorizontal, progressVertical);
                    }
                }
                if (controllerMode == ControllerMode.SteeringWheel) {
                    com.teamclouday.androidsteering.view.SteeringWheelView sw = findViewById(R.id.steering_wheel_view);
                    android.widget.TextView tvSteer = findViewById(R.id.tv_steering_angle);
                    com.teamclouday.androidsteering.view.SensorCrosshairView crosshair = findViewById(R.id.sensorCrosshair);
                    
                    float finalSteerDeg = 0f;

                    if (sw != null && !isEditMode) {
                        if (useSensorSteering) {
                            finalSteerDeg = serviceMotion.readPitch(); // Corrected: Steer is Pitch in the math engine
                            sw.setSensorAngle(finalSteerDeg);
                        } else {
                            // Manual data is now sent directly from the listener in SteeringWheelView
                            // to prevent data collisions with the sensor thread.
                            finalSteerDeg = sw.getSteeringAngle() * sw.getMaxRotationDeg();
                        }
                        
                        if (tvSteer != null) {
                            tvSteer.setText(String.format("STEER: %.0f°", finalSteerDeg));
                        }
                    }

                    if (crosshair != null) {
                        // Horizontal Dot -> Steering
                        float steerPercent = (finalSteerDeg + 90.0f) / 180.0f * 100.0f;
                        // Vertical Dot -> Locked at 50% (since Accel is OFF for Layout 4)
                        crosshair.setRollPitch(steerPercent, 50f);
                    }
                }
                
                if (controllerMode == ControllerMode.Default || controllerMode == ControllerMode.Alter) {
                    View steeringWheel = findViewById(R.id.steering_wheel_view);
                    if (steeringWheel != null && !isEditMode && useSensorSteering) {
                        steeringWheel.setRotation(serviceMotion.readPitch()); // Corrected
                    }
                }
            } catch (Exception e) {
                Log.d("MainActivity", Objects.requireNonNull(e.getMessage()));
            } finally {
                handlerUpdateUI.postDelayed(this, 20);
            }
        }
    };

    private final Connection.MyBuffer globalBuffer = new Connection.MyBuffer();

    public Connection.MyBuffer getGlobalBuffer() {
        return globalBuffer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences layoutPrefs = getSharedPreferences("layout_positions", Context.MODE_PRIVATE);
        pitchSensitivity = layoutPrefs.getFloat("pitchSensitivity", 1.0f);
        rollSensitivity = layoutPrefs.getFloat("rollSensitivity", 1.0f);

        setContentView(R.layout.main);
        slideDistancePx = SLIDE_DISTANCE_DP * getResources().getDisplayMetrics().density;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayY = displayMetrics.heightPixels * 0.5f;
        setFragment(R.id.nav_connection_frag);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.navTitleConnection);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        NavigationView navigationView = findViewById(R.id.navView);
        navigationView.getMenu().getItem(0).setChecked(true);
        setupDrawerContent(navigationView);

        mDrawer = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();
        mDrawer.addDrawerListener(drawerToggle);

        checkSensor();
        serviceMotion = new Motion(this, globalBuffer);
        serviceMotion.start();
        serviceConnection = new Connection(this, globalBuffer);
        handlerUpdateUI.postDelayed(runnableUpdateUI, 0);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            selectDrawerItem(menuItem);
            return true;
        });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        setFragment(menuItem.getItemId());
        menuItem.setChecked(true);
        toolbar.setTitle(menuItem.getTitle());
        mDrawer.closeDrawers();
    }

    public void setFragment(int fragmentId) {
        resetController();
        Fragment fragment;
        try {
            if (fragmentId == R.id.nav_connection_frag) {
                fragment = FragmentConnection.class.newInstance();
                controllerMode = ControllerMode.None;
                globalBuffer.turnOff();
            } else if (fragmentId == R.id.nav_control_default_frag) {
                fragment = FragmentControlDefault.class.newInstance();
                controllerMode = ControllerMode.Default;
                globalBuffer.turnOn();
                globalBuffer.setUpdatePitch(true); // Steer ON
                globalBuffer.setUpdateRoll(true);  // Accel ON (Only Layout 1)
            } else if (fragmentId == R.id.nav_control_alt_frag) {
                fragment = FragmentControlAlter.class.newInstance();
                controllerMode = ControllerMode.Alter;
                globalBuffer.turnOn();
                globalBuffer.setUpdatePitch(true);  // Steer ON
                globalBuffer.setUpdateRoll(useSensorPitch); // Accel controlled by toggle
            } else if (fragmentId == R.id.nav_control_pad_frag) {
                fragment = FragmentControlPad.class.newInstance();
                controllerMode = ControllerMode.GamePad;
                globalBuffer.turnOn();
                globalBuffer.setUpdatePitch(false); // Steer OFF
                globalBuffer.setUpdateRoll(false);  // Accel OFF
            } else if (fragmentId == R.id.nav_control_steeringwheel_frag) {
                fragment = FragmentControlSteeringWheel.class.newInstance();
                controllerMode = ControllerMode.SteeringWheel;
                globalBuffer.turnOn();
                globalBuffer.setUpdatePitch(useSensorSteering);  // Steering is controlled by toggle
                globalBuffer.setUpdateRoll(false); // Acceleration sensor is ALWAYS OFF
            } else if (fragmentId == R.id.nav_sensitivity_frag) {
                fragment = FragmentSensitivity.class.newInstance();
                controllerMode = ControllerMode.None;
                globalBuffer.turnOff();
            } else {
                fragment = FragmentConnection.class.newInstance();
                controllerMode = ControllerMode.None;
                globalBuffer.turnOff();
            }
        } catch (Exception e) {
            Log.d("MainActivity", Objects.requireNonNull(e.getMessage()));
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        MenuItem editItem = menu.findItem(R.id.action_edit_layout);
        if (editItem != null) {
            editItem.setTitle(isEditMode ? "Save Layout" : "Edit Layout");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) return true;
        int id = item.getItemId();
        if (id == R.id.action_set_mapping) {
            showMappingDialog();
            return true;
        } else if (id == R.id.action_edit_layout) {
            isEditMode = !isEditMode;
            invalidateOptionsMenu(); // Refresh the menu title
            
            if (!isEditMode) {
                Toast.makeText(this, "Layout Saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Edit Mode: Drag items anywhere", Toast.LENGTH_SHORT).show();
            }
            
            recreateCurrentFragment();
            return true;
        } else if (id == R.id.action_reset_layout) {
            isEditMode = false;
            invalidateOptionsMenu();
            
            DraggableButtonHelper.resetPositions(this);
            // Also need to trigger the internal reset of DraggableFrameLayout if we are currently looking at it
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.flContent);
            if (current != null && current.getView() != null) {
                View root = current.getView().findViewById(R.id.draggable_root_sw);
                if (root == null) root = current.getView().findViewById(R.id.draggable_root_def);
                if (root == null) root = current.getView().findViewById(R.id.draggable_root_pad);
                if (root == null) root = current.getView().findViewById(R.id.draggable_root_alt);
                
                if (root instanceof com.teamclouday.androidsteering.view.DraggableFrameLayout) {
                    ((com.teamclouday.androidsteering.view.DraggableFrameLayout) root).resetToDefault();
                }
            }
            
            recreateCurrentFragment();
            Toast.makeText(this, "Layout Reset", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_export_layout) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "steering_layouts.json");
            startActivityForResult(intent, REQUEST_CODE_EXPORT);
            return true;
        } else if (id == R.id.action_import_layout) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String exportLayoutsToJson() {
        JSONObject rootObject = new JSONObject();
        try {
            SharedPreferences prefs = getSharedPreferences("layout_positions", Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                rootObject.put(entry.getKey(), entry.getValue());
            }
            return rootObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private void importLayoutsFromJson(String jsonString) {
        try {
            JSONObject rootObject = new JSONObject(jsonString);
            SharedPreferences.Editor editor = getSharedPreferences("layout_positions", Context.MODE_PRIVATE).edit();
            editor.clear(); // Clear existing preferences
            
            // Check if it's a legacy JSON missing Ratio keys
            boolean hasRatio = false;
            float maxX = 0;
            float maxY = 0;
            Iterator<String> keyCheck = rootObject.keys();
            while (keyCheck.hasNext()) {
                String k = keyCheck.next();
                if (k.endsWith("Ratio")) hasRatio = true;
                if (k.endsWith("_x")) maxX = Math.max(maxX, (float) rootObject.optDouble(k, 0));
                if (k.endsWith("_y")) maxY = Math.max(maxY, (float) rootObject.optDouble(k, 0));
            }
            
            float assumeWidth = maxX > 2300 ? 2400f : (maxX > 2100 ? 2340f : 2400f);
            float assumeHeight = 1080f;

            Iterator<String> viewKeys = rootObject.keys();
            while (viewKeys.hasNext()) {
                String key = viewKeys.next();
                Object value = rootObject.get(key);
                if (value instanceof Number) {
                    float fVal = ((Number) value).floatValue();
                    editor.putFloat(key, fVal);
                    // Generate Ratio if missing
                    if (!hasRatio && key.endsWith("_x")) {
                        editor.putFloat(key + "Ratio", fVal / assumeWidth);
                    }
                    if (!hasRatio && key.endsWith("_y")) {
                        editor.putFloat(key + "Ratio", fVal / assumeHeight);
                    }
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                }
            }
            editor.putBoolean("is_initialized", true);
            editor.apply();
            
            // Restart activity to apply imported layouts correctly
            finish();
            startActivity(getIntent());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_EXPORT) {
                try {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        os.write(exportLayoutsToJson().getBytes(StandardCharsets.UTF_8));
                        os.close();
                        Toast.makeText(this, "Layouts exported successfully", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_CODE_IMPORT) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        is.close();
                        importLayoutsFromJson(sb.toString());
                        Toast.makeText(this, "Layouts imported successfully", Toast.LENGTH_SHORT).show();
                        
                        // Force a complete refresh of positions
                        // But wait! We just imported new ones! So do NOT call DraggableButtonHelper.resetPositions(this), 
                        // just recreate the fragment so it reloads from the new SharedPreferences!
                        recreateCurrentFragment();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showMappingDialog() {
        if (!isConnected()) {
            Toast.makeText(this, "Connecting recommended to test mapping", Toast.LENGTH_SHORT).show();
        }
        // Suspend sensor updates so they don't interfere with mapping
        globalBuffer.setUpdatePitch(false);
        globalBuffer.setUpdateRoll(false);
        
        // Reset all continuous axes to 0 so the PC game doesn't read a "stuck" tilted axis
        globalBuffer.addData(MotionStatus.SetSteerAngle, 0.0f);
        globalBuffer.addData(MotionStatus.SetAccAngle, 0.0f);
        globalBuffer.addData(MotionStatus.SetLeftStickX, 0.0f);
        globalBuffer.addData(MotionStatus.SetLeftStickY, 0.0f);
        globalBuffer.addData(MotionStatus.SetRightStickX, 0.0f);
        globalBuffer.addData(MotionStatus.SetRightStickY, 0.0f);
        globalBuffer.addData(MotionStatus.SetLTValue, 0.0f);
        globalBuffer.addData(MotionStatus.SetRTValue, 0.0f);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        float d = getResources().getDisplayMetrics().density;
        int p24 = (int)(24 * d);
        int p16 = (int)(16 * d);
        int p12 = (int)(12 * d);
        int p8 = (int)(8 * d);

        android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(this);
        rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        rootLayout.setBackgroundResource(R.drawable.bg_mapping_dialog);
        rootLayout.setPadding(p24, p24, p24, p24);

        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Set Mapping: " + controllerMode.name());
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTextSize(18f);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 0, 0, p16);
        rootLayout.addView(title);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);

        // Define a helper to add mapping buttons
        class MappingAdder {
            void add(String name, MotionStatus status, float activeVal, int iconResId) {
                Button btn = new Button(MainActivity.this);
                btn.setText(name);
                btn.setTextColor(android.graphics.Color.WHITE);
                btn.setAllCaps(false);
                btn.setTextSize(14f);
                if (iconResId != 0) {
                    btn.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
                    btn.setCompoundDrawablePadding(p16);
                }
                btn.setPadding(p24, p12, p24, p12);
                btn.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                btn.setBackground(androidx.core.content.ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_mapping_btn));
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, p8);
                btn.setLayoutParams(params);

                btn.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.setPressed(true);
                        globalBuffer.addData(status, activeVal);
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        v.setPressed(false);
                        globalBuffer.addData(status, 0.0f);
                    }
                    return true;
                });
                layout.addView(btn);
            }

            void addButton(String name, MotionButton button, int iconResId) {
                Button btn = new Button(MainActivity.this);
                btn.setText(name);
                btn.setTextColor(android.graphics.Color.WHITE);
                btn.setAllCaps(false);
                btn.setTextSize(14f);
                if (iconResId != 0) {
                    btn.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
                    btn.setCompoundDrawablePadding(p16);
                }
                btn.setPadding(p24, p12, p24, p12);
                btn.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                btn.setBackground(androidx.core.content.ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_mapping_btn));
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, p8);
                btn.setLayoutParams(params);

                btn.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.setPressed(true);
                        globalBuffer.addData(button, true);
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        v.setPressed(false);
                        globalBuffer.addData(button, false);
                    }
                    return true;
                });
                layout.addView(btn);
            }
        }

        MappingAdder adder = new MappingAdder();

        if (controllerMode == ControllerMode.Default) {
            adder.add("Steer Left (Pitch Left)", MotionStatus.SetSteerAngle, 90.0f, R.drawable.ic_phone_steer_left);
            adder.add("Steer Right (Pitch Right)", MotionStatus.SetSteerAngle, -90.0f, R.drawable.ic_phone_steer_right);
            adder.add("Accel Forward (Roll Forward)", MotionStatus.SetAccAngle, -90.0f, R.drawable.ic_phone_accel_forward);
            adder.add("Accel Backward (Roll Backward)", MotionStatus.SetAccAngle, 90.0f, R.drawable.ic_phone_accel_backward);
            adder.addButton("Button X", MotionButton.X, R.drawable.ic_gamepad_btn);
            adder.addButton("Button Y", MotionButton.Y, R.drawable.ic_gamepad_btn);
            adder.addButton("Button A", MotionButton.A, R.drawable.ic_gamepad_btn);
            adder.addButton("Button B", MotionButton.B, R.drawable.ic_gamepad_btn);
        } else if (controllerMode == ControllerMode.Alter) {
            adder.add("Steer Left (Pitch Left)", MotionStatus.SetSteerAngle, 90.0f, R.drawable.ic_phone_steer_left);
            adder.add("Steer Right (Pitch Right)", MotionStatus.SetSteerAngle, -90.0f, R.drawable.ic_phone_steer_right);
            adder.add("Accel Forward (Roll Forward)", MotionStatus.SetAccAngle, -90.0f, R.drawable.ic_phone_accel_forward);
            adder.add("Accel Backward (Roll Backward)", MotionStatus.SetAccAngle, 90.0f, R.drawable.ic_phone_accel_backward);
            adder.add("Pedal LT", MotionStatus.SetLTValue, 1.0f, R.drawable.ic_pedal_btn);
            adder.add("Pedal RT", MotionStatus.SetRTValue, 1.0f, R.drawable.ic_pedal_btn);
            adder.addButton("Button LB", MotionButton.LB, R.drawable.ic_gamepad_btn);
            adder.addButton("Button RB", MotionButton.RB, R.drawable.ic_gamepad_btn);
            adder.addButton("Button X", MotionButton.X, R.drawable.ic_gamepad_btn);
            adder.addButton("Button Y", MotionButton.Y, R.drawable.ic_gamepad_btn);
            adder.addButton("Button A", MotionButton.A, R.drawable.ic_gamepad_btn);
            adder.addButton("Button B", MotionButton.B, R.drawable.ic_gamepad_btn);
        } else if (controllerMode == ControllerMode.GamePad) {
            adder.add("Left Stick Left", MotionStatus.SetLeftStickX, -1.0f, R.drawable.ic_stick_btn);
            adder.add("Left Stick Right", MotionStatus.SetLeftStickX, 1.0f, R.drawable.ic_stick_btn);
            adder.add("Left Stick Up", MotionStatus.SetLeftStickY, -1.0f, R.drawable.ic_stick_btn);
            adder.add("Left Stick Down", MotionStatus.SetLeftStickY, 1.0f, R.drawable.ic_stick_btn);
            adder.add("Right Stick Left", MotionStatus.SetRightStickX, -1.0f, R.drawable.ic_stick_btn);
            adder.add("Right Stick Right", MotionStatus.SetRightStickX, 1.0f, R.drawable.ic_stick_btn);
            adder.add("Right Stick Up", MotionStatus.SetRightStickY, -1.0f, R.drawable.ic_stick_btn);
            adder.add("Right Stick Down", MotionStatus.SetRightStickY, 1.0f, R.drawable.ic_stick_btn);
            adder.addButton("D-Pad UP", MotionButton.UP, R.drawable.ic_gamepad_btn);
            adder.addButton("D-Pad DOWN", MotionButton.DOWN, R.drawable.ic_gamepad_btn);
            adder.addButton("D-Pad LEFT", MotionButton.LEFT, R.drawable.ic_gamepad_btn);
            adder.addButton("D-Pad RIGHT", MotionButton.RIGHT, R.drawable.ic_gamepad_btn);
            adder.addButton("Button X", MotionButton.X, R.drawable.ic_gamepad_btn);
            adder.addButton("Button Y", MotionButton.Y, R.drawable.ic_gamepad_btn);
            adder.addButton("Button A", MotionButton.A, R.drawable.ic_gamepad_btn);
            adder.addButton("Button B", MotionButton.B, R.drawable.ic_gamepad_btn);
            adder.addButton("Button LB", MotionButton.LB, R.drawable.ic_gamepad_btn);
            adder.addButton("Button RB", MotionButton.RB, R.drawable.ic_gamepad_btn);
            adder.addButton("Button BACK", MotionButton.BACK, R.drawable.ic_gamepad_btn);
            adder.addButton("Button START", MotionButton.START, R.drawable.ic_gamepad_btn);
            adder.addButton("Button HOME", MotionButton.HOME, R.drawable.ic_gamepad_btn);
            adder.add("Trigger LT", MotionStatus.SetLTValue, 1.0f, R.drawable.ic_gamepad_btn);
            adder.add("Trigger RT", MotionStatus.SetRTValue, 1.0f, R.drawable.ic_gamepad_btn);
        } else if (controllerMode == ControllerMode.SteeringWheel) {
            adder.add("Steer Left (Pitch Left)", MotionStatus.SetSteerAngle, 90.0f, R.drawable.ic_phone_steer_left);
            adder.add("Steer Right (Pitch Right)", MotionStatus.SetSteerAngle, -90.0f, R.drawable.ic_phone_steer_right);
            adder.add("Pedal (Clutch)", MotionStatus.SetRightStickY, 1.0f, R.drawable.ic_pedal_btn);
            adder.add("Pedal (Acceleration)", MotionStatus.SetRTValue, 1.0f, R.drawable.ic_pedal_btn);
            adder.add("Pedal (Brake)", MotionStatus.SetLTValue, 1.0f, R.drawable.ic_pedal_btn);
            adder.addButton("D-Pad UP", MotionButton.UP, R.drawable.ic_gamepad_btn);
            adder.addButton("D-Pad DOWN", MotionButton.DOWN, R.drawable.ic_gamepad_btn);
            adder.addButton("D-Pad LEFT", MotionButton.LEFT, R.drawable.ic_gamepad_btn);
            adder.addButton("D-Pad RIGHT", MotionButton.RIGHT, R.drawable.ic_gamepad_btn);
            adder.addButton("Button X", MotionButton.X, R.drawable.ic_gamepad_btn);
            adder.addButton("Button Y", MotionButton.Y, R.drawable.ic_gamepad_btn);
            adder.addButton("Button A", MotionButton.A, R.drawable.ic_gamepad_btn);
            adder.addButton("Button B", MotionButton.B, R.drawable.ic_gamepad_btn);
            adder.addButton("Button LB", MotionButton.LB, R.drawable.ic_gamepad_btn);
            adder.addButton("Button RB", MotionButton.RB, R.drawable.ic_gamepad_btn);
        } else {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText("No controls to map in this mode.");
            tv.setTextColor(android.graphics.Color.WHITE);
            layout.addView(tv);
        }

        scrollView.addView(layout);
        rootLayout.addView(scrollView);

        builder.setView(rootLayout);
        builder.setPositiveButton("Done", (dialogInterface, i) -> dialogInterface.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.setOnDismissListener(     dialogInterface -> {
            // Restore sensor updates based on controller mode
            if (controllerMode == ControllerMode.Default) {
                globalBuffer.setUpdatePitch(true);
                globalBuffer.setUpdateRoll(true);
            } else if (controllerMode == ControllerMode.Alter) {
                globalBuffer.setUpdatePitch(true);
                globalBuffer.setUpdateRoll(useSensorPitch);
            } else if (controllerMode == ControllerMode.GamePad) {
                globalBuffer.setUpdatePitch(false);
                globalBuffer.setUpdateRoll(false);
            } else if (controllerMode == ControllerMode.SteeringWheel) {
                globalBuffer.setUpdatePitch(useSensorSteering);
                globalBuffer.setUpdateRoll(false);
            }
        });
        dialog.show();
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
    }


    public void recreateCurrentFragment() {
        if (controllerMode == ControllerMode.Default) {
            setFragment(R.id.nav_control_default_frag);
        } else if (controllerMode == ControllerMode.Alter) {
            setFragment(R.id.nav_control_alt_frag);
        } else if (controllerMode == ControllerMode.GamePad) {
            setFragment(R.id.nav_control_pad_frag);
        } else if (controllerMode == ControllerMode.SteeringWheel) {
            setFragment(R.id.nav_control_steeringwheel_frag);
        }
    }

    public boolean touchPedal(View view, MotionEvent e, MotionStatus status, MotionButton btn) {
        if (isEditMode) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                view.setTag(e.getRawY());
                if (btn != null) globalBuffer.addData(btn, true);
                return true;
            case MotionEvent.ACTION_MOVE:
                Object tag = view.getTag();
                if (tag == null) return false;
                float startY = (float) tag;
                float diff = startY - e.getRawY();
                float ratio = MathUtils.clamp(diff / slideDistancePx, 0.0f, 1.0f);
                view.setTranslationY(-ratio * slideDistancePx);
                globalBuffer.addData(status, ratio);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                view.setTranslationY(0);
                globalBuffer.addData(status, 0.0f);
                if (btn != null) globalBuffer.addData(btn, false);
                return true;
        }
        return false;
    }

    public boolean touchClutch(View v, MotionEvent e) { return touchPedal(v, e, MotionStatus.SetLTValue, MotionButton.LB); }
    public boolean touchBrake(View v, MotionEvent e) { return touchPedal(v, e, MotionStatus.SetAccRatio, MotionButton.RB); }
    public boolean touchGas(View v, MotionEvent e) { return touchPedal(v, e, MotionStatus.SetRTValue, null); }

    public boolean touchLT(View view, MotionEvent e) {
        ProgressBar bar = findViewById(R.id.progressBarLT);
        View btnLT = findViewById(R.id.buttonLT);
        if (btnLT == null) btnLT = view;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (releaseLTRunnable != null) handlerUpdateUI.removeCallbacks(releaseLTRunnable);
                view.setPressed(true);
                LTPressed = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                int[] loc = new int[2];
                btnLT.getLocationOnScreen(loc);
                float touchY = e.getRawY() - loc[1];
                float trackH = btnLT.getHeight();
                if (trackH > 0) {
                    LTRatio = MathUtils.clamp(1f - (touchY / trackH), 0.0f, 1.0f);
                    if (bar != null) bar.setProgress((int) (bar.getMax() * LTRatio));
                    globalBuffer.addData(MotionStatus.SetLTValue, LTRatio);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                LTPressed = false;
                animateReleaseLT(bar);
                return true;
        }
        return false;
    }

    private void animateReleaseLT(ProgressBar bar) {
        if (releaseLTRunnable != null) handlerUpdateUI.removeCallbacks(releaseLTRunnable);
        releaseLTRunnable = new Runnable() {
            @Override
            public void run() {
                if (LTPressed) return;
                if (LTRatio <= 0.10f) {
                    LTRatio = 0f;
                    if (bar != null) bar.setProgress(0);
                    globalBuffer.addData(MotionStatus.SetLTValue, 0f);
                    return;
                }
                LTRatio *= 0.65f;
                if (bar != null) bar.setProgress((int) (bar.getMax() * LTRatio));
                globalBuffer.addData(MotionStatus.SetLTValue, LTRatio);
                handlerUpdateUI.postDelayed(this, 16);
            }
        };
        handlerUpdateUI.post(releaseLTRunnable);
    }

    public boolean touchRT(View view, MotionEvent e) {
        ProgressBar bar = findViewById(R.id.progressBarRT);
        View btnRT = findViewById(R.id.buttonRT);
        if (btnRT == null) btnRT = view;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (releaseRTRunnable != null) handlerUpdateUI.removeCallbacks(releaseRTRunnable);
                view.setPressed(true);
                RTPressed = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                int[] loc = new int[2];
                btnRT.getLocationOnScreen(loc);
                float touchY = e.getRawY() - loc[1];
                float trackH = btnRT.getHeight();
                if (trackH > 0) {
                    RTRatio = MathUtils.clamp(1f - (touchY / trackH), 0.0f, 1.0f);
                    if (bar != null) bar.setProgress((int) (bar.getMax() * RTRatio));
                    globalBuffer.addData(MotionStatus.SetRTValue, RTRatio);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                RTPressed = false;
                animateReleaseRT(bar);
                return true;
        }
        return false;
    }

    private void animateReleaseRT(ProgressBar bar) {
        if (releaseRTRunnable != null) handlerUpdateUI.removeCallbacks(releaseRTRunnable);
        releaseRTRunnable = new Runnable() {
            @Override
            public void run() {
                if (RTPressed) return;
                if (RTRatio <= 0.10f) {
                    RTRatio = 0f;
                    if (bar != null) bar.setProgress(0);
                    globalBuffer.addData(MotionStatus.SetRTValue, 0f);
                    return;
                }
                RTRatio *= 0.65f;
                if (bar != null) bar.setProgress((int) (bar.getMax() * RTRatio));
                globalBuffer.addData(MotionStatus.SetRTValue, RTRatio);
                handlerUpdateUI.postDelayed(this, 16);
            }
        };
        handlerUpdateUI.post(releaseRTRunnable);
    }

    public boolean touchX(View view, MotionEvent e) { return touchButton(view, e, MotionButton.X); }
    public boolean touchY(View view, MotionEvent e) { return touchButton(view, e, MotionButton.Y); }
    public boolean touchA(View view, MotionEvent e) { return touchButton(view, e, MotionButton.A); }
    public boolean touchB(View view, MotionEvent e) { return touchButton(view, e, MotionButton.B); }
    public boolean touchLB(View view, MotionEvent e) { return touchButton(view, e, MotionButton.LB); }
    public boolean touchRB(View view, MotionEvent e) { return touchButton(view, e, MotionButton.RB); }
    public boolean touchBACK(View view, MotionEvent e) { return touchButton(view, e, MotionButton.BACK); }
    public boolean touchSTART(View view, MotionEvent e) { return touchButton(view, e, MotionButton.START); }
    public boolean touchUP(View view, MotionEvent e) { return touchButton(view, e, MotionButton.UP); }
    public boolean touchDOWN(View view, MotionEvent e) { return touchButton(view, e, MotionButton.DOWN); }
    public boolean touchLEFT(View view, MotionEvent e) { return touchButton(view, e, MotionButton.LEFT); }
    public boolean touchRIGHT(View view, MotionEvent e) { return touchButton(view, e, MotionButton.RIGHT); }
    public boolean touchHOME(View view, MotionEvent e) { return touchButton(view, e, MotionButton.HOME); }

    private boolean touchButton(View view, MotionEvent e, MotionButton button) {
        if (isEditMode) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                globalBuffer.addData(button, true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                globalBuffer.addData(button, false);
                return true;
        }
        return false;
    }

    public void checkSensor() {
        SensorManager test = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (test == null || test.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null || test.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            new AlertDialog.Builder(this).setTitle("Not Compatible").setMessage("Your phone does not have required sensors").setPositiveButton("OK", (dialog, which) -> System.exit(0)).show();
        }
    }

    public boolean isConnected() { return serviceConnection.connected; }
    public ConnectionMode getConnectionMode() { return serviceConnection.connectionMode; }
    
    public void setRadioGroupCallback() {
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        if (radioGroup == null) return;
        radioGroup.setOnCheckedChangeListener((group, i) -> {
            if (i == R.id.radioButtonBth) {
                serviceConnection.connectionMode = ConnectionMode.Bluetooth;
                Toast.makeText(this, "Mode: Bluetooth", Toast.LENGTH_SHORT).show();
            } else if (i == R.id.radioButtonWifi) {
                serviceConnection.connectionMode = ConnectionMode.Wifi;
                Toast.makeText(this, "Mode: Wifi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // check if bluetooth is supported and enabled
    private boolean checkBTH() {
        BluetoothAdapter test = BluetoothAdapter.getDefaultAdapter();
        if (test == null) {
            new AlertDialog.Builder(this).setTitle("Not Compatible").setMessage("Your phone does not support Bluetooth").setPositiveButton("OK", (dialog, which) -> System.exit(0)).show();
            return false;
        }

        // Check for Nearby Devices permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN}, 101);
                return false;
            }
        }

        if (!test.isEnabled()) {
            Toast.makeText(this, "Please enable bluetooth", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Tap Connect again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth permission is required for 'Nearby Devices'", Toast.LENGTH_LONG).show();
            }
        }
    }

    // check if wifi is connected
    private boolean checkWifi() {
        NetworkInfo test = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (test == null) {
            new AlertDialog.Builder(this).setTitle("Not Compatible").setMessage("Your phone cannot access wifi").setPositiveButton("OK", (dialog, which) -> System.exit(0)).show();
            return false;
        }
        if (!test.isConnected()) {
            Toast.makeText(this, "Please connect Wifi to PC", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // connect button callback
    public void connectionButtonOnClick(View view) {
        boolean connected = isConnected();
        // if already connected, start a thread to disconnect
        if (connected) {
            if (threadDisconnect != null && threadDisconnect.isAlive()) {
                Toast.makeText(this, "Already Disconnecting", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Disconnecting...", Toast.LENGTH_SHORT).show();
            threadDisconnect = new Thread(() -> {
                resetController();
                serviceConnection.disconnect();
                runOnUiThread(() -> {
                    if (!isConnected()) {
                        ((Button) view).setText(R.string.buttonConnect);
                        RadioGroup group = findViewById(R.id.radioGroup);
                        if (group != null) {
                            for (int i = 0; i < group.getChildCount(); i++) {
                                group.getChildAt(i).setEnabled(true);
                            }
                        }
                    } else ((Button) view).setText(R.string.buttonDisconnect);
                });
            });
            threadDisconnect.start();
        }
        // if not connected, start a thread to connect
        else {
            if (threadConnect != null && threadConnect.isAlive()) {
                Toast.makeText(this, "Already Connecting", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
            RadioGroup group = findViewById(R.id.radioGroup);
            if (group != null) {
                if (group.getCheckedRadioButtonId() == R.id.radioButtonBth && !checkBTH()) return;
                else if (group.getCheckedRadioButtonId() == R.id.radioButtonWifi && !checkWifi())
                    return;
            }
            threadConnect = new Thread(() -> {
                String result = serviceConnection.connect();
                runOnUiThread(() -> {
                    if (result.length() > 0)
                        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                    if (isConnected()) {
                        if (group != null) {
                            for (int i = 0; i < group.getChildCount(); i++) {
                                group.getChildAt(i).setEnabled(false);
                            }
                        }
                        ((Button) view).setText(R.string.buttonDisconnect);
                    } else ((Button) view).setText(R.string.buttonConnect);
                });
            });
            threadConnect.start();
        }
    }

    public void resetController() {
        globalBuffer.addData(MotionStatus.ResetSteerAngle, 0.0f);
        globalBuffer.addData(MotionStatus.ResetAccAngle, 0.0f);
    }

    public void moveLeftStick(int angle, int strength) {
        Pair<Float, Float> XY = computeJoyStickXY(angle, strength);
        globalBuffer.addData(MotionStatus.SetLeftStickX, XY.first);
        globalBuffer.addData(MotionStatus.SetLeftStickY, XY.second);
    }

    public void moveRightStick(int angle, int strength) {
        Pair<Float, Float> XY = computeJoyStickXY(angle, strength);
        globalBuffer.addData(MotionStatus.SetRightStickX, XY.first);
        globalBuffer.addData(MotionStatus.SetRightStickY, XY.second);
    }

    private Pair<Float, Float> computeJoyStickXY(int angle, int strength) {
        double r = Math.toRadians(angle);
        double s = strength / 100.0;
        return new Pair<>((float) (Math.cos(r) * s), (float) (Math.sin(r) * s));
    }

    public void onDestroy() {
        super.onDestroy();
        serviceMotion.stop();
    }
}
