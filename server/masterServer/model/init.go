package model

import (
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

var db *gorm.DB

func InitMysql() {
	dsn := "root:yh-qiu18@tcp(127.0.0.1:3306)/swiftest?charset=utf8mb4&parseTime=True&loc=Local"
	var err error
	db, err = gorm.Open(mysql.Open(dsn), &gorm.Config{})
	if err != nil {
		panic(err)
	}
	err = db.AutoMigrate(&TestData{})
	if err != nil {
		panic(err)
	}
	err = db.AutoMigrate(&DataUsage{})
	if err != nil {
		panic(err)
	}
}

func MySQL() *gorm.DB {
	return db
}
