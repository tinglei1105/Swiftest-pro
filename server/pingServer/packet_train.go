package main

import (
	"github.com/Q-Wednesday/advanced-sender/sender"
	"net"
)

var packetTrainServer sender.PacketTrainServer

var packetTrainTCP = net.TCPAddr{
	IP:   net.IPv4(0, 0, 0, 0),
	Port: 9878,
}

var packetTrainUDP = net.UDPAddr{
	IP:   net.IPv4(0, 0, 0, 0),
	Port: 9877,
}
