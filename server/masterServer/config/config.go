package config

import (
	"fmt"
	"gopkg.in/yaml.v3"
	"os"
)

type Config struct {
	BPSleep             int      `yaml:"bp_sleep"`
	DownloadSizeSleep   int      `yaml:"download_size_sleep"`
	TimeWindow          int      `yaml:"time_window"`
	TestTimeout         int      `yaml:"test_timeout"`
	GetInfoInterval     int      `yaml:"get_info_interval"`
	MaxTrafficUse4g     int      `yaml:"max_traffic_use_4_g"`
	MaxTrafficUse5g     int      `yaml:"max_traffic_use_5_g"`
	MaxTrafficUseWifi   int      `yaml:"max_traffic_use_wifi"`
	MaxTrafficUseOthers int      `yaml:"max_traffic_use_others"`
	KSimilar            int      `yaml:"k_similar"`
	Threshold           float64  `yaml:"threshold"`
	MaxBandwidth        float64  `yaml:"max_bandwidth"` // bandwidth limit for each server
	Servers             []string `yaml:"servers"`
}

var GlobalConfig Config

func InitConfig() {
	GlobalConfig = Config{}

	config, err := os.ReadFile("./config/config.yaml")
	if err != nil {
		fmt.Print(err)
	}
	err = yaml.Unmarshal(config, &GlobalConfig)
	if err != nil {
		fmt.Print(err)
	} else {
		fmt.Printf("config: %+v\n", GlobalConfig)
	}
}
