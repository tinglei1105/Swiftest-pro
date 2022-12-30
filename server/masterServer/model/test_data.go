package model

import "gorm.io/gorm"

type TestData struct {
	gorm.Model
	// bandwidth Mbps
	Bandwidth float64
	// duration seconds
	Duration float64
	// traffic MB
	Traffic float64
}

func AddData(data TestData) error {
	return MySQL().Create(&data).Error
}
