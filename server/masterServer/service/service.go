package service

import (
	"fmt"
	"io/ioutil"
	"masterServer/config"
	"net/http"
	"sort"
	"strconv"
	"sync"
)

func getBandwidthUsed(ip string) float64 {
	resp, err := http.Get("http://" + ip + ":8000/bandwidth")
	if err != nil {
		fmt.Println(err)
		return 10000
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	bandwidthUsed, _ := strconv.ParseFloat(string(body), 64)
	return bandwidthUsed * 8 / 1024 / 1024
}

type bdu struct {
	ip string
	bd float64 // bandwidthUsed
}

func SS(eBandwidth float64) (int, []string) {
	var bandwidthUsed []bdu
	wg := sync.WaitGroup{}
	wg.Add(len(config.GlobalConfig.Servers))
	buCh := make(chan bdu, len(config.GlobalConfig.Servers))
	for _, ip := range config.GlobalConfig.Servers {
		ip := ip
		go func() {
			bandwidthUsed := bdu{ip: ip, bd: getBandwidthUsed(ip)}
			buCh <- bandwidthUsed
			wg.Done()
		}()
	}
	wg.Wait()
	close(buCh)
	for bu := range buCh {
		bandwidthUsed = append(bandwidthUsed, bu)
	}
	sort.Slice(bandwidthUsed, func(i, j int) bool {
		return bandwidthUsed[i].bd < bandwidthUsed[j].bd
	})
	fmt.Println(bandwidthUsed)
	num := 0
	var ipList []string
	for _, bu := range bandwidthUsed {
		rest := config.GlobalConfig.MaxBandwidth - bu.bd
		if rest <= 0 {
			continue
		} else {
			num++
			ipList = append(ipList, bu.ip)
			eBandwidth -= rest
			if eBandwidth <= 0 {
				break
			}
		}
	}
	if eBandwidth > 0 {
		return -1, nil
	} else {
		return num, ipList
	}
}
