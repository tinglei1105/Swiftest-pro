package main

import (
	"fmt"
	"masterServer/config"
	"masterServer/model"
)
import "github.com/gin-gonic/gin"

func main() {
	config.InitConfig()
	model.InitMysql()
	r := gin.Default()
	register(r)
	if err := r.Run("0.0.0.0:9000"); err != nil {
		fmt.Println(err)
	}
}
