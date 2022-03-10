package com.example.seismo;

import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;    // 传感器管理器
    Button button;

    LinearLayout xCurve;  // declare X axis object
    LinearLayout yCurve; // declare Y axis object
    LinearLayout zCurve; // declare Z axis object

    private GraphicalView xView, yView, zView;//三个轴对应的图表
    private ChartService xService, yService, zService;
    private Timer timer;
    private float [] values={0, 0, 0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.goto_ppg);
        xCurve = findViewById(R.id.x_curve);  // create X axis object
        yCurve = findViewById(R.id.y_curve); // create Y axis object
        zCurve = findViewById(R.id.z_curve); // create Z axis object

        xService = new ChartService(this);
        xService.setXYMultipleSeriesDataset("加速度计X轴实时数据");
        xService.setXYMultipleSeriesRenderer(10, 1, "X轴曲线",
                "时间", "X轴加速度", Color.RED, Color.RED, Color.RED, Color.BLACK);
        xView = xService.getGraphicalView();

        yService = new ChartService(this);
        yService.setXYMultipleSeriesDataset("加速度计Y轴实时数据");
        yService.setXYMultipleSeriesRenderer(10, 1, "Y轴曲线",
                "时间", "Y轴加速度", Color.RED, Color.RED, Color.BLUE, Color.BLACK);
        yView = yService.getGraphicalView();

        zService = new ChartService(this);
        zService.setXYMultipleSeriesDataset("加速度计Z轴实时数据");
        zService.setXYMultipleSeriesRenderer(10, 1, "Z轴曲线",
                "时间", "Z轴加速度", Color.RED, Color.RED, Color.BLACK, Color.BLACK);
        zView = zService.getGraphicalView();

        // 将三个轴对应的图表添加到布局容器中
        xCurve.addView(xView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        yCurve.addView(yView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        zCurve.addView(zView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BPActivity.class);
                startActivity(intent);
                MainActivity.this.finish();
            }
        });

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendMessage(handler.obtainMessage());
            }
        }, 1, 100);
    }

    private double t = 0;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            xService.updateChart(t, values[0]);
            yService.updateChart(t, values[1]);
            zService.updateChart(t, values[2]);
            t += 0.1;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 为系统的加速度计传感器注册监听器
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        // 取消注册
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    // 当精度发生变化时调用
    public void onAccuracyChanged(Sensor sensor, int event) {

    }

    // 当sensor事件发生时候调用
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            values = event.values;
        }
    }
}
