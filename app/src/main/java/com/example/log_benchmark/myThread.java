package com.example.log_benchmark;

import android.os.Debug;

import com.bytedance.android.alog.Alog;
import com.dianping.logan.Logan;
import com.ss.android.agilelogger.ALog;
import com.tencent.mars.xlog.Log;

import static com.example.log_benchmark.MainActivity.ALOG;
import static com.example.log_benchmark.MainActivity.ALOGV2;
import static com.example.log_benchmark.MainActivity.LOGAN;
import static com.example.log_benchmark.MainActivity.TAG;
import static com.example.log_benchmark.MainActivity.XLOG;

/**
 * @description:
 * @author: bytedance
 * @date: 2020-03-31 11:50
 */
public class myThread extends Thread {
    public String name;
    public int calculationTimes;
    public int logType;
    private long CPUTime;


    public long getCPUTime() {
        return CPUTime;
    }

    @Override
    public void run() {
        long begin,end;
        begin = Debug.threadCpuTimeNanos();
        for (int i = calculationTimes; i > 0; i--) {
            switch (logType) {
                case XLOG:
                    Log.d("MainActivity", name + ": xlog run: " + i);
                    break;
                case LOGAN:
                    Logan.w(name + ": logan run: " + i, 2);
                    break;
                case ALOG:
                    ALog.d("MainActivity", name + ": alog run: " + i);
                    break;
                case ALOGV2:
                    com.bytedance.android.alog.Log.d("MainActivity", name + ": alogv2 run: " + i);
                    break;
                default:
                    android.util.Log.d("MainActivity", "init failed");
            }
        }
        end = Debug.threadCpuTimeNanos();
        CPUTime = end-begin;
    }

    public myThread(String name, int calculationTimes, int logType) {
        this.name = name;
        this.calculationTimes = calculationTimes;
        this.logType = logType;
        android.util.Log.d("MainActivity", name);
    }
}
