package main

import (
	"fmt"
	"masterServer/config"
)
import "github.com/gin-gonic/gin"

func main() {
	config.InitConfig()
	r := gin.Default()
	register(r)
	if err := r.Run(); err != nil {
		fmt.Println(err)
	}
}
