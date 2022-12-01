package com.example.swiftest.speedtest;

/**
 * result of test should be contained in this class
 */
public class TestResult {
    // bandwidth Mbps
    public double bandwidth;
    // duration seconds
    public double duration;
    // traffic MB
    public double traffic;

    public TestResult(double bandwidth, double duration, double traffic) {
        this.bandwidth = bandwidth;
        this.duration = duration;
        this.traffic = traffic;
    }

    public TestResult() {
    }
}
