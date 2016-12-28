# Zookeeper管理员指南

## 部署

本章节包含了Zookeeper部署信息以及以下主题：

* 系统要求
* 集群设置
* 单机服务和开发

前两个部分假定你在生产环境如数据中心安装Zookeeper。最后的部分包含你在有限的基础上安装Zookeeper - 评估、测试或者开发 - 而不是在生产

### 系统要求

**支持的平台**

* GNU/LINUX支持服务端和客户端的开发和生产。
* Sun Solaris支持服务端和客户端的开发和生产。
* FreeBSD只支持客户端的开发和生产。Java NIO选择器在FreeBSD JVM已经不支持了。
* Win32只支持服务端和客户端的开发。
* MacOSX只支持服务端和客户端的开发。

**必要的软件**
Zookeeper运行在JDK1.6或更高版本以上。它作为Zookeeper服务的整体运行。对于整体，三个Zookeeper服务器是最小的推荐数量，并且我们建议它们运行在不同的机器上。在Yahoo,Zookeeper通常部署到专用的RHEL容器，双核处理器，2GB内存，80GB的硬盘驱动。

### 集群设置

对于可靠的Zookeeper服务，你应该部署Zookeeper在一个集群环境里。只要集群的多数服务可用，集群服务就是可用的。因为Zookeeper要求一个大多数，进群最好使用奇数个机器。例如，4台主机的Zookeeper只可以处理单机的故障；如果两个主机故障，剩下的两个机器不能成为大多数。然而，5台主机的Zookeeper可以处理两个机器的故障。

下面是设置成为集群服务的步骤。这些步骤应该在集群里的每台主机上执行：

