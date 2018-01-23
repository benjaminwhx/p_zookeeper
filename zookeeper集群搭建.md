# zookeeper集群搭建

1、下载包并解压到`/usr/local`目录，全路径`/usr/local/zookeeper-3.4.10`

2、创建3个目录
```
cd /usr/local/zookeeper-3.4.10
mkdir z1
mkdir z2
mkdir z3
mkdir z1/data
mkdir z2/data
mkdir z3/data
```

3、更改配置文件
```
cp conf/zoo_sample.cfg conf/zoo.cfg
```

配置文件如下：
```
tickTime=2000
dataDir=/usr/local/data/zookeeper
clientPort=2181
```

4、配置集群配置
```
cp conf/zoo.cfg z1/z1.cfg
cp conf/zoo.cfg z2/z2.cfg
cp conf/zoo.cfg z3/z3.cfg
```

修改配置文件
```
## z1.cfg
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/usr/local/zookeeper-3.4.10/z1/data
clientPort=2181
server.1=127.0.0.1:2222:2223
server.2=127.0.0.1:3333:3334
server.3=127.0.0.1:4444:4445

## z2.cfg
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/usr/local/zookeeper-3.4.10/z2/data
clientPort=2182
server.1=127.0.0.1:2222:2223
server.2=127.0.0.1:3333:3334
server.3=127.0.0.1:4444:4445

## z3.cfg
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/usr/local/zookeeper-3.4.10/z3/data
clientPort=2183
server.1=127.0.0.1:2222:2223
server.2=127.0.0.1:3333:3334
server.3=127.0.0.1:4444:4445
```

## 4、增加myid文件
当启动一个服务器时，我们需要知道启动的是哪个服务器。一个服务器通过读取data目录下一个名为myid的文件来获取服务器id信息。
```
echo 1 > z1/data/myid
echo 2 > z2/data/myid
echo 3 > z3/data/myid
```

## 5、启动服务器
首先启动z1服务器，启动以后会报错，它会疯狂地尝试连接到其他服务器。
```
sh bin/zkServer.sh start-foreground z1/z1.cfg
```

接着，我们启动z2服务器和z3服务器。
```
sh bin/zkServer.sh start-foreground z2/z2.cfg
sh bin/zkServer.sh start-foreground z3/z3.cfg
```

## 6、客户端连接
```
sh bin/zkCli.sh -server 127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
```