package main

import (
	"bytes"
	"fmt"
	"net"
	"net/http"
	"strings"
	"time"
)

const BUFFER_SIZE = 1024
const reportUrl = "http://124.223.41.138:8080/report/server"
const reportUrlClient = "http://124.223.41.138:8080/report/client"

type Sender struct {
	stopped   bool
	byteCount int
	sock      *net.UDPConn
	addr      *net.UDPAddr
	key       string
}

func (s *Sender) send() {
	startTime := time.Now()
	for time.Now().Before(startTime.Add(3 * time.Second)) {
		if s.stopped {
			break
		}
		sz, _ := s.sock.WriteToUDP(rawData, s.addr)
		s.byteCount += sz
	}
	fmt.Println(s.byteCount)
	jsonStr := []byte(fmt.Sprintf("{\"key\":\"%s\",\"count\":%v}", s.key, s.byteCount))
	req, _ := http.NewRequest(http.MethodPost, reportUrl, bytes.NewBuffer(jsonStr))
	req.Header.Set("Content-Type", "application/json")
	resp, err := httpClient.Do(req)
	if err != nil || resp.StatusCode != http.StatusOK {
		fmt.Println(err)
	}
}
func (s *Sender) stop() {
	s.stopped = true
}

var serverSocket *net.UDPConn
var rawData []byte
var runningSenders map[string]*Sender
var bytesRecord map[string]int
var httpClient http.Client

func initSenders() {
	var err error
	serverSocket, err = net.ListenUDP("udp", &net.UDPAddr{
		IP:   net.IPv4(0, 0, 0, 0),
		Port: 9876,
	})
	if err != nil {
		panic(err)
	}
	rawData = make([]byte, BUFFER_SIZE)
	for i := 0; i < len(rawData); i++ {
		rawData[i] = 'A'
	}
	runningSenders = make(map[string]*Sender)
}

func downloadHandler() {
	for {
		data := make([]byte, BUFFER_SIZE)
		n, clientAddress, err := serverSocket.ReadFromUDP(data)
		key := clientAddress.String()
		if err != nil {
			continue
		}
		fmt.Printf("%#v %s\n", clientAddress, key)
		dataString := strings.TrimSpace(string(data[:n]))
		fmt.Println(len(dataString))
		fmt.Println(dataString)
		if dataString[:4] == "stop" {
			fmt.Println("stop")
			if sender, ok := runningSenders[key]; ok {
				sender.stop()
				delete(runningSenders, key)
			}
		} else {
			runningSenders[key] = &Sender{
				sock: serverSocket,
				addr: clientAddress,
				key:  dataString,
			}
			go runningSenders[key].send()
		}
	}
}
