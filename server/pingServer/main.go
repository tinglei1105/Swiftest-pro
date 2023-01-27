package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"net/http"
)

func main() {
	r := gin.Default()
	r.GET("/ping", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"message": "ok",
		})
	})
	r.Static("/static", "static")
	if err := r.Run("0.0.0.0:8080"); err != nil {
		fmt.Println(err)
	}
}
