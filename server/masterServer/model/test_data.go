package model

import (
	"gorm.io/datatypes"
	"gorm.io/gorm"
)

type TestData struct {
	gorm.Model
	// bandwidth Mbps
	Bandwidth float64
	// baseline Mbps
	Baseline float64
	// duration seconds
	Duration float64
	// traffic MB
	Traffic float64
	// long tail MB
	LongTail float64 `gorm:"column:long_tail"`
	//Speed sample
	SpeedSample datatypes.JSON `json:"speed_sample"`
	//Network Type
	NetworkType string `json:"network_type"`
	//Cell info
	CellInfo string `json:"cell_info" gorm:"type:json"`
	//Wi-Fi info
	WifiInfo string `json:"wifi_info" gorm:"type:json"`
	//IP
	IP string
}

func AddData(data TestData) error {
	return MySQL().Create(&data).Error
}
