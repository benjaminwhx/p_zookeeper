# Zookeeper

## Zookeeper: 分布式调度服务

Zookeeper是一个为分布式应用提供分布式、开源的调度服务。它暴露一组简单的基本架构，分布式应用可以在其上面来实现高层次服务用于同步、维护配置、分组和命名。它涉及简单，易于编程。在类似于文件系统树结构目录下使用一个数据模型。它运行在Java环境上，绑定于Java和C。

调度服务是出了名的难，他们极易发生类似于竞态条件和思索的错误，ZooKeeper的动机是减轻分布式应用从零开始来实现调度服务的责任。

## Design Goals（设计目的）

**Zookeeper是简单的**，ZooKeeper允许分布式进程通过一个共享的跟标准文件系统相似的架构的层级命名空间来互相调度。命名空间包含称为znodes的数据寄存器（在ZooKeeper的说法中），这些类似于文件和目录。不像传统的文件系统被设计用于存储，ZooKeeper数据是保存在内存中，那就意味着ZooKeeper能够获得高吞吐量和低延迟。

ZooKeeper实现高性能、高可用性和严格的访问命令。性能方面意味着它可以用在大型分布式系统。可靠性方面使它避免了单点故障。严格的访问命令意味着复杂的同步原语可以在客户端实现。

**Zookeeper是可复制的**，像它所调度的分布式进程，ZooKeeper他本身也是可以被复制来构成一组集合。

![Zookeeper Service](http://zookeeper.apache.org/doc/trunk/images/zkservice.jpg)

构成ZooKeeper服务的所有服务器都必须知道彼此。它们维持着一个状态相关的内存图像，和事务日志和快照保存在一个持久化的仓库。只要大多数服务器是可用的，那么ZooKeeper服务就可用。

客户端与一个单独的服务器建立连接。它们之间通过发送请求、获得回复，获得观察事件和发送心跳来维持一个TCP连接。如果客户端与服务器的TCP连接断开了，那么客户端会去连接另一个服务器。

**Zookeeper是有序的**。ZooKeeper用一个数字来记录每个反映所有ZooKeeper事务的顺序。后续的操作可以使用顺序来实现高水平的抽象，例如同步原语。

**Zookeeper是快速的**。尤其是在读取性能特性明显。ZooKeeper应用运行在成千台机器上，并且它在读取上比写入表现得更好，比率大概为10:1

## Data model and the hierarchical namespace（数据模型和分层命名）

Zookeeper所提供的命名空间和标注你的文件系统很相似，以/分隔，每个节点以path名来识别。

![ZooKeeper's Hierarchical Namespace](http://zookeeper.apache.org/doc/trunk/images/zknamespace.jpg)

## Nodes and ephemeral nodes（节点和临时节点）

不像标准的文件系统，Zookeeper中的每个节点都拥有自己和它的子节点相关的数据，就像拥有一个文件系统一样允许一个文件作为一个目录，（ZooKeeper被设计为储存调度数据：状态信息，配置信息、位置信息等等，所以储存每个节点中的数据通常很小，在字节到千字节之间）。当我们讨论ZooKeeper数据节点时使用“znode”这个称呼使得表述清晰。

Znodes维持一个状态结构包括数据改变的状态码，ACL改变和时间戳，允许缓存验证和调度更新信息。znodes的每个时间点的数据改变，版本号会增加。例如，当客户端获得数据时也接收到数据的版本。

储存在每个znode命名空间中的数据的读写都是原子性的。读取时获得与znode相关联的所有数据，写入时替换所有数据。每个节点都有严格的约束来限制谁可以做什么。

ZooKeeper也拥有临时节点的概念。只要创建这些临时节点的会话一直活跃这些节点就会存在。会话结束节点会被删除。当你想要实现临时节点是有用(待定)。

## Conditional updates and watches（条件更新和监控）

Zookeeper支持监控的概念，客户端可以对任意的znode节点进行监控，监控将会在znode节点更改的时候触发并且移除。当一个监控触发时，客户端会收到一个包含znode更改信息的数据包。如果当客户端和ZooKeeper服务器的连接断开，客户端将会收到一个本地通知。这些都可以用来（待定）

## Guarantees（保证）

ZooKeeper非常快和非常简单。然而它一直以来的目标，是作为更多复杂服务结构的基础，例如同步，提供一系列的保证。它们是：

* 顺序一致性：来自客户端的更新会按照它们的发送顺序进行应用。
* 原子性：更新只有成功或者失败，没有中间状态
* 单一系统图像：客户端无论连接哪个服务器，它所得到ZooKeeper服务的图像都是一致的
* 可靠性：一旦更新被应用，那么它将会一直持续保存直到更新被覆盖。
* 时效性：系统的客户端视图在一个特定的时间里都保证是最新的。

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

深入讨论这些方法以及如何用它们实现更高层次的操作，请参考[tbd]

## Implementation（实现）

ZooKeeper Components 展示了ZooKeeper服务的高级组件。除了请求处理器，组成ZooKeeper服务的每个服务器复制它本身每个组件的副本。

![ZooKeeper Components](http://zookeeper.apache.org/doc/trunk/images/zkcomponents.jpg)

复制式数据库是一个包含整个数据树的内存数据库。更新信息将被记录在磁盘中保证可恢复性，在它们被写到内存数据库之前序列化写到磁盘中。

每个ZooKeeper服务器服务客户端。客户端连接到一个正确的服务器来提交请求。读取请求是由每个服务器的数据库的本地副本提供服务的。改变服务的请求和写请求都是由一致性协议来处理的。

所有来自客户端的写请求作为协议的一部分都将转发给一个单独的服务器，称之为leader。剩下的ZooKeeper服务器称之为followers，接收来自leader的信息提案和达成信息传输的一致性。消息传递层负责leaders的失效替换和同步leaders和followers。

ZooKeeper使用一个自定义的原子的消息传递协议。所以消息传递层是原子性的。ZooKeeper可以保证本地副本不会分割。当leader服务器收到一个写请求，它会计算这个写入操作执行时系统的状态和获取这个操作转化成一个事务的新状态。

## Uses（使用）

ZooKeeper的编程接口特意设计得简单。然而，你可以使用它来实现高层次的命令操作，例如同步原语，组的成员关系。所有权等。一些分布式应用可以使用它。

## Performance（性能）

ZooKeeper是设计成高性能的，但是真的这样么？ZooKeeper在雅虎的研发团队研究结果显明它真的如此。(看[ZooKeeper Throughput as the Read-Write Ratio Varies](http://zookeeper.apache.org/doc/r3.4.6/zookeeperOver.html#fg_zkPerfRW))应用在读取性能上表现地写性能高得多，因为写操作要涉及所有服务器的同步。（在调度服务中读性能超过写性能是普遍的情况）

![ZooKeeper Throughput as the Read-Write Ratio Varies](http://zookeeper.apache.org/doc/trunk/images/zkperfRW-3.2.jpg)

 ZooKeeper Throughput as the Read-Write Ratio Varies 图是ZooKeeper3.2发布版本运行在配置为两个2GHz的至强芯片和两个SATA 15K RPM驱动器上的吞吐量图表。一个驱动器用来ZooKeeper专用的日志设备。快照写到系统驱动。1K的读和1K的写。“服务器”数表明ZooKeeper集群的大小，服务器的数量构成服务。大概30个服务器用于模拟客户端。ZooKeeper集群配置leaders不允许客户端的连接。
 
 ## Note（说明）
 
 3.2版本比之前3.1版本提高了两倍性能。
 
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
 
 
