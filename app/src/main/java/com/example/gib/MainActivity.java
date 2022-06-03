package com.example.gib;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public TextView tv_gyro_x, tv_gyro_y, tv_gyro_z;        // 一大堆显示控件
    public TextView tv_acce_x, tv_acce_y, tv_acce_z;
    public TextView tv_magn_x, tv_magn_y, tv_magn_z;
    public TextView tv_atti_x, tv_atti_y, tv_atti_z;
    public TextView tv_veln_x, tv_veln_y, tv_veln_z;
    public TextView tv_posi_x, tv_posi_y, tv_humidity;
    public TextView tv_time, tv_baro, tv_temperature;
    public TextView tv_utc_time, tv_gnss_lat, tv_gnss_lng, tv_gnss_alt, tv_gnss_vhor, tv_gnss_head;    // GNSS数据控件
    public TextView tv_gpstow, tv_bds, tv_gps, tv_gal, tv_glo, tv_gpsweek;              // GNSS原始数据控件
    public Button btn_nav, btn_save;               // 三个按钮 先不做操作
    public Button btn_record, btn_recordimu,btn_uimu;                //记录数据
    public static SensorManager sensorManager_IMU;          // Sensor Manager对象
    public static Sensor sensor_gyro, sensor_acce, sensor_magn, sensor_att, sensor_pre,sensor_tem;
    public static Sensor sensor_u_gyro,sensor_u_acce,sensor_u_mag;//未标定的
    public static Sensor sensor_l_acce;//线性加速度计
    public static Sensor sensor_humidity;//湿度


    public static SensorEventListener sensorEventListener;      // 手机传感器监听器
    public View.OnClickListener buttonListener;                 // 按键监听器


    // GNSS
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLocation;
    // 记录数据标识符
    private boolean isRecordImu;
    private boolean isRecordGnss;
    private boolean isRecorduImu;

    private int numbergyro = 0;
    private int numberacc = 0;
    private int numbermag = 0;
    private int numberpre = 0;
    private static final int OPEN_SET_REQUEST_CODE = 100;

    // 记录文件
    public BufferedWriter mFileWriterSensors = null;
    public BufferedWriter mFileWriterUSensors = null;
    public BufferedWriter mFileWriterGnss = null;
    public File mFileSensors = null;
    public File mFileUSensors = null;
    public File mFileGnss = null;
    private final Object mFileLock = new Object();

    private static final String COMMENT_START = "# ";
    private static final String VERSION_TAG = "Version: ";

    private final StringBuilder mStringBuilder = new StringBuilder();
    private static final char RECORD_DELIMITER = ',';

    @RequiresApi(api = Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*              控件初始化               */
        tv_gyro_x = findViewById(R.id.tv_gyro_x);
        tv_gyro_y = findViewById(R.id.tv_gyro_y);
        tv_gyro_z = findViewById(R.id.tv_gyro_z);
        tv_acce_x = findViewById(R.id.tv_acce_x);
        tv_acce_y = findViewById(R.id.tv_acce_y);
        tv_acce_z = findViewById(R.id.tv_acce_z);
        tv_magn_x = findViewById(R.id.tv_magn_x);
        tv_magn_y = findViewById(R.id.tv_magn_y);
        tv_magn_z = findViewById(R.id.tv_magn_z);
        tv_atti_x = findViewById(R.id.tv_atti_x);
        tv_atti_y = findViewById(R.id.tv_atti_y);
        tv_atti_z = findViewById(R.id.tv_atti_z);
        tv_veln_x = findViewById(R.id.tv_veln_x);
        tv_veln_y = findViewById(R.id.tv_veln_y);
        tv_veln_z = findViewById(R.id.tv_veln_z);
        tv_posi_x = findViewById(R.id.tv_posi_x);
        tv_posi_y = findViewById(R.id.tv_posi_y);
        tv_humidity = findViewById(R.id.tv_humidity);
        tv_time = findViewById(R.id.tv_time);
        tv_baro = findViewById(R.id.tv_baro);
        tv_temperature = findViewById(R.id.tv_temperature);

        btn_nav = findViewById(R.id.btn_nav);
        btn_save = findViewById(R.id.btn_save);
        btn_uimu = findViewById(R.id.btn_uimu);

        btn_record = findViewById(R.id.btn_record);
        btn_recordimu = findViewById(R.id.btn_recordimu);

        tv_utc_time = findViewById(R.id.tv_utc_time);
        tv_gnss_lat = findViewById(R.id.tv_gnss_lat);
        tv_gnss_lng = findViewById(R.id.tv_gnss_lng);
        tv_gnss_alt = findViewById(R.id.tv_gnss_alt);
        tv_gnss_vhor = findViewById(R.id.tv_gnss_vhor);
        tv_gnss_head = findViewById(R.id.tv_gnss_head);

        tv_gpstow = findViewById(R.id.tv_gpstow);
        tv_gpsweek = findViewById(R.id.tv_gpsweek);
        tv_gps = findViewById(R.id.tv_gpsnumber);
        tv_bds = findViewById(R.id.tv_bdsnumber);
        tv_glo = findViewById(R.id.tv_glonumber);
        tv_gal = findViewById(R.id.tv_galnumber);
        /* ******************************************************************* */

        //这个是解决主线程没有网络连接问题的代码
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // 请求GNSS权限
        int hasPermission = ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    OPEN_SET_REQUEST_CODE);
        }
