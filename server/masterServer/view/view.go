package view

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"masterServer/config"
	"masterServer/model"
	"masterServer/service"
	"net/http"
)

func GetIPList(c *gin.Context) {
	type Res struct {
		ServerNum int      `json:"server_num"`
		IpList    []string `json:"ip_list"`
		ClientIP  string   `json:"client_ip"`
	}
	var res Res
	res.ServerNum = len(config.GlobalConfig.Servers)
	res.IpList = config.GlobalConfig.Servers
	res.ClientIP = c.ClientIP()
	c.JSON(http.StatusOK, res)
}

func GetInfo(c *gin.Context) {
	type Req struct {
		NetworkType        string   `json:"network_type"`
		ServersSortedByRTT []string `json:"servers_sorted_by_rtt"`
	}
	var req Req
	err := c.BindJSON(&req)
	if err != nil {
		fmt.Println("err")
		return
	}
	type Res struct {
		ServerNum         int      `json:"server_num"`
		IpList            []string `json:"ip_list"`
		TestTimeout       int      `json:"test_timeout"`
		DownloadSizeSleep int      `json:"download_size_sleep"`
		BPSleep           int      `json:"bp_sleep"`
		TimeWindow        int      `json:"time_window"`
		KSimilar          int      `json:"k_similar"`
		MaxTrafficUse     int      `json:"max_traffic_use"`
		Threshold         float64  `json:"threshold"`
		GetInfoInterval   int      `json:"get_info_interval"`
	}
	var res Res
	res.BPSleep = config.GlobalConfig.BPSleep
	res.DownloadSizeSleep = config.GlobalConfig.DownloadSizeSleep
	res.TimeWindow = config.GlobalConfig.TimeWindow
	res.TestTimeout = config.GlobalConfig.TestTimeout
	res.MaxTrafficUse = config.GlobalConfig.MaxTrafficUseOthers
	res.KSimilar = config.GlobalConfig.KSimilar
	res.Threshold = config.GlobalConfig.Threshold
	res.GetInfoInterval = config.GlobalConfig.GetInfoInterval
	var eBandwidth float64
	if req.NetworkType == "LTE" || req.NetworkType == "3G" || req.NetworkType == "2G" {
		eBandwidth = 400
		res.MaxTrafficUse = config.GlobalConfig.MaxTrafficUse4g
	} else if req.NetworkType == "WIFI" {
		eBandwidth = 1500
		res.MaxTrafficUse = config.GlobalConfig.MaxTrafficUseWifi
	} else if req.NetworkType == "5G" {
		eBandwidth = 1000
		res.MaxTrafficUse = config.GlobalConfig.MaxTrafficUse5g
	} else {
		eBandwidth = 500
		res.MaxTrafficUse = config.GlobalConfig.MaxTrafficUseOthers
	}
	res.ServerNum, res.IpList = service.SS(eBandwidth)
	c.JSON(http.StatusOK, res)
}

func UploadData(c *gin.Context) {
	var data model.TestData
	//d, _ := io.ReadAll(c.Request.Body)
	//fmt.Printf("%s\n", d)
	if err := c.BindJSON(&data); err != nil {
		fmt.Printf("%#v\n", err)
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "wrong data format",
		})
		return
	}
	data.IP = c.ClientIP()
	fmt.Printf("receive: %#v\n", data)
	if err := model.AddData(data); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"message": "cannot write data",
		})
		return
	}
	c.JSON(http.StatusOK, gin.H{
		"message": "ok",
	})
}

type serverReport struct {
	Key   string
	Count int
}

func ReportServerUsage(c *gin.Context) {
	var report serverReport
	if err := c.BindJSON(&report); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": err.Error(),
		})
		return
	}
	if err := model.AddDataUsage(report.Key, 0, report.Count); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(http.StatusOK, gin.H{
		"message": "ok",
	})
}

func ReportClientUsage(c *gin.Context) {
	var report serverReport
	if err := c.BindJSON(&report); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": err.Error(),
		})
		return
	}
	if err := model.AddDataUsage(report.Key, report.Count, 0); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(http.StatusOK, gin.H{
		"message": "ok",
	})
}
