 #!/bin/bash
rm -rf /usr/local/go && tar -C /usr/local -xzf go1.19.4.linux-amd64.tar.gz
# Do this in $HOME/.profile
export PATH=$PATH:/usr/local/go/bin
go version