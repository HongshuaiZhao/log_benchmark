package com.example.log_benchmark;

import android.util.Log;

import com.ss.android.agilelogger.ALog;

import static com.example.log_benchmark.MainActivity.TAG;

/**
 * @description:
 * @author: bytedance
 * @date: 2020-03-31 11:50
 */
public class myThread extends Thread {
    public String name;
    public int calculationTimes;

    @Override
    public void run() {
        for (int i = calculationTimes; i > 0; i--) {
            ALog.d("MainActivity", name + ": run: " + i);

        }
    }

    public myThread(String name, int calculationTimes) {
        this.name = name;
        this.calculationTimes = calculationTimes;
        ALog.d("MainActivity", name);
    }
}
