#!/bin/bash
ip_list=( "81.70.55.189"
    "81.70.193.140"
    "110.42.169.86"
    "121.5.26.137"
    "124.223.35.212"
)
for ip in ${ip_list[@]}; do
 echo "try to connect http://$ip:8080/ping"
 curl "http://$ip:8080/ping" --max-time 3
done