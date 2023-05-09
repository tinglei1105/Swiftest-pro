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
	r.POST("/report/server", view.ReportServerUsage)
	r.POST("/report/client", view.ReportClientUsage)
	r.POST("/report/swiftest", view.UploadSwiftestData)
	r.POST("/report/packet-train", view.ReportPacketTrain)
	r.POST("/report/packet-train-baseline", view.ReportPacketTrainBaseline)
	r.StaticFS("/static", http.Dir("./static"))
	//r.StaticFile("/static")
}
