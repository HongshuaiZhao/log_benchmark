package com.example.log_benchmark;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ss.android.agilelogger.ALog;
import com.ss.android.agilelogger.ALogConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.OptionalLong;
import java.util.logging.Logger;

import static com.ss.android.agilelogger.ALog.getContext;

/**
 * @author bytedance
 * @target 寻找打印log的方法，并在不同多线程环境下进行测试
 */
public class MainActivity extends AppCompatActivity {
    public Context mContext;
    private int calculationTimes;
    public static final String TAG = "MainActivity";
    protected int thread_count;
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
        Button commit_button = findViewById(R.id.commit);
        Button Log_start = findViewById(R.id.Log_start);

        commit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test_Count = Integer.parseInt(testCount.getText().toString());
                thread_count = Integer.parseInt(editText.getText().toString());
                calculationTimes = 10000 / thread_count;
                ALog.d("MainActivity", "onClick: " + thread_count);
                for (int i = 0; i < thread_count; i++) {
                    myThread myThread = new myThread(("myThread" + i), calculationTimes);
                    mList.add(myThread);
                }
            }
        });

        Log_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int times = 0; times < test_Count; times++) {
                    testData mytestData = new testData();
                    long meminfoBegin = getAvailableMemory();
                    Instant beginTime = Instant.now();
                    long CPU_begin = 0, CPU_end = 0, CPU_sum = 0;

                    for (int i = 0; i < thread_count; i++) {
                        CPU_begin = Debug.threadCpuTimeNanos();
                        mList.get(i).start();
                    }
                    for (int i = 0; i < thread_count; i++) {
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
                    for (int i = 0; i < thread_count; i++) {
                        CPU_sum += mCPU_time.get(i);
                    }
                    mytestData.setInstant(Duration.between(beginTime,endTime).toMillis());
                    mytestData.setCPU_Time(CPU_sum/1000000);
                    mytestData.setMem_usage(meminfoBegin-meminfoEnd);
                    mTestDataList.add(mytestData);
                    ALog.d(TAG, "ToMills: " + Duration.between(beginTime, endTime).toMillis());
                    ALog.d(TAG, "CPU_SUM_TIME : " + CPU_sum / 1000000);
                    ALog.d(TAG, "MemoryUsage : " + (meminfoBegin - meminfoEnd));
                    mList.clear();
                }
                calcution();

            }
        });

    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void calcution(){
        for(int i=0;i<test_Count;i++){
            long mtime = mTestDataList.stream().mapToLong(testData::getInstant).max().getAsLong();
            Log.d(TAG, "calcution: "+mtime);
        }
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

        Log.i(TAG, "avail:" + l + ",isLowMem:" + isLowMem + ",threshold:" + threshold + ",totalMem:" + totalMem);
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