//        //检测是否有写的权限
//        String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
//        int permission = ActivityCompat.checkSelfPermission(getApplication(), "android.permission.WRITE_EXTERNAL_STORAGE");
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//            // 没有写的权限，去申请写的权限，会弹出对话框
//            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 1);
//        }
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED.equals(state)) {
//            baseDirectory = new File(Environment.getExternalStorageDirectory(), "gnss_log_bai");
//            baseDirectory.mkdirs();
//        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//            Log.i(TAG, "Cannot write to external storage.");
//            return;
//        } else {
//            Log.i(TAG, "Cannot write to external storage.");
//            return;
//        }


        // 记录数据标识符初始化
        isRecordGnss = false;
        isRecordImu = false;
        isRecorduImu=false;

        //初始化GNSS对象
        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onLocationChanged(@NonNull Location location) {
                tv_utc_time.setText(String.valueOf(location.getTime() / 1000 % 10000));
                tv_gnss_lat.setText(String.format("%.4f", location.getLatitude()));
                tv_gnss_lng.setText(String.format("%.4f", location.getLongitude()));
                tv_gnss_alt.setText(String.format("%.4f", location.getAltitude()));
                tv_gnss_vhor.setText(String.format("%.4f", location.getSpeed()));
                synchronized (mFileLock) {
                    if (isRecordGnss) {
                        String locationStream = String.format(Locale.US, "Fix,%s,%f,%f,%f,%f,%f,%d",
                                location.getProvider(),
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAltitude(),
                                location.getSpeed(),
                                location.getAccuracy(),
                                location.getTime());
                        try {
                            mFileWriterGnss.write(locationStream);
                            mFileWriterGnss.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        registerLocation();
        registerGnssMeasurements();


        // 初始化Sensor对象
        sensorManager_IMU = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // 接收传感器数据
        sensorEventListener = new SensorEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @SuppressLint("DefaultLocale")
            @Override
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_GYROSCOPE:         // 陀螺仪 原始数据
                        numbergyro++;
                        if (numbergyro > 50) {
                            tv_gyro_x.setText(String.format("%8.4f", event.values[0]));
                            tv_gyro_y.setText(String.format("%8.4f", event.values[1]));
                            tv_gyro_z.setText(String.format("%8.4f", event.values[2]));
                            numbergyro = 0;
                        }
                        break;
                    case Sensor.TYPE_ACCELEROMETER:     // 加速度 包括重力 原始数据
                        numberacc++;
                        if (numberacc > 50) {
                            tv_acce_x.setText(String.format("%8.4f", event.values[0]));
                            tv_acce_y.setText(String.format("%8.4f", event.values[1]));
                            tv_acce_z.setText(String.format("%8.4f", event.values[2]));
                            numberacc = 0;
                        }
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:    // 磁场强度
                        numbermag++;
                        if (numbermag > 50) {
                            tv_magn_x.setText(String.format("%8.4f", event.values[0]));
                            tv_magn_y.setText(String.format("%8.4f", event.values[1]));
                            tv_magn_z.setText(String.format("%8.4f", event.values[2]));
                            numbermag = 0;
                        }
                        break;
                    case Sensor.TYPE_ROTATION_VECTOR:   // 姿态四元数（Android计算姿态结果）
                        break;
                    case Sensor.TYPE_PRESSURE:          // 气压
                        numberpre++;
                        if (numberpre > 10) {
                            tv_baro.setText(String.format("%8.4f", event.values[0]));
                            numberpre = 0;
                        }
                        break;
                    case Sensor.TYPE_AMBIENT_TEMPERATURE: //环境温度 已弃用
                        tv_temperature.setText(String.format("%8.4f", event.values[0]));
                        break;
                    case Sensor.TYPE_RELATIVE_HUMIDITY:
                        tv_humidity.setText(String.format("%8.4f", event.values[0]));
                }
                // 把全部的传感器数据写入到文件中
                synchronized (mFileLock) {
                    if (isRecordImu) {
                        if(mFileWriterSensors==null)
                        {
                            return;
                        }
                        mStringBuilder.setLength(0);
                        mStringBuilder.append(event.sensor.getStringType().substring(event.sensor.getStringType().lastIndexOf('.') + 1)).append(RECORD_DELIMITER);
                        long Utc_timestamp = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
                        mStringBuilder.append(Utc_timestamp).append(RECORD_DELIMITER);
                        mStringBuilder.append(event.timestamp).append(RECORD_DELIMITER);
                        mStringBuilder.append(java.util.Arrays.toString(event.values).replaceAll("[] ]", "").replace("[", ""));
                        switch (event.sensor.getType())
                        {
                            case Sensor.TYPE_GYROSCOPE:
                            case Sensor.TYPE_ACCELEROMETER:
                            case Sensor.TYPE_MAGNETIC_FIELD:
                            case Sensor.TYPE_PRESSURE:
                                try {
                                    mFileWriterSensors.write(mStringBuilder.toString());
                                    mFileWriterSensors.newLine();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                    if (isRecorduImu) {
                        if(mFileWriterUSensors==null)
                        {
                            return;
                        }
                        mStringBuilder.setLength(0);
                        mStringBuilder.append(event.sensor.getStringType().substring(event.sensor.getStringType().lastIndexOf('.') + 1)).append(RECORD_DELIMITER);
                        long Utc_timestamp = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
                        mStringBuilder.append(Utc_timestamp).append(RECORD_DELIMITER);
                        mStringBuilder.append(event.timestamp).append(RECORD_DELIMITER);
                        mStringBuilder.append(java.util.Arrays.toString(event.values).replaceAll("[] ]", "").replace("[", ""));
                        switch (event.sensor.getType())
                        {
                            case Sensor.TYPE_ROTATION_VECTOR:
                            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                            case Sensor.TYPE_RELATIVE_HUMIDITY:
                                try {
                                    mFileWriterUSensors.write(mStringBuilder.toString());
                                    mFileWriterUSensors.newLine();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        // 注册传感器
        registerSensors();
        //未经补偿的传感器
        registerSensors_UNCALIBRATED();

        // 开始记录数据
        buttonListener = new View.OnClickListener() {
            @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_recordimu:
                        Log.i(TAG, "Click -> Record (IMU+PRE+TEM+ORI)");
                        if (!isRecordImu) {
                            isRecordImu = true;
                            animationClickStart();
                            try {
                                startRecordImu();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            isRecordImu = false;
                            animationClickStop();
                            try {
                                stopRecordImu();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case R.id.btn_uimu:
                        if (!isRecorduImu) {
                            isRecorduImu = true;
                            Log.i(TAG, "Click -> Record (u IMU)");
                            //保持屏幕常亮
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            btn_uimu.setText(R.string.stop);
                            try {
                                startRecorduImu();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            isRecorduImu = false;
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            btn_uimu.setText("记录IMU未标定");
                            try {
                                stopRecorduImu();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case R.id.btn_record:
                        if (!isRecordGnss) {
                            isRecordGnss = true;
                            Log.i(TAG, "Click -> Record (GNSS)");
                            //保持屏幕常亮
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            btn_record.setText(R.string.stop);
                            try {
                                startRecordGnss();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            isRecordGnss = false;
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            btn_record.setText("记录GNSS");
                            try {
                                stopRecordGnss();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
            }
        };
        btn_recordimu.setOnClickListener(buttonListener);
        btn_record.setOnClickListener(buttonListener);
        btn_uimu.setOnClickListener(buttonListener);
    }

    private void startRecorduImu() throws IOException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        String fileNameSensor = String.format("%s_%s.txt", "u_sensor_logger_", formatter.format(now));
        mFileUSensors = new File("/storage/emulated/0/Documents", fileNameSensor);
        mFileWriterUSensors = new BufferedWriter(new FileWriter(mFileUSensors));
        Toast.makeText(getApplication(), "USensors File opened: " + mFileUSensors.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }
    private void stopRecorduImu() throws IOException {
        mFileWriterUSensors.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't receive any more updates from either sensor.
//        sensorManager_IMU.unregisterListener(sensorEventListener);
//        mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsEvent);
    }
    private final GnssMeasurementsEvent.Callback gnssMeasurementsEvent = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            super.onGnssMeasurementsReceived(eventArgs);

            synchronized (mFileLock) {
                if (mFileWriterGnss == null) {
                    return;
                }
                if (isRecordGnss) {
                    //long Utc_timestamp = System.currentTimeMillis() + (eventArgs.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
                    GnssClock gnssClock = eventArgs.getClock();
                    for (GnssMeasurement gnssMeasurement : eventArgs.getMeasurements()) {
                        try {
                            writeGnssMeasurementToFile(gnssClock, gnssMeasurement);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    private void registerSensors()
    {
        // 注册传感器
        sensor_gyro = sensorManager_IMU.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Log.i(TAG, "GYRO name:"+sensor_gyro.getName()+",ventor:"+sensor_gyro.getVendor()+",version:"+sensor_gyro.getVersion());
        sensor_acce = sensorManager_IMU.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Log.i(TAG, "ACCE name:"+sensor_acce.getName()+",ventor:"+sensor_acce.getVendor()+",version:"+sensor_acce.getVersion());
        sensor_magn = sensorManager_IMU.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Log.i(TAG, "MAG name:"+sensor_magn.getName()+",ventor:"+sensor_magn.getVendor()+",version:"+sensor_magn.getVersion());
        sensor_att = sensorManager_IMU.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensor_pre = sensorManager_IMU.getDefaultSensor(Sensor.TYPE_PRESSURE);
        Log.i(TAG, "PRE name:"+sensor_pre.getName()+",ventor:"+sensor_pre.getVendor()+",version:"+sensor_pre.getVersion());
        sensor_tem=sensorManager_IMU.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        sensorManager_IMU.registerListener(sensorEventListener, sensor_gyro, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager_IMU.registerListener(sensorEventListener, sensor_acce, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager_IMU.registerListener(sensorEventListener, sensor_magn, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager_IMU.registerListener(sensorEventListener, sensor_att, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager_IMU.registerListener(sensorEventListener, sensor_pre, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager_IMU.registerListener(sensorEventListener,sensor_tem,SensorManager.SENSOR_DELAY_FASTEST);

    }
    private void registerSensors_UNCALIBRATED()
    {
        //未经补偿的传感器
        sensor_u_gyro=sensorManager_IMU.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        sensor_u_acce=sensorManager_IMU.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
        sensor_u_mag=sensorManager_IMU.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

        sensorManager_IMU.registerListener(sensorEventListener,sensor_u_gyro,SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager_IMU.registerListener(sensorEventListener,sensor_u_acce,SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager_IMU.registerListener(sensorEventListener,sensor_u_mag,SensorManager.SENSOR_DELAY_FASTEST);

        sensor_l_acce=sensorManager_IMU.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager_IMU.registerListener(sensorEventListener,sensor_l_acce,SensorManager.SENSOR_DELAY_FASTEST);

        //湿度
        sensor_humidity=sensorManager_IMU.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        sensorManager_IMU.registerListener(sensorEventListener,sensor_humidity,SensorManager.SENSOR_DELAY_FASTEST);
    }


    @SuppressLint("ResourceAsColor")
    private void animationClickStart() {
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btn_recordimu.setText(R.string.stop);
    }

    @SuppressLint("SetTextI18n")
    private void animationClickStop() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btn_recordimu.setText("记录IMU");
    }

    private void startRecordImu() throws IOException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        String fileNameSensor = String.format("%s_%s.txt", "sensor_logger_", formatter.format(now));
        mFileSensors = new File("/storage/emulated/0/Documents", fileNameSensor);
        mFileWriterSensors = new BufferedWriter(new FileWriter(mFileSensors));
        Toast.makeText(getApplication(), "Sensors File opened: " + mFileSensors.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }

    private void startRecordGnss() throws IOException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        String fileNameGnss = String.format("%s_%s.txt", "gnss_logger_", formatter.format(now));
        mFileGnss = new File("/storage/emulated/0/Documents", fileNameGnss);
        mFileWriterGnss = new BufferedWriter(new FileWriter(mFileGnss));
        Toast.makeText(getApplication(), "Raw GNSS File opened: " + mFileGnss.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        try {
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.write("Header Description:");
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.write(VERSION_TAG);
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            String fileVersion = "v2.0.0.1"
                    + " Platform: "
                    + Build.VERSION.RELEASE
                    + " "
                    + "Manufacturer: "
                    + manufacturer
                    + " "
                    + "Model: "
                    + model;
            mFileWriterGnss.write(fileVersion);
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.write(
                    "Raw,CurrentTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,"
                            + "BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,"
                            + "HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,"
                            + "ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,"
                            + "PseudorangeRateUncertaintyMetersPerSecond,"
                            + "AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,"
                            + "AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,"
                            + "CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,"
                            + "ConstellationType,AgcDb");
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.write("Fix,Provider,Latitude,Longitude,Altitude,Speed,Accuracy,(UTC)TimeInMs");
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.write("Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)");
            mFileWriterGnss.newLine();
            mFileWriterGnss.write(COMMENT_START);
            mFileWriterGnss.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void stopRecordGnss() throws IOException {
        mFileWriterGnss.close();
    }

    private void stopRecordImu() throws IOException {
        mFileWriterSensors.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void registerLocation() {
        //Check Permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    OPEN_SET_REQUEST_CODE);
        }
        // 为获取地理位置信息时设置查询条件
        String bestProvider = mLocationManager.getBestProvider(getCriteria(), true);
        // 如果不设置查询要求，getLastKnownLocation方法传人的参数为LocationManager.GPS_PROVIDER
        assert bestProvider != null;
        mLocation = mLocationManager.getLastKnownLocation(bestProvider);
        mLocationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 100, 0, mLocationListener);
    }
    private void registerGnssMeasurements() {
        //Check Permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    OPEN_SET_REQUEST_CODE);
        }
        mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEvent);
        Log.i(TAG, "Register callback -> measurementsEvent");
    }

    private static Criteria getCriteria() {
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 设置是否要求速度
        criteria.setSpeedRequired(true);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(false);
        // 设置是否需要方位信息
        criteria.setBearingRequired(true);
        // 设置是否需要海拔信息
        criteria.setAltitudeRequired(true);
        // 设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        return criteria;
    }

    private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurement measurement)
            throws IOException {
        String clockStream =
                String.format(
                        "Raw,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        System.currentTimeMillis(),
                        //SystemClock.elapsedRealtime(),
                        clock.getTimeNanos(),
                        clock.hasLeapSecond() ? clock.getLeapSecond() : "",
                        clock.hasTimeUncertaintyNanos() ? clock.getTimeUncertaintyNanos() : "",
                        clock.getFullBiasNanos(),
                        clock.hasBiasNanos() ? clock.getBiasNanos() : "",
                        clock.hasBiasUncertaintyNanos() ? clock.getBiasUncertaintyNanos() : "",
                        clock.hasDriftNanosPerSecond() ? clock.getDriftNanosPerSecond() : "",
                        clock.hasDriftUncertaintyNanosPerSecond()
                                ? clock.getDriftUncertaintyNanosPerSecond()
                                : "",
                        clock.getHardwareClockDiscontinuityCount() + ",");
        mFileWriterGnss.write(clockStream);

        String measurementStream =
                String.format(
                        "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        measurement.getSvid(),
                        measurement.getTimeOffsetNanos(),
                        measurement.getState(),
                        measurement.getReceivedSvTimeNanos(),
                        measurement.getReceivedSvTimeUncertaintyNanos(),
                        measurement.getCn0DbHz(),
                        measurement.getPseudorangeRateMetersPerSecond(),
                        measurement.getPseudorangeRateUncertaintyMetersPerSecond(),
                        measurement.getAccumulatedDeltaRangeState(),
                        measurement.getAccumulatedDeltaRangeMeters(),
                        measurement.getAccumulatedDeltaRangeUncertaintyMeters(),
                        measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "",
                        measurement.hasCarrierCycles() ? measurement.getCarrierCycles() : "",
                        measurement.hasCarrierPhase() ? measurement.getCarrierPhase() : "",
                        measurement.hasCarrierPhaseUncertainty()
                                ? measurement.getCarrierPhaseUncertainty()
                                : "",
                        measurement.getMultipathIndicator(),
                        measurement.hasSnrInDb() ? measurement.getSnrInDb() : "",
                        measurement.getConstellationType(),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                && measurement.hasAutomaticGainControlLevelDb()
                                ? measurement.getAutomaticGainControlLevelDb()
                                : "");
        mFileWriterGnss.write(measurementStream);
        mFileWriterGnss.newLine();
    }

}