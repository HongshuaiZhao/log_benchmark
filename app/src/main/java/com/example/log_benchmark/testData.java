package com.example.log_benchmark;

/**
 * @description:
 * @author: bytedance
 * @date: 2020-04-07 11:14
 */
public class testData {
    long mInstant;
    long mCPU_Time;
    long mMem_usage;

    public long getInstant() {
        return mInstant;
    }

    public void setInstant(long instant) {
        mInstant = instant;
    }

    public long getCPU_Time() {
        return mCPU_Time;
    }

    public void setCPU_Time(long CPU_Time) {
        mCPU_Time = CPU_Time;
    }

    public long getMem_usage() {
        return mMem_usage;
    }

    public void setMem_usage(long mem_usage) {
        mMem_usage = mem_usage;
    }
}
