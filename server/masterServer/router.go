package main

import (
	"github.com/gin-gonic/gin"
	"masterServer/view"
	"net/http"
)

func register(r *gin.Engine) {
	r.GET("/hello", func(c *gin.Context) {
		c.JSON(http.StatusOK, `hello, Swiftest!`)
	})
	r.GET("/speedtest/iplist/available", view.GetIPList)
	r.POST("/speedtest/info", view.GetInfo)
	r.POST("/speedtest/data/upload", view.UploadData)
	r.Static("/static", "static")
	//r.StaticFile("/static")
}
