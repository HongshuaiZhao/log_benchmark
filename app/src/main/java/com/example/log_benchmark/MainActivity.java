package com.example.log_benchmark;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ss.android.agilelogger.ALog;
import com.ss.android.agilelogger.ALogConfig;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * @author bytedance
 * @target 寻找打印log的方法，并在不同多线程环境下进行测试
 */
public class MainActivity extends AppCompatActivity {
    private int calculationTimes;
    public static final String TAG = "MainActivity";
    protected int threadCount;
    public int test_Count;
    List<testData> mTestDataList = new ArrayList<>();
    List<myThread> mList = new ArrayList<>();
    List<Long> mCPU_time = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alogInit();


        final EditText testCount = findViewById(R.id.testcount);
        final EditText editText = findViewById(R.id.ThreadNumber);
        TextView timeView = findViewById(R.id.time);
        TextView cpuView = findViewById(R.id.CPUtime);
        TextView meminfoView = findViewById(R.id.Meminfo);
        Button commit_button = findViewById(R.id.commit);
        Button Log_start = findViewById(R.id.Log_start);


        commit_button.setOnClickListener(v -> {
            test_Count = Integer.parseInt(testCount.getText().toString());
            threadCount = Integer.parseInt(editText.getText().toString());
            calculationTimes = 10000 / threadCount;
            ALog.d("MainActivity", "onClick: " + threadCount);

        });

        Log_start.setOnClickListener(v -> {
            for (int times = 0; times < test_Count; times++) {
                for (int i = 0; i < threadCount; i++) {
                    myThread myThread = new myThread(("myThread" + i), calculationTimes);
                    mList.add(myThread);
                }
                testData mytestData = new testData();
                long meminfoBegin = getAvailableMemory();
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
                long meminfoEnd = getAvailableMemory();
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void calcution(TextView time, TextView cpuTime, TextView meminfoView) {
        long mtime = mTestDataList.stream().mapToLong(testData::getInstant).max().getAsLong();
        double atime = mTestDataList.stream().mapToLong(testData::getInstant).average().getAsDouble();
        long mCPU_time = mTestDataList.stream().mapToLong(testData::getCPU_Time).max().getAsLong();
        double aCPU_time = mTestDataList.stream().mapToLong(testData::getCPU_Time).average().getAsDouble();
        long mMeminfo = mTestDataList.stream().mapToLong(testData::getMem_usage).max().getAsLong();
        double aMeminfo = mTestDataList.stream().mapToLong(testData::getMem_usage).average().getAsDouble();
        Log.d(TAG, "time: " + mtime + " : " + atime);
        Log.d(TAG, "CPU_time: " + mCPU_time + " : " + aCPU_time);
        Log.d(TAG, "Meminfo: " + mMeminfo + " : " + aMeminfo);
        time.setText("time: " + mtime + " : " + atime);
        cpuTime.setText("CPU_time: " + mCPU_time + " : " + aCPU_time);
        meminfoView.setText("Meminfo: " + mMeminfo + " : " + aMeminfo);


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

        Log.d(TAG, "avail:" + l + ",isLowMem:" + isLowMem + ",threshold:" + threshold + ",totalMem:" + totalMem);
        return l;

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

}
