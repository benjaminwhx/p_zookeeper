# Zookeeper简介

## Zookeeper: 分布式调度服务

Zookeeper是一个分布式的、开源的分布式应用协调服务。它暴露了一组简单的基础原件，分布式应用可以在这些原件之上实现更高级别的服务，如同步、配置维护、群组、和命名。它被设计成容易编程实现的，并且使用一个常见的文件系统的树型结构的数据模型。它运行在Java中，并且绑定了Java和C。

众所周知，协调服务很难做对。它们特别容易发生像文件竞争条件问题和死锁的错误。Zookeeper的动机就是缓解分布式应用从头开始实现分布式协调服务的责任。

## Design Goals（设计目标）

**简单**，Zookeeper允许程序通过一个共享的类似于标准文件系统的有组织的分层命名空间分布式处理协调。命名空间包括：数据寄存器 - 在Zookeeper中叫znodes, 它和文件、目录一样。和一般文件系统的不同之处是，它的设计就是为了存储，Zookeeper的数据保持在内存中，这就意味着它可以实现高吞吐量和低延迟的数据。
       
Zookeeper的实现提供了一个优质的高性能、高可用，严格的访问顺序。Zookeeper的性能方面意味着它可以用于大型的分布式系统。可靠性方面防止它成为一个单点故障。严格的顺序意味着可以在客户端实现复杂的同步原件。

**复制**，像分布式处理一样，Zookeeper自己在处理协调的时候要复制多个主机。

