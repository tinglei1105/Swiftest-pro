import threading
import socket
import time
from typing import Dict, Any

SEND_THREAD_NUM = 1
BUFFER_SIZE = 2048


raw_data = ''
for i in range(BUFFER_SIZE):
    raw_data = raw_data + '0'

class SendThread(threading.Thread):
    def __init__(self, sock:socket.socket):
        threading.Thread.__init__(self)
        self.sock = sock
        self.byte_count = 0

    def run(self):
        start_time = time.time()
        while time.time() - start_time < 10:

            self.byte_count += self.sock.send(bytes(raw_data, 'ascii'))
        print("sent",self.byte_count)
        self.sock.close()


def get_host_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()

    return ip




if __name__ == '__main__':
    print(get_host_ip())
    server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_sock.bind(('0.0.0.0', 9876))
    server_sock.listen()
    try:
        while True:
            conn,client_address = server_sock.accept()
            client_name = str(client_address[0]) + ':' + str(client_address[1])
            print(client_name)
            thread = SendThread(conn)
            thread.start()
            # print('receive package, data=%s, address=%s' % (data, str(client_address)))


    finally:
        server_sock.close()
