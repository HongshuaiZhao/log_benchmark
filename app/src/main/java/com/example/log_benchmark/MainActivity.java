package com.example.log_benchmark;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bytedance.android.alog.Alog;
import com.dianping.logan.Logan;
import com.dianping.logan.LoganConfig;
import com.ss.android.agilelogger.ALog;
import com.ss.android.agilelogger.ALogConfig;
import com.tencent.mars.xlog.Log;
import com.tencent.mars.xlog.Xlog;


import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;


/**
 * @author bytedance
 * @target 寻找打印log的方法，并在不同多线程环境下进行测试
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int calculationTimes;
    protected static final String TAG = "MainActivity";
    protected static final int XLOG = 1;
    protected static final int LOGAN = 2;
    protected static final int ALOG = 3;
    protected static final int ALOGV2 = 4;
    public int logType;
    protected int threadCount;
    public int test_Count;
    List<testData> mTestDataList = new ArrayList<>();
    List<String> mTestData = new ArrayList<>();
    List<myThread> mList = new ArrayList<>();
    List<Long> mCPU_time = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final EditText testCount = findViewById(R.id.testcount);
        final EditText editText = findViewById(R.id.ThreadNumber);
        TextView timeView = findViewById(R.id.time);
        TextView cpuView = findViewById(R.id.CPUtime);
        TextView meminfoView = findViewById(R.id.Meminfo);
        Button commit_button = findViewById(R.id.commit);
        Button Log_start = findViewById(R.id.Log_start);
        Button xlog = findViewById(R.id.xlog);
        Button logan = findViewById(R.id.logan);
        Button alog = findViewById(R.id.alog);
        Button alogV2 = findViewById(R.id.alogV2);
        xlog.setOnClickListener(this);
        logan.setOnClickListener(this);
        alog.setOnClickListener(this);
        alogV2.setOnClickListener(this);


        commit_button.setOnClickListener(v -> {
            test_Count = Integer.parseInt(testCount.getText().toString());
            threadCount = Integer.parseInt(editText.getText().toString());
            switch (logType) {
                case XLOG:
                    xlogInit();
                    break;
                case LOGAN:
                    loganInit();
                    break;
                case ALOG:
                    alogInit();
                    break;
                case ALOGV2:
                    alogV2Init();
                    break;
                default:
                    android.util.Log.d("MainActivity","commit_button Init failed!");
            }
            calculationTimes = 10000 / threadCount;
            ALog.d("MainActivity", "Init_Success: " + threadCount);

        });

        Log_start.setOnClickListener(v -> {
            for (int times = 0; times < test_Count; times++) {
                for (int i = 0; i < threadCount; i++) {
                    myThread myThread = new myThread(("myThread" + i), calculationTimes,logType);
                    mList.add(myThread);
                }
                testData mytestData = new testData();
//                long meminfoBegin = getAvailableMemory();
                long meminfoBegin = getMemory();
                ALog.d("test", "begin : " + meminfoBegin);
                for (int i = 0; i < 100000; i++) {
                    mTestData.add("asdasfgad" + i);
                }
                long meminfoTest = getMemory();
                ALog.d("test", "test : " + meminfoTest);

                Instant beginTime = Instant.now();
                long CPU_begin = 0, CPU_end = 0, CPU_sum = 0;


                for (int i = 0; i < threadCount; i++) {
                    CPU_begin = Debug.threadCpuTimeNanos();
                    mList.get(i).start();
                }
                for (int i = 0; i < threadCount; i++) {
                    try {
                        mList.get(i).join();
                        CPU_end = Debug.threadCpuTimeNanos();
                        mCPU_time.add(CPU_end - CPU_begin);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.gc();

                Instant endTime = Instant.now();
//                long meminfoEnd = getAvailableMemory();
                long meminfoEnd = getMemory();
                ALog.d("test", "end : " + meminfoEnd);
                for (int i = 0; i < threadCount; i++) {
                    CPU_sum += mCPU_time.get(i);
                }

                mytestData.setInstant(Duration.between(beginTime, endTime).toMillis());
                mytestData.setCPU_Time(CPU_sum / 1000000);
                mytestData.setMem_usage(meminfoBegin - meminfoEnd);
                mTestDataList.add(mytestData);
                mList.clear();
            }
            calcution(timeView, cpuView, meminfoView);
            mTestDataList.clear();
        });

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void calcution(TextView time, TextView cpuTime, TextView meminfoView) {
        long mtime = mTestDataList.stream().mapToLong(testData::getInstant).max().getAsLong();
        double atime = mTestDataList.stream().mapToLong(testData::getInstant).average().getAsDouble();
        long mCPU_time = mTestDataList.stream().mapToLong(testData::getCPU_Time).max().getAsLong();
        double aCPU_time = mTestDataList.stream().mapToLong(testData::getCPU_Time).average().getAsDouble();
        long mMeminfo = mTestDataList.stream().mapToLong(testData::getMem_usage).max().getAsLong();
        double aMeminfo = mTestDataList.stream().mapToLong(testData::getMem_usage).average().getAsDouble();
        ALog.d(TAG, "time: " + mtime + " : " + atime);
        ALog.d(TAG, "CPU_time: " + mCPU_time + " : " + aCPU_time);
        ALog.d(TAG, "Meminfo: " + mMeminfo + " : " + aMeminfo);
        time.setText("time: " + mtime + " : " + atime);
        cpuTime.setText("CPU_time: " + mCPU_time + " : " + aCPU_time);
        meminfoView.setText("Meminfo: " + mMeminfo + " : " + aMeminfo);


    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public final long getMemory() {
        ActivityManager mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int pid = Process.myPid();
        ALog.d("pid", "mypid : " + pid);
        int[] pids = {pid};

        Class clazz = new Debug.MemoryInfo().getClass();
        Field[] fields = clazz.getDeclaredFields();

        Debug.MemoryInfo[] memoryInfos = mActivityManager.getProcessMemoryInfo(pids);

        Arrays.stream(memoryInfos).forEach(v -> {
            for (int i = 0; i < fields.length; i++) {
                try {
                    Field field = fields[i];
                    field.setAccessible(true);
                    String name = field.getName();
                    Object val = field.get(v);
                    ALog.d("1111", "getMemory: " + name + " " + val.toString());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        long sum = (memoryInfos[0].getTotalPrivateDirty());
        return sum;
    }


    public final long getAvailableMemory() {

        ActivityManager.MemoryInfo localMemoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager localActivityManager = (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        if (localActivityManager != null) {
            localActivityManager.getMemoryInfo(localMemoryInfo);
        }

        //可用内存
        long l = localMemoryInfo.availMem / 1000000;
        //是否达到最低内存
        boolean isLowMem = localMemoryInfo.lowMemory;
        //临界值，达到这个值，进程就要被杀死
        long threshold = localMemoryInfo.threshold / 1000000;
        //总内存
        long totalMem = localMemoryInfo.totalMem / 1000000;

        ALog.d(TAG, "avail:" + l + ",isLowMem:" + isLowMem + ",threshold:" + threshold + ",totalMem:" + totalMem);
        return l;

    }

    public void xlogInit() {
        System.loadLibrary("c++_shared");
        System.loadLibrary("marsxlog");

        final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String logPath = SDCARD + "/marssample/log";

        // this is necessary, or may crash for SIGBUS
        final String cachePath = this.getFilesDir() + "/xlog";

        //init xlog
        if (BuildConfig.DEBUG) {
            Xlog.appenderOpen(Xlog.LEVEL_DEBUG, Xlog.AppednerModeAsync, cachePath, logPath, "MarsSample", 0, "");
            Xlog.setConsoleLogOpen(true);

        } else {
            Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath, "MarsSample", 0, "");
            Xlog.setConsoleLogOpen(false);
        }
        Log.setLogImp(new Xlog());
    }

    public void loganInit() {
        LoganConfig config = new LoganConfig.Builder()
                .setCachePath(getApplicationContext().getFilesDir().getAbsolutePath())
                .setPath(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()
                        + File.separator + "logan_v1")
                .setEncryptKey16("0123456789012345".getBytes())
                .setEncryptIV16("0123456789012345".getBytes())
                .build();
        Logan.init(config);
    }

    public void alogV2Init() {
        Alog.init();
        com.bytedance.android.alog.Log.setInstance(new Alog.Builder(this).build());

    }

    public void alogInit() {
        // 先配置 ALog 参数
        ALogConfig config = new ALogConfig.Builder(getApplicationContext())
                // 日志目录中最大缓存大小, 默认20M, 超过后, 会删除最旧的日志
                .setMaxDirSize(20 * 1024 * 1024)
                // 每个日志分片大小, 默认2M, 考虑上行带宽的限制, 不宜超过5M
                .setPerSize(2 * 1024 * 1024)
                .build();

        ALog.setsPackageClassName(Logger.class.getCanonicalName());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.xlog:
                logType = XLOG;
                break;
            case R.id.logan:
                logType = LOGAN;
                break;
            case R.id.alog:
                logType = ALOG;
                break;
            case R.id.alogV2:
                logType = ALOGV2;
                break;
            default:
                android.util.Log.d("MainActivity","set logType failed");
        }
    }
}
