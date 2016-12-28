# Zookeeper入门指南

## 简介

此文档包含了Zookeeper的快速入门指南。它主要的目的是让开发人员快速试用Zookeeper，且包含了单个Zookeeper服务器的简单安装介绍，一些验证运行的命令，和一个简单的编程实例。最后，为了方便，还有一些复杂的安装部分，例如运行主从复制的部署，和优化事务日志。然而对于商业部署的完整介绍，请参考[ZooKeeper Administrator's Guide](https://github.com/benjaminwhx/zookeeper-example/blob/master/translate/Admin%26Ops/Administrator's%20Guide.md)

## 前提条件

查看[Zookeeper Administrator's Guide](https://github.com/benjaminwhx/zookeeper-example/blob/master/translate/Admin%26Ops/Administrator's%20Guide.md)中的System Requirements 部分。

## 下载

获取Zookeeper的发布包，从Apache Download Mirros中下载一个最新的[稳定](http://zookeeper.apache.org/releases.html)版本。

## 单机操作

安装Zookeeper的单机模式非常简单。服务包含在一个单独的压缩文件中，所以安装只需要创建配置文件。

一旦你下载了一个Zookeeper的稳定的发布版本之后，解压并进入根目录。

启动Zookeeper之前需要一个配置文件。这是一个示例，创建一个文件在 `conf/zoo.cfg`:

```
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
```

这个文件本来可以叫任何名字，此时我们叫它conf/zoo.cfg。修改dataDir的值为一个已经存在的(空的开始)文件夹。下面是每个字段的含义：

**tickTime**
Zookeeper使用的基本时间，时间单位为毫秒。它用于心跳机制，并且设置最小的session超时时间为两倍心跳时间

**dataDir**
保存内存数据库快照信息的位置，如果没有其他说明，更新的事务日志也保存到数据库。

**clientPort**
监听客户端连接的端口。

现在你已经创建了配置文件，可以启动Zookeeper：

```
bin/zkServer.sh start
```

Zookeeper的日志信息使用log4j -- 更多详细信息可以在开发人员指南的 Logging 模块获取。你可以根据log4j的配置进入控制台查看日志信息(或日志文件)。
这里列出了Zookeeper独立运行模式的步骤。没有主从复制，所以如果Zookeeper进程故障，服务就会停止。这对于大多数的开发情况是可以的，想要运行Zookeeper的主从复制模式，请参见[Running Replicated ZooKeeper](http://zookeeper.apache.org/doc/trunk/zookeeperStarted.html#sc_RunningReplicatedZooKeeper).

## 管理Zookeeper存储

长时间运行的生产系统上的Zookeeper存储必须要在外部管理(datadir 和 logs)，查看[maintenance](http://zookeeper.apache.org/doc/trunk/zookeeperAdmin.html#sc_maintenance)部分获取更多信息。

## 连接Zookeeper

```
$ bin/zkCli.sh -server 127.0.0.1:2181
```

这将给你一个简单的shell脚本，在Zookeeper上执行文件系统的操作。

一旦连接上了，你可以看到一些信息：

```
Connecting to localhost:2181
log4j:WARN No appenders could be found for logger (org.apache.zookeeper.ZooKeeper).
log4j:WARN Please initialize the log4j system properly.
Welcome to ZooKeeper!
JLine support is enabled
[zkshell: 0]
```

从shell脚本，输入help，可以获取一个从客户端可执行的命令列表：

```
[zkshell: 0] help
ZooKeeper host:port cmd args
        get path [watch]
        ls path [watch]
        set path data [version]
        delquota [-n|-b] path
        quit
        printwatches on|off
        create path data acl
        stat path [watch]
        listquota path
        history
        setAcl path acl
        getAcl path
        sync path
        redo cmdno
        addauth scheme auth
        delete path [version]
        deleteall path
        setquota -n|-b val path

```

从这里，你可以尝试一些简单的命令行接口找到一些感觉。第一，通过发行的列表命令开始，像ls :

```
[zkshell: 8] ls /
[zookeeper]
```

下一步，通过运行create /zk_test my_data，创建一个新的znode。这将创建一个新的znode节点和一个相关联的字符串"my_data"：

```
[zkshell: 9] create /zk_test my_data
Created /zk_test
```

使用ls / 命令查看目录：

```
[zkshell: 11] ls /
[zookeeper, zk_test]
```

注意 zk_test目录已经被创建了。

接下来，使用 get 命令验证数据是否与znode关联上了:

```
[zkshell: 12] get /zk_test
my_data
cZxid = 5
ctime = Fri Jun 05 13:57:06 PDT 2009
mZxid = 5
mtime = Fri Jun 05 13:57:06 PDT 2009
pZxid = 5
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0
dataLength = 7
numChildren = 0
```

我们可以是用set命令改变数据与zk_test的关联。像：

```
[zkshell: 14] set /zk_test junk
cZxid = 5
ctime = Fri Jun 05 13:57:06 PDT 2009
mZxid = 6
mtime = Fri Jun 05 14:01:52 PDT 2009
pZxid = 5
cversion = 0
dataVersion = 1
aclVersion = 0
ephemeralOwner = 0
dataLength = 4
numChildren = 0
[zkshell: 15] get /zk_test
junk
cZxid = 5
ctime = Fri Jun 05 13:57:06 PDT 2009
mZxid = 6
mtime = Fri Jun 05 14:01:52 PDT 2009
pZxid = 5
cversion = 0
dataVersion = 1
aclVersion = 0
ephemeralOwner = 0
dataLength = 4
numChildren = 0
```

(注意我们使用get在setting data之后，并且它确实改变了。)

最后，让我们delete节点：

```
[zkshell: 16] delete /zk_test
[zkshell: 17] ls /
[zookeeper]
[zkshell: 18]
```

先就这样，想探索更多，继续这个文档的其他部分和查看[Programmer's Guide](https://github.com/benjaminwhx/zookeeper-example/blob/master/translate/developer/Programmer's%20Guide.md).

## Zookeeper编程

Zookeeer有一个java绑定和一个C绑定。它们在功能上是相同的。C绑定有两种形式：单线程和多线程。它们的区别仅仅是怎么循环消息。获取更多消息，查看[Programming Examples in the ZooKeeper Programmer's Guide](https://github.com/benjaminwhx/zookeeper-example/blob/master/translate/developer/Programmer's%20Guide.md)使用不同APIs的实例代码。

## 运行主从复制的Zookeeper

运行Zookeeper的独立模式方便评估、开发和测试。但是在生产中，你要运行Zookeeper的主从复制模式。相同应用中服务器主从复制组叫做 quorum , 并且在主从复制模式中，在quorum中的所有服务器有相同的配置文件副本。配置文件和使用独立模式相似，但有一点点的区别，如：

```
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
server.1=zoo1:2888:3888
server.2=zoo2:2888:3888
server.3=zoo3:2888:3888
```

新的条目，`initLimit` 是Zookeeper用它来限定quorum中的Zookeeper服务器连接到Leader的超时时间。`syncLimit`限制了一个服务器从Leader多长时间超时。
使用这两种超时，你指定的时间单位使用tickTime.在这个例子中，initLimit的超时时间是5个标记号，2000毫秒一个标记，就是10秒。
条目`server.x`列出了构成Zookeeper服务的服务器。当服务启动时，它通过查找data目录中的`myid`文件知道是哪个服务。这个myid个文件包含了服务号，用ASCII.
最后，注意每个服务器名称后面的两个端口号："2888"和"3888"。同事使用前面的端口连接到其他同事。这样的一个连接非常重要，以便于同事之间可以通讯，例如，对更新的顺序取得统一的意见。更具体的说，一个Zookeeper的服务器用这个端口连接follower到leader。当一个新的leader产生时，follower使用这个端口打开一个TCP连接，连接到leader。因为默认的leader选举也使用TCP。我们现在需要另一个端口用来leader选举。这是在服务器条目的第二个端口。

> 注意：如果你想在一台机器上测试多台服务器，在服务器配置文件为每个server.x指定servername为localhost,和独有的quorum & leader选举端口（也就是 2888:3888, 2889:3889, 2890:3890在上面的示例中）。当然分开dataDir和不同的clientPort也是非常重要的(在上面的主从复制示例中，在单个主机上运行，你仍然需要三个配置文件)。

## 其他优化

有一些其他的配置可以大大提高性能：

获取更新的低延迟，有一个专门的事务日志目录非常重要。默认情况下，事务日志作为一个数据快照和myid文件放入同一个目录。datalogDir参数指示一个不同的目录用于事务日志。