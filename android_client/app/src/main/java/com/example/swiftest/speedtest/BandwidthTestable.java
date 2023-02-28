package com.example.swiftest.speedtest;

import java.io.IOException;

public interface BandwidthTestable {
    TestResult test() throws IOException, InterruptedException;
    void stop();
}

