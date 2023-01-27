#!/bin/bash
cd masterServer/
rm -f masterServer
go build
cd ../pingServer/
rm -f pingServer
go build
cd ..
ip_list=( "81.70.55.189"
    "81.70.193.140"
    "110.42.169.86"
    "121.5.26.137"
    "124.223.35.212"
)
export SSHPASS=Fastbts123
for ip in ${ip_list[@]}; do
    sshpass -e ssh -o StrictHostKeyChecking=no "ubuntu@$ip" "
        echo start config TestServer
        if [ ! -d /home/ubuntu/swifttest ];then
            mkdir /home/ubuntu/swifttest
        fi
    "
    sshpass -e scp ./testServer/server_flooding.py "ubuntu@$ip":/home/ubuntu/swifttest
     sshpass -e ssh -o StrictHostKeyChecking=no "ubuntu@$ip" "
     ps aux|grep pingServer |grep -v grep| awk '{print \$2}' |xargs kill -9
     "
    
    sshpass -e scp ./pingServer/pingServer "ubuntu@$ip":/home/ubuntu/swifttest
    sshpass -e ssh -o StrictHostKeyChecking=no "ubuntu@$ip" "
         cd /home/ubuntu/swifttest
    #nohup python3 -u server_flooding.py > flooding.log 2>&1 &
    nohup ./pingServer > ping.log 2>&1 &
    #     cat flooding.log
    #     cat ping.log
    "
    #  mkdir static
    #  cd static
    #  echo pong >ping
    #  dd if=/dev/zero of=GB  bs=1G count=1
done