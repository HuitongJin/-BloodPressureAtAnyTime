package com.example.seismo;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import static java.lang.Math.ceil;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.seismo.Math.Math.Fft;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BPActivity extends AppCompatActivity{

    private static final String TAG = "HeartRateMonitor";
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static PowerManager.WakeLock wakeLock = null;
    private TextView SPTX = null;
    private TextView DPTX = null;
    private TextView beatsTX = null;

    // Toast
    private Toast mainToast;

    // ProgressBar
    private ProgressBar ProgBP;
    public int ProgP = 0;
    public int inc = 0;

    // Beats variable
    public int Beats = 0;
    public double bufferAvgB = 0;

    // Freq + timer variable
    private static long startTime = 0;
    private double SamplingFreq;

    // BloodPressure variables
    public double Gen = 1, Agg = 21, Hei = 166.5, Wei = 47.5;
    public double Q = 4.5;
    private static int SP = 0, DP = 0;

    // Arraylist
    public ArrayList<Double> GreenAvgList = new ArrayList<>();
    public ArrayList<Double> RedAvgList = new ArrayList<>();
    public int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bp_2);

        // XML connecting
        SPTX = findViewById(R.id.bp1);
        DPTX = findViewById(R.id.bp2);
        beatsTX = findViewById(R.id.beats);
        preview = findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        ProgBP = findViewById(R.id.BPPB);
        ProgBP.setProgress(0);

        // WakeLock Initialization : Forces the phone to stay On
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "app:DoNotDimScreen");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wakeLock.acquire();
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onPause() {
        super.onPause();
        wakeLock.release();
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) throw new NullPointerException();
            Camera.Size size =  cam.getParameters().getPreviewSize();
            if (size == null) throw new NullPointerException();

            if (! processing.compareAndSet(false, true)) return;

            int width = size.width;
            int height = size.height;

            double GreenAvg;
            double RedAvg;

            GreenAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 3); // 1表示红色， 2表示蓝色，3表示绿色
            RedAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 1);

            GreenAvgList.add(GreenAvg);
            RedAvgList.add(RedAvg);

            ++ counter; // counts number of frames in30 seconds

            //To check if we got a good red intensity to process if not return to the condition and set it again until we get a good red intensity
            if (RedAvg < 200) {
                inc  = 0;
                ProgP = inc;
                counter = 0;
                ProgBP.setProgress(ProgP);
                processing.set(false);
            }

            long endTime  =System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            if (totalTimeInSecs >= 30) {
                Double[] Green = GreenAvgList.toArray(new Double[GreenAvgList.size()]);
                Double[] Red = RedAvgList.toArray(new Double[RedAvgList.size()]);

                SamplingFreq = (counter / totalTimeInSecs);

                double HRFreq = Fft.FFT(Green, counter, SamplingFreq);  // 发送Green数组，并获取他对应的心率
                double bpm = (int)ceil(HRFreq * 60);
                double HR1Freq = Fft.FFT(Red, counter, SamplingFreq);
                double bpm1 = (int)ceil(HR1Freq * 60);

                // The following code is to make sure that if the heartrate from red and green intensities are reasonable
                // take the average between them, otherwise take the green or red if one of them is good

                if (bpm > 45 || bpm < 200) {
                    if (bpm1 > 45 || bpm1 < 200) {
                        bufferAvgB = (bpm + bpm1) / 2;
                    }else {
                        bufferAvgB = bpm;
                    }
                }else if (bpm1 > 45 || bpm1 < 200) {
                    bufferAvgB = bpm1;
                }

                if (bufferAvgB < 45 || bufferAvgB > 200) {
                    inc = 0;
                    ProgP = inc;
                    ProgBP.setProgress(ProgP);
                    mainToast = Toast.makeText(getApplicationContext(), "Measurement Failed", Toast.LENGTH_SHORT);
                    mainToast.show();
                    startTime = System.currentTimeMillis();
                    RedAvgList.clear();
                    GreenAvgList.clear();
                    counter = 0;
                    processing.set(false);
                    return;
                }

                Beats  =(int)bufferAvgB;

                double ROB = 18.5;
                double ET = (364.5 - 1.23*Beats);
                double BSA = 0.007184 * (Math.pow(Wei, 0.425)) * (Math.pow(Hei, 0.725));
                double SV = (-6.6 + (0.25 * (ET - 35)) - (0.62 * Beats) + (40.4 * BSA) - (0.51 * Agg));
                double PP = SV / ((0.013 * Wei - 0.007 * Agg - 0.004 * Beats) + 1.307);
                double MPP = Q * ROB;

                SP = (int)(MPP + 3/2 * PP);
                DP = (int)(MPP - PP/3);
            }

            if (SP != 0 && DP != 0) {
                String text = "高压：" + SP;
                String text1 = "低压：" + DP;
                String text2 = "心率：" + Beats;
                SPTX.setText(text);
                DPTX.setText(text1);
                beatsTX.setText(text2);
            }

            if (RedAvg != 0) {
                ProgP = inc ++ / 34;
                ProgBP.setProgress(ProgP);
            }
            processing.set(false);
        }
    };

    private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);
            }catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

            Camera.Size size = getSmallestPreviewSize(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
            }

            camera.setParameters(parameters);
            camera.startPreview();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

        }
    };

    private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                }else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea < resultArea) result = size;
                }
            }
        }
        return result;
    }
}
