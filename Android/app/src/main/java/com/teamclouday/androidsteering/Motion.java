// component for collecting motion information

package com.teamclouday.androidsteering;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Process;
import android.util.Log;

import androidx.core.math.MathUtils;

enum MotionStatus {
    SetSteerAngle(0),
    SetAccAngle(1),
    ResetSteerAngle(2),
    ResetAccAngle(3),
    SetAccRatio(4),
    SetLeftStickX(5),
    SetLeftStickY(6),
    SetRightStickX(7),
    SetRightStickY(8),
    SetLTValue(9),
    SetRTValue(10);

    private final int val;

    MotionStatus(int v) {
        val = v;
    }

    public int getVal() {
        return val;
    }
}

enum MotionButton {
    X(0),
    Y(1),
    A(2),
    B(3),
    LB(4),
    RB(5),
    UP(6),
    DOWN(7),
    RIGHT(8),
    LEFT(9),
    BACK(10),
    START(11),
    HOME(12);

    private final int val;

    MotionButton(int v) {
        val = v;
    }

    public int getVal() {
        return val;
    }
}

public class Motion implements SensorEventListener {
    static class MyMove {
        boolean MotionButton; // is it a button motion?
        int MotionStatus; // positive number for related status
        float data; // moving data

        public MyMove(boolean type, int status, float d) {
            MotionButton = type;
            MotionStatus = status;
            data = d;
        }
    }

    private final SensorManager sensorManager;

    private final Sensor accSensor;
    private final Sensor magSensor;

    private final MainActivity mainActivity;
    private final Connection.MyBuffer globalBuffer;

    private final float[] accReading = new float[3];
    private final float[] magReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientation = new float[3];

    private volatile float motionPitch = 0.0f;
    private volatile float motionRoll = 0.0f;

    private Thread dataSubmitThread;
    private volatile boolean dataSubmitShouldStop;
    private final int MAX_WAIT_TIME = 1000;
    private final int DATA_UPDATE_FREQ = 5; // wait for milliseconds for next update

    public Motion(MainActivity activity, Connection.MyBuffer buffer) {
        mainActivity = activity;
        globalBuffer = buffer;
        sensorManager = (SensorManager) mainActivity.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    // start sensor callback
    public void start() {
        if (dataSubmitThread != null && dataSubmitThread.isAlive()) {
            stop();
        }

        // sample period is set to 10ms
        if (!sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME))
            Log.d(mainActivity.getString(R.string.logTagMotion), "Failed to register accelerometer");
        else if (!sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_GAME))
            Log.d(mainActivity.getString(R.string.logTagMotion), "Failed to register magnetic field");
        else
            Log.d(mainActivity.getString(R.string.logTagMotion), "Sensor listener registered");

        // start data submission thread
        dataSubmitShouldStop = false;

        dataSubmitThread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
            while (!dataSubmitShouldStop) {
                try {
                    globalBuffer.addData(readPitch(), readRoll());
                    Thread.sleep(DATA_UPDATE_FREQ);
                } catch (InterruptedException e) {
                    Log.d(mainActivity.getString(R.string.logTagMotion), e.toString());
                    break;
                }
            }
        });
        dataSubmitThread.start();
    }

    // stop sensor callback
    public void stop() {
        sensorManager.unregisterListener(this);
        Log.d(mainActivity.getString(R.string.logTagMotion), "Sensor listener unregistered");

        dataSubmitShouldStop = true;
        if (dataSubmitThread != null && dataSubmitThread.isAlive()) {
            try {
                dataSubmitThread.join(MAX_WAIT_TIME);
            } catch (InterruptedException e) {
                Log.d(mainActivity.getString(R.string.logTagMotion), e.toString());
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) return;
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, accReading, 0, accReading.length);
            updatePose();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magReading, 0, magReading.length);
            updatePose();
        }
    }

    // update current pitch roll
    private void updatePose() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accReading, magReading);
        SensorManager.getOrientation(rotationMatrix, orientation);

        // Stable Gravity-based Math (No Gimbal Lock)
        float x = accReading[0];
        float y = accReading[1];
        float z = accReading[2];

        // 1. Steering (Pitch variable)
        // Calculated by the angle of gravity on the X-Y plane of the device.
        double steerAngle = 0;
        if (x >= 0) {
            // Landscape (Home button on right)
            steerAngle = Math.toDegrees(Math.atan2(y, x));
        } else {
            // Reverse Landscape (Home button on left)
            steerAngle = -Math.toDegrees(Math.atan2(y, -x));
        }

        // 2. Acceleration (Roll variable)
        // Calculated by how much gravity is pulling on the Z-axis (front/back tilt).
        double accelAngle = Math.toDegrees(Math.asin(MathUtils.clamp(z / 9.81f, -1.0f, 1.0f)));

        updatePitch((float) steerAngle);
        updateRoll((float) accelAngle);
    }

    // update pitch
    private synchronized void updatePitch(float newPitch) {
        motionPitch = newPitch;
    }

    // read pitch
    public synchronized float readPitch() {
        return motionPitch;
    }

    // update roll
    private synchronized void updateRoll(float newRoll) {
        motionRoll = newRoll;
    }

    // read roll
    public synchronized float readRoll() {
        return motionRoll;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
