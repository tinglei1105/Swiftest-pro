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
        port = client_address[1]
        
        print('start sending')
        start_time = time.time()
        while time.time() - start_time < self.sendingTime and self.ctl.stop != True:
            t = time.time() - start_time
            if self.byte_count <= self.speed/8*1024*1024*t - DATAGRAM_SIZE:
                self.byte_count += self.udpSock.sendto(bytes(getDatagram(), encoding='ascii'), (self.ip, port))
        print('client: %s:%s, duration: %f s, byte_count: %.2f (%.2f MB), setting speed: %.2f' % (self.ip, port, time.time() - start_time, self.byte_count, self.byte_count/1024/1024, self.speed))


class TcpConn(threading.Thread):
    def __init__(self, conn, addr):
        threading.Thread.__init__(self)
        self.conn = conn
        self.addr = addr
        self.stop = False
    
    def run(self):
        while True:
            data = conn.recv(1024)
            msg = data.decode('utf-8')
            msg = msg.split('-')
            if msg[0] == 'SET':
                speed = int(msg[1]) # Mbps
                sendingTime = int(msg[2]) # ms
                sender = Sender(self.addr, speed, sendingTime, self)
                sender.start()
            if msg[0] == 'FIN':
                self.stop = True
                self.conn.close()
                print('Connection from %s:%s closed.' % self.addr)
                break

if __name__ == '__main__':
    host = socket.gethostname()
    print(host)
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
