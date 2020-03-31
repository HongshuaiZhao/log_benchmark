package com.example.log_benchmark;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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
    List<myThread> mList = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        getContext().getApplicationContext()
//        Log.d(TAG, "onCreate: "+getContext());
        // 先配置 ALog 参数
        ALogConfig config = new ALogConfig.Builder(getApplicationContext())
                // 日志目录中最大缓存大小, 默认20M, 超过后, 会删除最旧的日志
                .setMaxDirSize(20 * 1024 * 1024)
                // 每个日志分片大小, 默认2M, 考虑上行带宽的限制, 不宜超过5M
                .setPerSize(2 * 1024 * 1024)
                .build();

        ALog.setsPackageClassName(Logger.class.getCanonicalName());

//// 初始化, 初始化后, 会将日志写到文件中, 不初始化, 则输出在控制台
//        if (!BuildConfig.DEBUG) {
//            ALog.init(config);
//        }

        final EditText editText = findViewById(R.id.ThreadNumber);
        Button commit_button = findViewById(R.id.commit);
        Button Log_start = findViewById(R.id.Log_start);

        commit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                Instant beginTime = Instant.now();
                for (int i = 0; i < thread_count; i++) {
                    mList.get(i).start();
                }
                for (int i = 0; i < thread_count; i++) {
                    try {
                        mList.get(i).join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.gc();
                Instant endTime = Instant.now();
                ALog.d(TAG, "ToMills: " + Duration.between(beginTime, endTime).toMillis());
                mList.clear();


            }
        });

    }
}
