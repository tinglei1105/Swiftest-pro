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
    // long tail MB
    public double longTail;
    public double serverUsage;
    // client_ip
    public String publicIP;
    public String privateIP;
    public String networkType;
    public String networkOperator;

    public TestResult(double bandwidth, double duration, double traffic, double longTail) {
        this.bandwidth = bandwidth;
        this.duration = duration;
        this.traffic = traffic;
        this.longTail = longTail;
    }

    public TestResult() {
    }
    public  static TestResultBuilder builder() {
        return new TestResultBuilder();
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "bandwidth=" + bandwidth +
                ", duration=" + duration +
                ", traffic=" + traffic +
                ", longTail=" + longTail +
                ", publicIP='" + publicIP + '\'' +
                ", privateIP='" + privateIP + '\'' +
                ", networkType='" + networkType + '\'' +
                ", networkOperator='" + networkOperator + '\'' +
                '}';
    }

    public static final class TestResultBuilder {
        private double bandwidth;
        private double duration;
        private double traffic;
        private double longTail;
        private double serverUsage;
        private String publicIP;
        private String privateIP;
        private String networkType;
        private String networkOperator;

        private TestResultBuilder() {
        }

        public static TestResultBuilder aTestResult() {
            return new TestResultBuilder();
        }

        public TestResultBuilder withBandwidth(double bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        public TestResultBuilder withDuration(double duration) {
            this.duration = duration;
            return this;
        }

        public TestResultBuilder withTraffic(double traffic) {
            this.traffic = traffic;
            return this;
        }

        public TestResultBuilder withLongTail(double longTail) {
            this.longTail = longTail;
            return this;
        }

        public TestResultBuilder withServerUsage(double serverUsage) {
            this.serverUsage = serverUsage;
            return this;
        }

        public TestResultBuilder withPublicIP(String publicIP) {
            this.publicIP = publicIP;
            return this;
        }

        public TestResultBuilder withPrivateIP(String privateIP) {
            this.privateIP = privateIP;
            return this;
        }

        public TestResultBuilder withNetworkType(String networkType) {
            this.networkType = networkType;
            return this;
        }

        public TestResultBuilder withNetworkOperator(String networkOperator) {
            this.networkOperator = networkOperator;
            return this;
        }

        public TestResult build() {
            TestResult testResult = new TestResult(bandwidth, duration, traffic, longTail);
            testResult.publicIP = this.publicIP;
            testResult.privateIP = this.privateIP;
            testResult.networkOperator = this.networkOperator;
            testResult.networkType = this.networkType;
            testResult.serverUsage = this.serverUsage;
            return testResult;
        }
    }
}
