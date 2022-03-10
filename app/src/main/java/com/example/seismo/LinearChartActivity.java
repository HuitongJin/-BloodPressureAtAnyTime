package com.example.seismo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.widget.LinearLayout;

import org.achartengine.GraphicalView;

import java.util.Timer;
import java.util.TimerTask;

public class LinearChartActivity extends AppCompatActivity {

    private LinearLayout mLeftCurveLayout;//存放左图表的布局容器
    private LinearLayout mRightCurveLayout;//存放右图表的布局容器
    private GraphicalView mView, mView2;//左右图表
    private ChartService mService, mService2;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linear_chart);

        mLeftCurveLayout = (LinearLayout)findViewById(R.id.left_temperature_curve);
        mRightCurveLayout = (LinearLayout)findViewById(R.id.right_temperature_curve);

        mService = new ChartService(this);
        mService.setXYMultipleSeriesDataset("左温度曲线");
        mService.setXYMultipleSeriesRenderer(10, 100, "左温度曲线",
                "时间", "温度",Color.RED, Color.RED, Color.RED, Color.BLACK);
        mView = mService.getGraphicalView();

        mService2 = new ChartService(this);
        mService2.setXYMultipleSeriesDataset("右温度曲线");
        mService2.setXYMultipleSeriesRenderer(10, 100, "右温度曲线", "时间", "温度",
                Color.RED, Color.RED, Color.RED, Color.BLACK);
        mView2 = mService2.getGraphicalView();

        // 将左右图表添加到布局容器中
        mLeftCurveLayout.addView(mView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        mRightCurveLayout.addView(mView2, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendMessage(handler.obtainMessage());
            }
        }, 10, 100);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private double t = 0;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            mService.updateChart(t, Math.random() * 100);
            mService2.updateChart(t, Math.random() * 100);
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
}
