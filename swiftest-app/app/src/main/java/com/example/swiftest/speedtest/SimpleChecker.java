package com.example.swiftest.speedtest;

import java.util.ArrayList;
import java.util.Collections;

// 检查采样的样本，在稳定是停止测速
class SimpleChecker extends Thread {
    ArrayList<Double> speedSample;
    boolean finish;
    Double simpleSpeed;
    final static private int CheckerSleep = 50;                 // Time interval between checks
    final static private int CheckerWindowSize = 10;            // SimpleChecker window size
    final static private int CheckerSelectedSize = 8;           // SimplerChecker selected size
    final static private double CheckerThreshold = 0.08;        // threshold
    final static private int CheckerTimeoutWindow = 50;         // Window size when overtime
    SimpleChecker(ArrayList<Double> speedSample) {
        this.speedSample = speedSample;
        this.finish = false;
        this.simpleSpeed = 0.0;
    }

    public void run() {
        while (!finish) {
            try {
                sleep(CheckerSleep);

                int n = speedSample.size();
                if (n < CheckerWindowSize) continue;

                ArrayList<Double> recentSamples = new ArrayList<>();
                for (int i = n - CheckerWindowSize; i < n; ++i)
                    recentSamples.add(speedSample.get(i));
                Collections.sort(recentSamples);
                int windowNum = CheckerWindowSize - CheckerSelectedSize + 1;
                for (int i = 0; i < windowNum; ++i) {
                    int j = i + CheckerSelectedSize - 1;
                    double lower = recentSamples.get(i), upper = recentSamples.get(j);
                    // Here no division by 0 is considered,
                    // but (NaN < CheckerThreshold) so it's work!
                    // All 0 should not go through this condition.
                    if ((upper - lower) / upper < CheckerThreshold) {
                        double res = 0;
                        for (int k = i; k <= j; ++k)
                            res += recentSamples.get(k);
                        simpleSpeed = res / CheckerSelectedSize;
                        finish = true;
                        break;
                    }
                }
            } catch (InterruptedException e) {
                double res = 0.0;
                int n = speedSample.size();
                if (n < CheckerTimeoutWindow) return;
                for (int k = n - CheckerTimeoutWindow; k < n; ++k)
                    res += speedSample.get(k);
                simpleSpeed = res / CheckerTimeoutWindow;
                break;
            }
        }
    }

    public double getSpeed() {
        return simpleSpeed;
    }
}
