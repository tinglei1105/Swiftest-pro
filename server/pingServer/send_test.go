package main

import (
	"bytes"
	"fmt"
	"math/rand"
	"net"
	"net/http"
	"strings"
	"testing"
	"time"
)

const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

func randomString(n int) string {
	sb := strings.Builder{}
	sb.Grow(n)
	for i := 0; i < n; i++ {
		sb.WriteByte(charset[rand.Intn(len(charset))])
	}
	return sb.String()
}

func TestSend(t *testing.T) {
	socket, err := net.DialUDP("udp", nil, &net.UDPAddr{
		IP:   net.IPv4(127, 0, 0, 1),
		Port: 9876,
	})
	if err != nil {
		t.Fatal(err)
	}
	defer socket.Close()
	key := fmt.Sprintf("%v-%s", time.Now().Unix(), randomString(3))
	//key := "1675091237-XVl"
	sendData := []byte(key)
	_, err = socket.Write(sendData) // 发送数据
	if err != nil {
		t.Fatal(err)
	}
	data := make([]byte, BUFFER_SIZE)
	stopped := false
	total := 0
	go func() {
		time.Sleep(1 * time.Second)
		stopData := []byte("stop")
		_, err = socket.Write(stopData) // 发送数据
		if err != nil {
			fmt.Println("发送数据失败，err:", err)
			return
		}
		stopped = true
	}()
	for !stopped {
		n, _, err := socket.ReadFromUDP(data) // 接收数据
		if err != nil {
			fmt.Println("接收数据失败，err:", err)
			return
		}
		total += n
	}
	fmt.Println(total)
	jsonStr := []byte(fmt.Sprintf("{\"key\":\"%s\",\"count\":%v}", key, total))
	req, _ := http.NewRequest(http.MethodPost, reportUrlClient, bytes.NewBuffer(jsonStr))
	req.Header.Set("Content-Type", "application/json")
	resp, err := httpClient.Do(req)
	if err != nil || resp.StatusCode != http.StatusOK {
		fmt.Println(err)
	}
}