![Zookeeper Service](http://zookeeper.apache.org/doc/trunk/images/zkservice.jpg)

Zookeeper服务的组成部分必须彼此都知道彼此，它们维持了一个内存状态影像，连同事务日志和快照在一个持久化的存储中。只要大多数的服务器是可用的，Zookeeper服务就是可用的。

客户端连接到一个单独的服务。客户端保持了一个TCP连接，通过这个TCP连接发送请求、获取响应、获取watch事件、和发送心跳。如果这个连接断了，会自动连接到其他不同的服务器。

**序列**。Zookeeper用数字标记每一个更新，用它来反射出所有的事务顺序。随后的操作可以使用这个顺序去实现更高级的抽象，例如同步原件。

**快速**。它在"Read-dominant"工作负载中特别快。Zookeeper 应用运行在数以千计的机器上，并且它通常在读比写多的时候运行的最好，读写大概比例在10:1。

## Data model and the hierarchical namespace（数据模型和分层的命名空间）

Zookeeper提供的命名空间非常像一个标准的文件系统。一个名字是一系列的以'/'隔开的一个路径元素。Zookeeper命名空间中所有的节点都是通过路径识别。

![ZooKeeper's Hierarchical Namespace](http://zookeeper.apache.org/doc/trunk/images/zknamespace.jpg)

## Nodes and ephemeral nodes（节点和临时节点）

不像标准的文件系统，Zookeeper命名空间中的每个节点可以有数据也可以有子目录。它就像一个既可以是文件也可以是目录的文件系统。(Zookeeper被设计成保存协调数据：状态信息，配置，位置信息，等等，所以每个节点存储的数据通常较小，通常在1个字节到数千字节的范围之内)我们使用术语znode来表明Zookeeper的数据节点。

znode维持了一个stat结构，它包括数据变化的版本号、访问控制列表变化、和时间戳，允许缓存验证和协调更新。每当znode的数据有变化，版本号就会增加。例如，每当客户端检索数据时同时它也获取数据的版本信息。

命名空间中每个znode存储的数据自动的读取和写入的，读取时获得znode所有关联的数据字节，写入时替换所有的数据。每个节点都有一个访问控制列表来制约谁可以做什么。

Zookeeper还有一个临时节点的概念。这些znode和session存活的一样长，session创建时存活，当session结束，也跟着删除。

## Conditional updates and watches（条件的更新和watches）

Zookeeper支持watches的概念。客户端可以在znode上设置一个watch。当znode发生变化时触发并移除watch。当watch被触发时，客户端会接收到一个包说明znode有变化了。并且如果客户端和其中一台server中间的连接坏掉了，客户端就会收到一个本地通知。这些可以用来[tbd]。

## Guarantees（保证）

ZooKeeper非常快和非常简单。然而它一直以来的目标，是作为更多复杂服务结构的基础，例如同步，提供一系列的保证。它们是：

* 顺序一致性 - 来自客户端的更新会按顺序应用。
* 原子性 - 更新成功或者失败，没有局部的结果产生。
* 唯一系统映像 - 一个客户端不管连接到哪个服务端都会看到同样的视图。
* 可靠性- 一旦一个更新被应用，它将从更新的时间开始一直保持到一个客户端重写更新。
* 时效性：系统的客户端视图在一个特定的时间里都保证是最新的。
* 获取更多的信息，了解如何使用它们，请看[tbd]。

## Simple API（简单的API）

Zookeeper的设计宗旨之一就是提供一些非常简单的API接口，例如，它提供了下面这些操作：

create（创建）
&emsp;&emsp;在树结构为止中创建一个节点

delete（删除）
&emsp;&emsp;删除一个节点

exists（判断是否存在）
&emsp;&emsp;判断当前为止上是否存在一个节点

get data（获取数据）
&emsp;&emsp;从一个节点读取数据

set data（设置数据）
&emsp;&emsp;往一个节点里面写入数据

get children（获得子集）
&emsp;&emsp;获得子节点集合

sync（同步）
&emsp;&emsp;等待数据同步到每个节点上

深入讨论这些方法以及如何用它们实现更高级别的操作，请参考[tbd]

## Implementation（实现）

ZooKeeper Components 展示了ZooKeeper服务的高级组件。除了请求处理器的异常之外，组成ZooKeeper服务的每个服务器复制它们自己组件的副本。

![ZooKeeper Components](http://zookeeper.apache.org/doc/trunk/images/zkcomponents.jpg)

Replicated database是一个内存数据库。它包含全部的数据树。为了可恢复性，更新记录保存到磁盘上，并且写入操作在应用到内存数据库之前被序列化到磁盘上。

每个Zookeeper 服务端服务客户端。客户端正确的连接到一个服务器提交请求。每个服务端数据库的本地副本为读请求提供服务。服务的变化状态请求、写请求，被一个协议保护。

作为协议的一部分，所有的写操作从客户端转递到一台单独服务器，称为leader。其他的Zookeeper服务器叫做follows，它接收来自leader的消息建议并达成一致的消息建议。消息管理层负责在失败的时候更换Leader并同步Follows。

Zookeeper使用了一个自定义的原子消息协议。因为消息层是原子的，Zookeeper可以保证本地副本从不出现偏差。当leader接受到一个写请求，它计算写操作被应用时系统的状态，并将捕获到的新状态转化进入事务。

## Uses（使用）

程序接口实现Zookeeper非常简单。然而，用它你可以实现更高级的操作，如同步基本数据，组成员，所有权，等等。一些分布式应用已经使用了它，[tbd,从白皮书和视频教程添加使用]，获取更多信息，查看[tbd]

## Performance（性能）

ZooKeeper是设计成高性能的，但是真的这样么？ZooKeeper在雅虎的研发团队研究结果显明它真的如此。(看[ZooKeeper Throughput as the Read-Write Ratio Varies](http://zookeeper.apache.org/doc/r3.4.6/zookeeperOver.html#fg_zkPerfRW))应用在读取性能上表现地写性能高得多，因为写操作要涉及所有服务器的同步。（在调度服务中读性能超过写性能是普遍的情况）

![ZooKeeper Throughput as the Read-Write Ratio Varies](http://zookeeper.apache.org/doc/trunk/images/zkperfRW-3.2.jpg)

上图是Zookeeper3.2版本在dual 2Ghz Xeon and two SATA 15K RPM 驱动配置的服务器上的吞吐量图像。一个驱动作为专门的Zookeeper日志装置。快照写进操作系统驱动。1k的写请求和1K的读取请求。"Servers" 表明了Zookeeper全体的大小，组成Zookeeper服务的服务器数量。接近于30台机器模仿客户端。Zookeeper全体被配置为leaders不允许客户端连接。
 
 ## Note（说明）
 
相对于3.1的发布版本，在3.2的版本中读写性能都改善了。
 
基准测试也表明它的可靠性。[Reliability in the Presence of Errors](http://zookeeper.apache.org/doc/r3.4.6/zookeeperOver.html#fg_zkPerfReliability) 展示了部署的框架如何应用各种失效。下面是图像中标志的事件：
 
 * follower的失效和恢复
 * 不同的follower的失效和恢复
 * leader的失效
 * 两个follower的失效和恢复
 * 另一个 leader 的失效
 
 ## Reliability（可靠性）
 
 展示运行在7台机器上的ZooKeeper服务在故障发生后随着时间的推进系统的行为。我们运行跟上面测试同样的环境上，但这次只保持30%的写入，保持在一个保守的负载。
 
 ![Reliability in the Presence of Errors](http://zookeeper.apache.org/doc/trunk/images/zkperfreliability.jpg)
 
 从图表中我们得到一些重要的观察。第一，如果followers失效和迅速恢复，zooKeeper能够保持一个高吞吐量无视失效。但是可能重要的是，leader选举算法允许系统快速恢复来避免吞吐量的大幅下降。在我们的观察当中，ZooKeeper只需要不到200ms来选举中一个新的leader。第三，随着follower恢复，ZooKeeper能够提高吞吐量一旦他们开始处理请求。
 
 ## The ZooKeeper Project（ZooKeeper项目）
 
 ZooKeeper已经成功运行在许多单独的项目中。它被Yahoo！用来作为Yahoo！消息中间件，一个具有高可扩展性的用于管理上千个话题的复制和数据传输的发布-订阅系统的调度和失效恢复服务。也用在Yahoo！爬虫程序中管理失效恢复。大量的Yahoo！广告系统也它来实现可靠地服务。
 
 
