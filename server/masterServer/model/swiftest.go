package model

import (
	"gorm.io/datatypes"
	"gorm.io/gorm"
)

type SwiftestData struct {
	gorm.Model
	// bandwidth Mbps
	Bandwidth float64
	// duration seconds
	Duration float64
	// traffic MB
	Traffic float64
	// long tail MB
	LongTail float64 `gorm:"column:long_tail" json:"long_tail"`
	// server usage MB
	ServerUsage float64 `gorm:"column:server_usage" json:"server_usage"`
	//Speed sample
	SpeedSample datatypes.JSON `json:"speed_sample"`
	//Network Type
	NetworkType string `json:"network_type"`
	//IP
	IP string
}

func AddSwiftestData(data SwiftestData) error {
	return MySQL().Create(&data).Error
}
