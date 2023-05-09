package model

import (
	"gorm.io/gorm"
)

type PacketTrainBaseline struct {
	gorm.Model
	//Network Type
	NetworkType string `json:"network_type" gorm:"network_type"`
	//IP
	IP string
	// bandwidth Mbps
	PacketBandwidth float64 `gorm:"column:packet_bandwidth" json:"packet_bandwidth"`
	// duration seconds
	PacketDuration float64 `json:"packet_duration" gorm:"column:packet_duration"`
	// traffic MB
	PacketTraffic float64 `json:"packet_traffic" gorm:"column:packet_traffic"`
	// server usage MB
	PacketServerUsage float64 `gorm:"column:packet_server_usage" json:"packet_server_usage"`
	// bandwidth Mbps
	SwiftestBandwidth float64 `json:"swiftest_bandwidth" gorm:"column:swiftest_bandwidth"`
	// duration seconds
	SwiftestDuration float64 `json:"swiftest_duration" gorm:"column:swiftest_duration"`
	// traffic MB
	SwiftestTraffic float64 `json:"swiftest_traffic" gorm:"column:swiftest_traffic"`
	// server usage MB
	SwiftestServerUsage float64 `gorm:"column:swiftest_server_usage" json:"swiftest_server_usage"`
}

func AddPacketTrainBaseline(data PacketTrainBaseline) error {
	return MySQL().Create(&data).Error
}