1、安装Java JDK。你可以使用系统的原生包装系统，或者从JDK下载：[jdk](http://java.sun.com/javase/downloads/index.jsp)
2、设置Java堆大小。这对于避免swapping非常重要，它将严重降低Zookeeper的性能。确定正确值，使用负载测试，并确定低于引起swap的的限制。保守的 - 对于4GB的机器使用最大堆大小3GB。
3、安装Zookeeper服务包。可以从以下地址下载：[zookeeper_download](http://zookeeper.apache.org/releases.html)
4、新建一个配置文件。这个文件可以叫任何名字。使用下面的设置作为出发点：

```
tickTime=2000
dataDir=/var/lib/zookeeper/
clientPort=2181
initLimit=5
syncLimit=2
server.1=zoo1:2888:3888
server.2=zoo2:2888:3888
server.3=zoo3:2888:3888
```

你可以在配置参数部分找到这些配置的含义和其他的配置项。这里有以下几点要解释：Zookeeper集群的每个主机应该知道集群里的每个主机。通过server.id=host:port:port的形式来完成。参数host和port非常明确。通过为每个主机创建一个myid文件把server id归于每个主机，这个文件放在配置文件参数dataDir指定的data目录里。

5、myid文件只包含主机id的文字。所以服务器1的myid只包含文字"1"，没有其他任何东西。id值在集群中必须是唯一的并且应该在1到255之间。
6、如果你设置了配置文件，你可以启动Zookeeper服务：

```
$ java -cp zookeeper.jar:lib/slf4j-api-1.6.1.jar:lib/slf4j-log4j12-1.6.1.jar:lib/log4j-1.2.15.jar:conf \ org.apache.zookeeper.server.quorum.QuorumPeerMain zoo.cfg
```

QuorumPeerMain 启动Zookeeper服务，同事还注册JMX管理beans他允许通过JMX管理控制台管理。Zookeeper JMX document包含JMX管理Zookeeper的详细说明。

7、测试一下是否连接成功，在java中，你可以运行一下以下的这条简单命令：

```
$ bin/zkCli.sh -server 127.0.0.1:2181
```

## 单机服务器和开发人员设置

如果你以开发的目的安装Zookeeper，你可以向安装Zookeeper的单机服务器实例，然后安装Java或C客户端类库绑定到你的开发机。

安装单服务器实例的步骤可上面的类似，除了配置文件更简单了。你可以在Zookeeper入门指南的安装和运行Zookeeper单机模式的章节里找到完整说明。

安装客户端类库的资料，参考Zookeeper开发人员指南的绑定章节。

## 管理

本章节包含关于运行和维护Zookeeper的资料，并包含了这些主题：

* 设计Zookeeper部署
* 配备
* Zookeeper的优势和局限
* 管理
* 维护
* 监管
* 监控
* 记录
* 故障排除
* 配置参数
* Zookeeper命令：四字母单词
* 数据文件管理
* 避免事情
* 最佳实践

### 设计Zookeeper部署

Zookeeper的可靠性取决于两个基本设想。

* 只有少数服务会失败。这里的Failure意思是主机损坏，或者在网络里发生一些错误使服务从多数里分开。
* 部署机器操作正确。操作正确意思是正确的执行代码，有正常工作的时钟，且有一致性执行的网络组件存储。

下面的章节包含Zookeeper管理员最大化这些设想成为事实的概率的注意事项。其中的一些是跨机的注意事项，以及在部署里每个机器的其他一些应该考虑的事情。

#### 跨机器要求

为了Zookeeper服务是可用的，可以互相通信的机器必须是多数可用的。创建一个可以容错F个机器的部署，你应该部署2*F+1个机器。因此，一个由三台机器组成的部署可以处理一个故障，一个五台机器组成的部署可以处理两个故障。注意一个由6台机器组成的部署只可以处理两个故障因为3台机器不是多数。由于这个原因，Zookeeper部署通常由奇数个机器组成。

为了实现容错的最大概率你应该尝试使机器故障独立。例如，如果多数的机器共享相同的网关，网关故障可能引起一个关联故障并降低服务。共享电路、冷却系统也是如此，等等。

#### 单机要求

如果Zookeeper必须和其他应用竞争访问资源如存储介质、CPU、网络或内存，它的性能将会受到显著影响。Zookeeper有强大的耐用性保证，意味着他使用存储介质在操作允许完成之前记录变化。你应该意识到这个依赖性，并且用心确保Zookeeper操作没有通过介质绑定。给你一些降低这些的建议：

* Zookeeper的事务日志必须在专用设备上。(一个专用分区不够。)Zookeeper按顺序写日志，和其他应用分享你的日志设备可能引起寻找和争夺，这反过来又可能导致多次延迟。
* 不要将Zookeeper放置在可能引起swap的环境。为了Zookeeper的及时性功能，它简直不能允许swap。因此，确保最大的堆大小不是把大于真实可用内存给Zookeeper。关于这个更多资料，查看下面的Things to Avoid。

### 配备

### Zookeeper的优势和局限管理维护

Zookeeper集群的中长期维护你必须意识到：

#### 持续的数据目录清理

Zookeeper数据目录包含由服务存储的znode持久化拷贝文件。有快照和事务日志文件。znodes的变化追加到事务日志，偶尔当日志变大的时候，所有znode当前状态的快照写入到文件系统。这个快照取代所有的以往日志。

使用默认配置的时候，Zookeeper服务器不会删除老版本快照和日志文件(查看下面的autopurge)，这是操作员的责任。每个服务环境都是不同的因此管理这些文件的要求可能是不同的。

PurgeTxnLog功能实现了一个管理员可以使用的简单保留策略。API docs包含调用规则的详细资料。

在下面的例子中最近一次的快照统计和他们相应的日志被保留并删除其他的。典型的值应该大于3。这可以在Zookeeper服务器上运行一个计划任务清理每天的日志。

```
java -cp zookeeper.jar:lib/slf4j-api-1.6.1.jar:lib/slf4j-log4j12-1.6.1.jar:lib/log4j-1.2.15.jar:conf org.apache.zookeeper.server.PurgeTxnLog-n
```

自动的清除快照和对应事务日志在3.4.0版本引入并且可以通过下面的配置参数启用：autopurge.snapRetainCount和autopurge.purgeInterval。关于这个的更多信息，查看下面的高级配置。

#### 调试日志清理(log4j)

查看本文档的logging章节。它希望你使用内置的log4j功能设置滚动文件追加。发布文件tar包里的conf/log4j.properties提供了一个这样的实例。

### 监理

你可能想有一个管理进程管理每个Zookeeper服务进程(JVM)。Zookeeper服务器的"fail fast"设计意味着如果发生错误他就停止(进程退出)。Zookeeper服务集群是高度可靠的，这意味着当服务器从集群离开他始终是可用的和服务请求。此外，因为集群是"自我修复的"，故障的服务器一旦重启，就会自动的重新加入集群而不需要人工干预。

监管进程像daemontoos或SMF(监管进程可以有其他的选择，可以根据个人喜好使用，这里只是两个例子)管理你的Zookeeper服务去吧如果进程非正常的离开他讲自动的重启并快速重新加入集群。

### 监控

Zookeeper服务可以通过两个途径监控：

* 通过使用4个字母单词命令
* JMX。查看你环境/要求适合的章节

### 记录

Zookeeper使用log4j1.2作为它的日志基础设施。Zookeeper默认的log4j.properties文件在conf目录里。Log4j要求log4j.properties在工作目录里(Zookeeper运行目录)或可以从classpath可访问的路径里。

获取更多资料，查看log4j手册的 Log4j Default Initialization Procedure

### 故障排除

因为文件损坏服务器不来；

因为Zookeeper服务器里的事务日志的一些文件损坏，服务器可能不能读取他的数据库且不能出现。在加载Zookeeper数据库时你会看到一些IOException。在这种情况下，确保集群中其他服务器是正常工作的。在命令端口使用"stat"命令查看他们是否是正常的。检查集群中其他所有服务是正常的之后，你可以继续清理损坏服务器的数据库。删除datadir/version-2和datalogdir/version-2的所有文件。重启服务器。

### 配置参数

Zookeeper的行为由Zookeeper配置文件控制。这个文件是这样设计的精确相同的文件可以由组成Zookeeper服务的所有服务器使用(假定磁盘格局都是一样的)。如果服务器使用不同的配置文件，必须确保所有不同配置文件的服务器列表。

### 最小配置

以下是必须定义在配置文件的最小配置关键词：

clientPort
监听客户端连接的端口；这是客户端尝试连接的端口。

dataDir
Zookeeper存储内存数据库快照的位置，除非有特殊说明，更新的事务日志也存储进数据库。

**注意**
注意你存放事务日志的地方。一个专用的事务日志装在是保持良好性能的关键。存放日志到忙碌的装置会严重影响性能。

tikeTime
单个tick的长度，它是Zookeeper使用的基本时间单位，以毫秒为计量单位。它用于控制心跳和超时。例如，最小的session超时是两倍ticks。

### 高级配置

本部分的配置是可选的。你可以使用它们进一步优化Zookeeper服务的运行状况。有一些可以通过Java系统属性设置，一般是zookeeper.keyword的形式。精确的可用属性，在下面提到。

dataLogDir
(没有Java系统属性)
这个选项将决定机器将事务日志写到dataLogDir而不是dataDir。这允许使用专用的日志设备，帮助你避免日志和快照之间的竞争。

**注意**
专用的日志设备对吞吐量和稳定的延迟有很大的影响。强烈建议你使用专用的日志设备并设置dataLogDir指向那个设备的目录，然后确保指定dataDir不在那个设备上。

globalOutstandingLimit
(Java系统属性：zookeeper.globalOutstandingLimit)
客户端提交请求的速度快于Zookeeper处理他们的速度，特别是如果有很多客户端。为了防止Zookeeper由于排队请求而耗尽内存，Zookeeper将限制客户端在系统里最多只有不超过globalOutstandingLimit个未完成的请求。默认限制是1000。

preAllocSize
(Java系统属性：zookeeper.preAllocSize)
为了避免在preAllocSize kb的块的事务日志里寻找分配的空间。默认的块大小是64M。改变块大小的原因之一是如果快照更多就减少块大小(查看snapCount)。

snapCount
(Java系统属性：zookeeper.snapCount)
Zookeeper记录事务到一个事务日志。snapCount个事务写入日志文件之后启动一个快照并创建一个新的事务日志文件。默认的snapCount是100000。

traceFile
(Java系统属性：requestTraceFile)
如果定义了这个选项，请求将会被记录的名为traceFile.year.month.day的跟踪文件。使用这个选项提供有用的调试信息，但是会影响性能。(注意：系统属性没有zookeeper前缀，并且配置变量名和系统属性不一样。是的 - 是不一致的，让人讨厌)

maxClientCnxns
(没有Java系统属性)
限制单个客户端的并发连接数(在socket级别)，通过IP地址识别，Zookeeper集群的单个成员。这用于预防某些类的Dos攻击，包括文件描述符耗尽。默认是60.设置为0是完全的删除并发连接的限制。

clientPortAddress
3.3.0新属性：监听客户端连接的地址(ipv4,ipv6或hostname)；换言之，客户端尝试连接的地址。这是可选的，默认的我们以绑定的方式在服务器上任何连接到clientPort的address/interface/nic都将被接受。

minSessionTimeout
(没有Java系统属性)
3.3.0新属性：服务器允许客户端交涉的最小session超时时间。默认是2倍的tickTime。

maxSessionTimeout
(没有Java系统属性)
3.3.0新属性：服务器允许客户端交涉的最大session超时时间。默认是20倍的tickTime。

fsync.warningthresholdms
(Java系统属性：fync.warningthresholdms)
3.3.4新加入：事务日志所用时间大于这个值的时候输出警告消息。值的单位是毫秒默认值是1000.这个值只可以作为系统属性设置。

autopurge.snapRetainCount
(没有Java系统属性)
3.4.0新加入：启用时，Zookeeper自动清洗功能保留autopurge.snapRetainCount个最近的快照和各自dataDir/dataLogDir对应的事务日志并删除先前的。默认值是3.最小值是3.

autopurge.purgeInterval
(没有Java系统属性)
3.4.0新加入：触发清洗任务的时间间隔，以小时为单位。设置正整数(1或更高)启用自动清洗。默认是0。

syncEnabled
(Java系统属性：zookeeper.observer.syncEnabled)
3.4.6,3.5.0更新：观察者记录事务和快照到硬盘。这降低观察者重启的恢复时间。设置"false"禁用这个功能。默认是"true"

### 集群选项

本章节的选项设计用于服务器的全员 -- 换句话说，部署集群时。

electionAlg
(没有Java系统属性)
用于选举实现。"0"代表原始的UDP版本，"1"代表快速领导者选举的不授权的UDP版本，"2"代表授权的快速领导者选举的UDP版本，"3"代表快速领导者选举的TCP版本。当前，默认值是3.

**注意**
领导者选举0,1,2的实现现在是弃用的。我们计划在下个发布版本里移除他们，到那时只有FastLeaderElection可用。

initLimit
(没有Java系统属性)
时间数量，单位是ticks，允许追随者连接和同步leader。根据需要增加这个值，如果Zookeeper管理的数据量非常大。

leaderServers
(Java系统属性：zookeeper.leaderServers)
Leader接受客户端连接。默认值是"yes"。leader机器协调更新。默认是yes，它意味着leader接受客户端连接。

**注意**
Zookeeper集群大于3台机器时强烈建议开启领导者选举。

server.x=[hostname]:nnnnn[:nnnnn]，等等
(没有Java系统属性)
组成Zookeeper机器的服务器。当服务启动时，通过查找数据目录里的myid文件确定它是哪个服务器。那个文件包含服务器号，ASCII的形式，并且应该匹配左边server.x里的x。
使用Zookeeper服务的服务器的客户端必须匹配每个Zookeeper服务。

有两个端口号nnnnn。第一个用于连接领导者，第二个用于领导者选举。领导者选举端口只在electionAlg是1、2、3时必要。如果electionAlg是0，第二个端口就没必要。如果你想在单机上测试多服务，每个服务要使用不同的端口。

syncLimit
(没有Java系统属性)
时间总数，以ticks为单位，运行追随者同步Zookeeper。如果追随者失败太久，就掉线。

group.x=nnnnn[:nnnnn]
(没有Java系统属性)
启用分层的法定人数构造。"x"是group标识符,"="后面的数字对应服务标识符。左边的任务是冒号分隔的服务标识符。注意groups必须是不相交的并且所有groups联盟必须是Zookeeper全体。

weight.x=nnnnn
(没有Java系统属性)
兼用"group"，当形成法定人数时它给服务分配一个权重。投票时这个值对应服务的权重。Zookeeper的一些部分要求投票如领导者选举和自动广播协议。默认服务器的权重是1.如果配置定义了groups，但不是权重，将会给所有的服务器权重设置为1.

cnxTimeout
(Java系统属性：zookeeper.cnxTimeout)
设置领导者选举通知打开连接的超时值。只适用于electionAlg3。

**注意**
默认值是5秒

### 身份认证 & 授权选项

本章节的选项允许通过服务执行控制身份认证/授权。

zookeeper.DigestAuthenticationProvider.superDigest
(Java系统属性：zookeeper.DigestAuthenticationProvider.superDigest)
默认这个功能是禁用的

3.2新加入：使用zookeeper全体管理员能够以超级用户的身份访问znode。特别的是对于授权超级用户的用户没有ACL检查

org.apache.zookeeper.server.auth.DigestAuthenticationProvider 可以用于生产superDigest，用"super:"的参数调用它。规定生成的"super:"作为系统属性值当启动每个服务时。

当授权Zookeeper服务通过"digest"的计划和"super:"的authdata时。注意digest认证 通过服务，它是谨慎的做法在本地使用这个授权方法或在安全协议之上。

### 实验性的选项/功能

目前处于试验阶段的新特性

只读模式的服务

(Java系统属性：readonlymode.enabled)

3.4.0加入：设置为ture启用只读模式支持(默认不启用)。在这个模式里ROM客户端可以从

ZK服务里读取数据，但是不能写入数据和从其他客户端看到变化。查看ZOOKEEPER-784查看更多。

### 不安全选项

下面的选项可以用，但用的时候要小心。每个的风险说明和变量一起。

forceSync
(Java系统属性:zookeeper.forceSync)
在完成处理更新之前要求通过更新事务日志的媒介。如果这个选项设置为no，Zookeeper将不要求同步更新媒介。

jute.maxbuffer:
(Java系统属性：jute.maxbuffer)
此选择系可以通过Java系统属性设置。没有zookeeper前缀。它指定znode中可以存储的数据的最大值。默认是0xffffff,或低于1M。如果这个选项变了，系统属性必须在所有服务器和客户端上设置，否则会出现问题。这确实是一个合理性检查。Zookeeper用来存储数据大约在KB大小。

skipACL
(Java系统属性：zookeeper.skipACL)
跳过ACL检查。这会增加吞吐量，但是会像所有人打开完整的访问权限

quorumListenOnAllIPs
设置为true时，Zookeeper服务器将监听所有同行可用IP地址的连接，并且不只是服务器里配置的地址。它影响连接处理ZAB协议和快速领导者选举协议。默认是false。

### 使用Netty框架通信

3.4新加入：Netty是一个基于NIO的客户端/服务端通信框架，它简化了Java应用很多复杂的网络层通信。此外Netty框架支持数据加密(SSL)和身份验证(证书)。还有可选择的功能可以独立的开启或关闭。

在版本3.4之前Zookeeper一直直接使用NIO，然而在版本3.4和以后的版本支持NIO选择Netty。NIO仍然是默认值，然而基于Netty的通信可以通过设置环境变量"zookeeper.serverCnxnFactory"为"org.apache.zookeeper.server.NettyServerCnxnFactory"使用。可以在客户端或服务端设置这个选项，一般两边都设置，这个由你决定。

TBD - netty的调优选项

TBD - 怎么管理数据加密

TBD - 怎么管理证书

### Zookeeper命令：四个字母的单词

Zookeeper支持一小组命令。每个命令由四个字母组成。你通过telnet或nc在客户端端口发行命令到Zookeeper。

三个比较有趣的命令："stat"给出了服务器和连接的客户端的一般信息，"srvr"和"cons"分别提供服务器上和连接的扩展细节。

conf
3.3.0加入：打印服务配置详情

cons
3.3.0加入：列出所有连接到这个服务器的客户端完整的connection/session详情。包括接收/发送数据包的数量，session id，操作延迟，最后执行的操作，等等。

crst
3.3.9加入：重置所有连接的统计信息。

dump
列出未交付的session和临时节点。这只适用于领导者。

envi
打印服务环境详情。

ruok
测试服务器是否正常运行在一个无错误状态。如果正在运行服务器回复imok。否则不响应。"imok"的回复并不表明服务器已经加入quorum，至少说服务进程是活动的并绑定到指定的客户端端口。使用"stat"获取详细信息。

srst
重置服务器统计

srvr
3.3.0加入：列出服务器的完整详情。

stat
列出服务器和连接的客户端摘要信息。

wchs
3.3.0加入：列出服务器watch的摘要信息。

wchc
3.3.0加入：列出服务器watch的详细信息，通过session。这个输出相关watch的session清单。注意，这个选项取决于watch的数量可能开销比较大，使用时要小心。

wchp
3.3.0加入：列出服务器watch的详细信息，通过路径。这个输出相关session的路径清单。注意，这个选项取决于watch的数量可能开销比较大，使用时要小心。

mntr
3.4.0加入：输出可以监控集群状态的变量清单。

```
$ echo mntr | nc localhost 2185
zk_version  3.4.0
zk_avg_latency  0
zk_max_latency  0
zk_min_latency  0
zk_packets_received 70
zk_packets_sent 69
zk_outstanding_requests 0
zk_server_state leader
zk_znode_count   4
zk_watch_count  0
zk_ephemerals_count 0
zk_approximate_data_size    27
zk_followers    4                   - only exposed by the Leader
zk_synced_followers 4               - only exposed by the Leader
zk_pending_syncs    0               - only exposed by the Leader
zk_open_file_descriptor_count 23    - only available on Unix platforms
zk_max_file_descriptor_count 1024   - only available on Unix platforms
```

输出和java属性格式兼容并且内容可能随着时间变动。你的脚本应该改变。

这里有个ruok命令的实例

```
$ echo ruok | nc 127.0.0.1 5111
imok
```

### 数据文件管理

Zookeeper存储它的数据到一个数据目录和它的事务日志到一个事务日志目录。默认这两个目录是一样的。服务器可以配置存储事务日志文件到一个单独的目录里。使用专用的设备存储事务日志可以增加吞吐量和减缓延迟。

### 数据目录

数据目录里有两个文件：

myid - 包含一个单独的可读的整型数字ASCII文本，代表server id。

snatshop.- 保留数据树的模糊快照。

每个Zookeeper服务器有一个唯一id。这个id用于两个地方：myid文件和配置文件。myid文件标识服务器。配置文件通过它的server id暴露每个服务器的联系方式。当一个Zookeeper服务实例启动，它从myid文件读取它的id，然后使用这个id，从配置文件读取它应该监听的端口。

数据目录的snapshot文件是Zookeeper服务采取快照、更新的模糊快照。snapshot文件名字的后缀是zxid，在开启快照时最后提交的事务id。因此，快照包括在快照过程中数据树变化的子集。然后快照可能不对应任何数据树，因为这个原因我们叫它为模糊的快照。Zookeeper仍然可以使用这个快照因为它运用了更新的等幂性质。使用模糊的快照回复事务日志Zookeeper得到日志结尾的系统状态。

### 日志目录

日志目录包含Zookeeper事务日志。在任何更新发生之前，Zookeeper确保代表更新的事务写入不易丢失的存储。每次启用新的日志文件快照就开始。日志文件的后缀是写到日志的第一个zxid。

### 文件管理

快照和日志文件的格式在单个Zookeeper服务器和复制的Zookeeper服务器的不同配置之间没有变化。因此，你可以从运行的Zookeeper服务器上拉取这些文件到部署的机器。

使用老的日志和快照文件，你可以考虑Zookeeper服务的上一个状态和恢复状态。LogFormatter类可让管理员查看日志里的事务。

Zookeeper服务创建快照和日志文件，当从不删除它们。数据和日志文件的保留策略由Zookeeper服务以外的实现。服务本身只需要最后完整的模糊快照和日志文件。查看maintennance章节可以了解Zookeeper存储维护的保留策略设置。

### 要避免的事情

下面的问题可以通过Zookeeper正确的配置避免：

不一致的服务器清单

客户端使用的Zookeeper服务器清单必须和每个Zookeeper服务的一致。每个Zookeeper服务配置文件的服务器清单应该和其他的一致。

不正确的事务日志放置

Zookeeper性能的关键部分是事务日志。Zookeeper在响应之前同步事务到媒介。一个专用的事务日志装置是保持良好性能的关键。将日志放置在繁忙的装置上会严重影响性能。如果你只有一个存储设备，将跟踪文件放置在NFS上并增加snapshotCount；这不能解决问题，但能改善。

不正确的Java堆大小

你应该特别注意正确的设置Java最大堆大小。特别的是，你不应该营造Zookeeper交换磁盘的情况。磁盘可让Zookeeper死亡。每个事情都是有序的，所以如果处理一个请求交换磁盘，所有其他队列里的请求很可能会做同样的事情。磁盘，不要SWAP。

保守估计：如果你有4G的RAM，不用设置Java最大堆大小为6G或4G。例如，4G的机器更建议你使用3G的堆，因为操作系统和缓存也需要内存。堆大小的最佳推荐，你的系统需要运行负载测试，然后确保使用在引起系统交换的限制以下。

### 最佳实践

为了获得最佳效果，注意下面的优秀Zookeeper实践清单：

对于multi-tennant安装查看Zookeeper"chroot"支持的详细章节，这对部署多个应用/服务连接到单个Zookeeper集群非常有用。