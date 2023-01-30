package model

import (
	"fmt"
	"gorm.io/gorm"
	"time"
)

type DataUsage struct {
	ID          string `gorm:"primaryKey"`
	ClientUsage int
	ServerSend  int
}

func GetDataUsage(key string) (*DataUsage, error) {
	var usage DataUsage
	err := MySQL().First(&usage, "id = ?", key).Error

	if err != nil {
		return nil, err
	}
	return &usage, nil
}

func AddDataUsage(id string, clientUsage, serverUsage int) error {
	var err error
	//尝试三次
	for i := 0; i < 3; i++ {
		err = MySQL().Transaction(func(tx *gorm.DB) error {
			var usage DataUsage
			if tx.First(&usage, "id = ?", id).Error == gorm.ErrRecordNotFound {
				return tx.Create(&DataUsage{
					ID:          id,
					ClientUsage: clientUsage,
					ServerSend:  serverUsage,
				}).Error
			}
			usage.ClientUsage += clientUsage
			usage.ServerSend += serverUsage
			return tx.Save(&usage).Error
		})
		if err == nil {
			return nil
		}
		fmt.Println(err)
		time.Sleep(100)
	}
	return err

}
