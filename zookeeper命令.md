# zookeeper命令

## 1、服务器相关命令
```
# 启动服务器
bin/zkServer.sh start-foreground z1/z1.cfg

# 查询服务器状态，在集群模式中，Mode显示的是leader，还可能是follower，而在单机模式中，Mode显示的是standalone。一个服务器根据不同配置启动了多个，需要在status后面指定配置文件
bin/zkServer.sh status z2/z2.cfg

# 启动客户端
sh bin/zkCli.sh -server 127.0.0.1:2181
```

## 2、客户端命令
version代表之前的版本，类似乐观锁。
```
# -s或-e分别指定节点特性：顺序或临时节点。默认创建持久节点。
create [-s] [-e] path data acl

# 列出指定节点下的第一级的所有子节点
ls path [watch]

# 获取指定节点的数据内容和属性信息
get path [watch]

# 更新指定节点的数据内容
set path data [version]

# 删除指定节点
delete path [version]
```