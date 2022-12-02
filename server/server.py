import socket
import threading
import time

MAX_WAITING_TIME = 3 # second
DATAGRAM_SIZE = 1024
DATAGRAM = ''
for i in range(DATAGRAM_SIZE):
    DATAGRAM = DATAGRAM + '0'

td = []
for i in range(10):
    d = ''
    for j in range(DATAGRAM_SIZE):
        d = d + str(i)
    td.append(d)
# print(td)

x = 0
def getDatagram():
    global x
    x = x + 1
    if x>=len(td):
        x = 0
    return td[x]

class Sender(threading.Thread):
    def __init__(self, addr, speed, sendingTime, ctl):
        threading.Thread.__init__(self)
        self.ip = addr[0]
        self.udpSock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.udpSock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.udpSock.bind(('', 9876))
        self.speed = speed
        self.sendingTime = sendingTime/1000 # s
        self.byte_count = 0
        self.ctl = ctl
    
    def run(self):
        data, client_address = self.udpSock.recvfrom(DATAGRAM_SIZE)
        client_name = str(client_address[0]) + ':' + str(client_address[1])
        print('receive package, data=%s, address=%s' % (data, str(client_address)))
        self.ctl.message_getted()
        port = client_address[1]
        
        # print('start sending')
        self.start_time = time.time()
        while time.time() - self.start_time < self.sendingTime and self.ctl.stop != True:
            t = time.time() - self.start_time
            if self.byte_count <= self.speed/8*1024*1024*t - DATAGRAM_SIZE:
                self.byte_count += self.udpSock.sendto(bytes(getDatagram(), encoding='ascii'), (self.ip, port))
        self.end_time=time.time()

class TcpConn(threading.Thread):
    def __init__(self, conn, addr):
        threading.Thread.__init__(self)
        self.conn = conn
        self.addr = addr
        self.stop = False
        self.status="normal"
    def run(self):
        sender = Sender(self.addr, 0, 0, self)
        while True:
            if self.status=="normal":
                data = conn.recv(1024)
                msg = data.decode('utf-8')
                msg = msg.split('-')
                # print("wait----")
                if msg[0] == 'SET':
                    speed = int(msg[1]) # Mbps
                    sendingTime = int(msg[2]) # ms
                    print("speed",speed)
                    sender.speed=speed
                    sender.sendingTime=sendingTime/1000
                    self.status="wait"
                    sender.start()
                if msg[0] == 'FIN':
                    self.stop = True
                    self.conn.close()
                    print('Connection from %s:%s closed.' % self.addr)
                    print('client: %s:%s, duration: %f s, byte_count: %.2f (%.2f MB)sent %.2f (%.2f MB) received, setting speed: %.2f' % (
                    sender.ip, port, sender.end_time - sender.start_time, sender.byte_count, sender.byte_count / 1024 / 1024,
                    float(msg[1]),float(msg[1])/1024/1024,
                    sender.speed))
                    break
            else:
                # print("sleep---")
                time.sleep(0.1)

    def message_getted(self):
        conn.send(b"get")
        self.status="normal"
def get_host_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()

    return ip

if __name__ == '__main__':
    # host = socket.gethostname()
    # print(host)
    # ip = socket.gethostbyname(host)
    # print(ip)
    print(get_host_ip())
    port = 8080
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(("0.0.0.0", port))
    s.listen(50)
    print('Waiting for connection...')
    while True:
        conn, addr = s.accept()
        print("addr", addr)
        tcpConn = TcpConn(conn, addr)
        tcpConn.start()
