# Zookeeper开发者编程指南

## 概述

这篇文档是使用Zookeeper协调服务开发分布式应用的开发者编程指南。它包含概念性的和实用性信息。

这个指南开始的4个部分对Zookeeper的概念提出了高层次的论述。这对理解Zookeeper怎样工作和怎样使用它都非常重要。它不包含源代码，但是它呈现了分布式计算的相关问题。第一组的章节是：

* Zookeeper数据模型
* ZooKeeper Sessions
* ZooKeeper Watches
* 一致性保证

接下来的4个章节提供了实际的编程信息，它们是：

* Zookeeper操作指南
* 绑定
* 程序结构，简单例子
* 常见问题和故障排除

文章的结尾有一个目录，它包含了一些其他有用的与Zookeeper有关的信息的链接。

这篇文档的大部分信息写成独立的容易理解的参考资料。然而，在开始你的第一个Zookeeper应用之前，你至少要读Zookeeper数据模型和Zookeeper基本操作上的部分。并且，简单的编程例子对理解Zookeeper客户端应用的基础结构非常有帮助。

##　Zookeeper数据模型

Zookeeper有一个分层的命名空间，非常像一个分布式的文件系统。不同的地方仅仅是命名空间里的每个节点可以有关联数据，也可以有子目录。它就像一个既可以是文件又可以是目录的文件系统。路径节点总是表示为标准的，绝对的，以斜线为分隔符的路径；没有相对引用。任何unicode编码字符都可以用在路径上，但是有以下限制：

*　null字符(\u0000)不能作为路径名字的一部分(这将导致C绑定的问题)。
* 下面的字符不能用，因为它们显示不好或者显示混乱：\u0001 - \u0019 and \u007F - \u009F.
* 下面的字符不允许： \ud800 -uF8FFF, \uFFF0-uFFFF, \uXFFFE - \uXFFFF (其中X是一个数字：1-E), \uF0000 - \uFFFFF.
* "."字符可以作为名字的一部分，但是"."和".."不能单独使用。因为Zokeeper不使用相对路径。下面的路径将是无效的："/a/b/./c" or "/a/b/../c".
* 保留字"zookeeper"

### Znodes

Zookeeper中的每个节点都被称为znode。Znode维护了一个stat结构，这个stat包含数据变化的版本号、访问控制列表变化。stat结构还有时间戳。版本号和时间戳一起，可让Zookeeper验证缓存和协调更新。每次znode的数据发生了变化，版本号就增加。例如，无论何时客户端检索数据，它也一起检索数据的版本号。并且当客户端执行更新或删除时，客户端必须提供他正在改变的znode的版本号。如果它提供的版本号和真实的数据版本号不一致，更新将会失败。(这种行为可以被覆盖)

> 在分布式应用工程中，node可以指的是一般的主机，一个服务器，全体成员的一员，一个客户端程序，等等。在Zookeeper的文档中，znode指的是数据节点。Servers指的是组成Zookeeper服务的机器；quorum peers 指的是组成全体的servers；client指的是任何使用Zookeeper服务的主机和程序。

Znode是程序员使用的主要实体。这里需要提到几个有价值的特征：

#### Watches

客户端可以在znode上设置watches。znode的变化将会触发watches后清除watches。触发watches时，Zookeeper向客户端发送一个通知。更多关于watches的信息可以在 Zookeeper watches 章节查看。

#### 数据访问

命名空间里的每个znodes上的数据存储都是原子性的读取和写入。读取时获取所有与znode有关的数据字节，写入时替换所有的数据字节。每个节点有一个访问控制列表用来限制谁可以做什么。

Zookeeper不是设计成通用的数据库或者大数据对象存储。而是管理协调数据。这个数据的形式是配置表单，状态信息，集合点等等。各种形式的协调数据属性都非常小：经过测量在KB之内。Zookeeper客户端和服务端实现检查确保znode有不到1M的数据，但是实际的数据要远小于平均值。在相对较大的数据上的操作将会引起一些操作花费更多的时间并且影响一些操作的延迟，因为需要额外的时间在网络和存储设备之间移动数据。如果需要大数据存储，通常的做法是将数据存储进大存储器系统，如NFS和HDFS，然后将存储指针和地址存储进Zookeeper。

#### 临时节点

Zookeeper还有一个临时节点的概念。这些znode一旦session创建就存在，session结束就被删除。因为这个特性，临时节点不允许有子节点。

#### 序列节点 -- 唯一的命名

当创建znode的时候你还可以请求在路径的最后追加一个单调递增的计数器。这个计数器在父节点是唯一的。计数器有一个%010d --的格式，它是10位数用0做填充(计数器用这个方法格式化简化排序)，也就是：0000000001。查看Queue Recipe使用这个特性的例子。注释：计数器的序列号由父节点通过一个int类型维护，计数器当超过2147483647的时候将会溢出(-2147483647将会导致)。

#### 容器节点

**3.6.0中增加**
Zookeeper中有容器节点的概念，容器节点用在leader，lock中特别有用。当容器中最后一个子节点被删除后，容器将要在某个时刻被删除。

鉴于这个属性，当你在容器节点中创建子节点时，你应该准备收到类似KeeperException、NoNodeException的一些异常。当检查到这些异常去尝试重试。

### Zookeeper里的计时

Zookeeper通过多种方式追踪计时：

#### Zxid

每个Zookeeper状态的变化都以zxid(事务ID)的形式接收到标记。这个暴露了Zookeeper所有变化的总排序。每个变化都会有一个zxid，并且如果zxid1早于zxid2则zxid1一定小于zxid2。

#### 版本号

节点的每个变化都会引起那个节点的版本号的其中之一增加。这三个版本号是version(znode的数据变化版本号),cversion(子目录的变化版本号)，和aversion(访问控制列表的变化版本号)。

#### Ticks

当使用多服务器的Zookeeper时，服务器使用ticks定义事件的时间，如状态上传，会话超时，同事之间的连接超时等等。tick次数只是通过最小的会话超时间接的暴露；如果一个客户端请求会话超时小于最小的会话超时，服务器就会告诉客户端会话超时实际上是最低会话超时时间。

#### Real time

Zookeeper不使用实时或时钟时间，除了将时间戳加在znode创建和更新的stat结构上。

### Zookeeper Stat 结构

Zookeeper中的每个znode的stat机构都由下面的字段组成：

* czxid - 引起这个znode创建的zxid
* mzxid - znode最后更新的zxid
* ctime - znode被创建的毫秒数(从1970年开始)
* mtime - znode最后修改的毫秒数(从1970年开始)
* version - znode数据变化号
* cversion - znode子节点变化号
* aversion - znode访问控制列表的变化号
* ephemeralOwner - 如果是临时节点这个是znode拥有者的session id。如果不是临时节点则是0。
* dataLength - znode的数据长度
* numChildren - znode子节点数量

## Zookeeper Sessions

Zookeeper客户端通过使用语言绑定在服务上创建一个handle建立一个和Zookeeper服务的会话。一旦创建了，handle从CONNECTION状态开始并且客户端库尝试连接到Zookeeper服务的服务器在这时候切换到CONNECTED状态。在正常运行期间会在这两个状态期间。如果发生不可恢复的错误，例如会话超时或授权失败，或应用明确的关闭处理器，handle将会移动到CLOSED状态。下面的图像展示了Zookeeper客户端可能的状态变化流程：

![](http://zookeeper.apache.org/doc/trunk/images/state_dia.jpg)

创建客户端会话，应用代码必须提供一个以逗号分隔开的host:port的列表，每个对应一个Zookeeper服务(如:"127.0.0.1:4545"or"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002")。Zookeeper客户端会选择任意一个服务并尝试连接它。如果这个连接失败，或者客户端变为disconected，客户端会自动的尝试连接列表里的下一个服务器，直到建立连接。

**3.2.0添加**可以在连接字符串后面加入一个"chroot"可选项。这将相对于这个root解析执行客户端命令(类似于unix的chroot命令)。如果使用实例："127.0.0.1:4545/app/a"或者"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"客户端将根在"/app/a"并且所有的路径将相对于这个根 - 例如getting/setting/等等。"/foo/bar"将会将操作变为"/app/a/foo/bar"。这个特性在每个用户的根目录都不同的多用户环境里特别有用。

客户端得到Zookeeper服务handle时，Zookeeper创建一个Zookeeper session，用64位的数字代表，分配到客户端。如果客户端连接到不同的Zookeeper服务器，他将发送session id作为连接握手的一部分。作为安全措施，服务器为session id创建一个任何Zookeeper服务器可以验证的密码。当建立会话是连同session id一起发送密码到客户端。客户端每当重建会话时都发送这个session id和密码。

Zookeeper客户端调用创建Zookeeper会话的参数之一是以毫米为单位的会话超时时间。客户端发送请求超时，服务器响应一个可以给客户端的超时时间。当前的实现要求超时时间的最小值是2个tickTime(服务端配置里设置)最大值是20个tickTime。Zookeeper客户端API允许访问协商的超时时间。

当客户端从ZK集群分隔它将开始搜索在session创建期间指定的服务器列表。最终，当客户端和服务器之间的连通性是重建时，session将会转变为"connected"状态或者转为"expired"状态(冲过重建超时)。为断开创建一个session对象是不明智的。ZK客户端类库会为你处理重连。我们启发式的构建客户端类库处理像“羊群效应”的事情等等。只有当通知session超时是建立新session(强制的)。

session逾期由Zookeeper集群自己管理，并不是客户端。当ZK客户端建立一个与集群的session时它提供一个上面描述的"timeout"值。这个值由集群确定什么时候客户端session逾期。当集群在指定的session超时周期内没有听到客户端(没有心跳)时发生。session逾期，集群会删除全部session的临时节点并立即通知其他链接的客户端(watch这些znode的客户端)。这时候逾期session的客户端仍然是disconnected的，它不会被通知session逾期知道他能够重新连接到集群。客户端将一直待在disconnected状态知道重新建立与集群的TCP连接，届时逾期session的watcher会收到"session expired"的通知。

过期session的状态转化实例通过watcher查看：

1、'connected'：建立session且正在和集群通信
2、...客户端从集群分离
3、'disconnected'：客户端丢失集群的连接
4、...时间消失，'timeout'周期之后session集群过期，客户端什么都看不到
5、...时间消失，客户端收复集群的连通性
6、'expired'：最终客户端重连到集群，然后是过期通知

建立Zookeeper session的另一个参数是默认的watcher。任何发生在客户端的状态变化都会通知Watchers。例如如果客户端丢失服务器的连通性客户端将会被通知，或者如果客户端session逾期，等等。这个watcher应该考虑初始化状态是disconnected(也就是说要在任何状态事件变化之前发送到watcher)。对于一个新连接，通常发送到watcher的第一个时间是session连接事件。

sesion通过客户端发送请求保持存活。如果session空闲了一个超时时间，客户端会发送一个PING请求保持session存活。这个PING请求不仅可让Zookeeer服务器知道客户是活动的，而且可让客户端核查它的连接是活动的。PING的时间是能够确保合理检测死链的时间并重连到新服务。

一旦成功的建立的到服务器的连接基本上有两种情况客户端生产丢失连接，当执行同步或异步操作时并且下面的其中之一：

1、应用在session上调用一个它不再存活的操作
2、Zookeeper在执行期间断开和服务器的连接。

**3.2.0添加 -- SessionMovedException**。有一个客户端通常不可见的exception叫做SessionMovedException。这个异常发生在session重新创建在另一个服务器上时。一般引起这个错误的原因是客户端向服务端发送请求，但是网络延迟，以至于客户端超时并连接到新服务器。当延迟的包到达第一个服务器时，老服务器检查到session已经被移除，并关闭客户端连接。一般客户端看不到这个错误因为他们不从这些老连接读取数据。当两个客户端尝试使用保存的sessionid和password重新建立相同连接时可以看出来。其中一个客户端将会建立连接且第二个客户端会断开连接。

**更新服务器列表**，我们允许一个客户端更新连接字符串通过提供一个新的逗号分隔的主机：端口号列表，每个都是一个服务器。函数调用一个概率负载均衡算法会引起客户端断开与当前主机的连接，来使在新列表中的每个服务器达到与预期一致的数量。万一客户端连接的当前主机不在新的列表中，这个调用会引起连接被删除。另外，这个决定基于是否服务器的数量增加或减少了多少。
            
比如说，如果之前的连接包含三个主机，现在的连接多了两个主机，连接到每个主机的客户端的40%为了负载均衡将会移动到新的主机上去。这个算法会引起客户端断掉它当前与服务器的连接，这个概览是0.4，并且客户端随机选择两个新主机中的一个连接。
            
另一个例子，假设我们有5个主机，然后现在更新列表移除两个主机，连接到剩余三台主机的客户端依然保持连接，然而所有连接到已被移除主机的客户端都需要移到剩下三台主机的一台上，并且这种选择是随机的。如果连接断开，客户端进入一个特殊的模式并使用概率算法选择一个新的服务器，而不仅仅只是循环。
            
在第一个例子中，每个客户端决定断开连接的概览为0.4，但是一旦做了决定，它将会随机的连接到一个新的服务器，仅仅当它不能连接到任何一台新的服务器上时，它将尝试连接旧的服务器。当找到一个服务器或者新列表中所有的服务器都连接失败的时候，客户端回到操作正常模式，选择一个任意的服务器并尝试连接它，如果连接失败，它会继续尝试不同的随机的服务器，并一直循环下去。

## ZooKeeper Watches

Zookeeper里的所有读取操作 - **getData()**,**getChildren()**和**exists()** - 都有设置watch的选项。这是Zookeeper watch的定义：watch事件是one-time触发，向客户端发送设置watch，当设置watch的数据变化时发生。在watch定义里有三个关键点：

1、**一次触发** - 当数据有了变化时将向客户端发送一个watch事件。例如，如果一个客户端用getData("/znode1",true)并且过一会之后/znode1的数据改变或删除了，客户端将获得一个/znode1的watch事件。如果/znode1再次改变，将不会发送watch事件除非设置了新watch。
2、**发送给客户端** - 这意味着事件发往客户端，但是可能在成功之前没到客户端。Watches是异步发往watchers。Zookeeper提供一个顺序保证：在看到watch事件之前绝不会看到变化。网络延迟或其他因素可能引起客户端看到watches并在不同时间返回code。关键点是不同客户端看到的是一致性的顺序。
3、**被设置了watch的数据** - 这是指节点发生变动的不同方式。你可以认为ZooKeeper维护了两个watch列表：data watch和child watch。getData()和exists()设置data watch，而getChildren()设置child watch。或者，可以认为watch是根据返回值设置的。getData()和exists()返回节点本身的信息，而getChildren()返回子节点的列表。因此，setData()会触发znode上设置的data watch（如果set成功的话）。一个成功的?create() 操作会触发被创建的znode上的数据watch，以及其父节点上的child watch。而一个成功的?delete()操作将会同时触发一个znode的data watch和child watch（因为这样就没有子节点了），同时也会触发其父节点的child watch。Watch由client连接上的ZooKeeper服务器在本地维护。这样可以减小设置、维护和分发watch的开销。当一个客户端连接到一个新的服务器上时，watch将会被以任意会话事件触发。当与一个服务器失去连接的时候，是无法接收到watch的。而当client重新连接时，如果需要的话，所有先前注册过的watch，都会被重新注册。通常这是完全透明的。只有在一个特殊情况下，watch可能会丢失：对于一个未创建的znode的exist watch，如果在客户端断开连接期间被创建了，并且随后在客户端连接上之前又删除了，这种情况下，这个watch事件可能会被丢失。?

在ZooKeeper服务器中，当客户端连接的时候，监听器被保存在本地。这使得监听器轻量级的被设置、保存、分发。当一个客户端连接一个新的服务器，监听器会触发一些会话事件。当从服务器断开连接的时候，不会受到监听器。当一个客户端重新连接，如果需要的话，之前注册的监听器会被注册和触发。有一个监听器可能丢失的情况：如果在断开连接期间，一个节点被创建和删除，一个已存在的节点的监听器还没有创建，将丢失。

### 监听器的意思

我们能在三种调用读取ZooKeeper状态的情况下设置监听器：exists，getData和getChildren，下面的列表是一个监听器触发的事件的详细情况：

* 创建事件：exists的调用
* 删除事件：exists，getData和getChildren的调用
* 改变事件：exists，getData的调用
* 子节点事件：getChildren的调用

### 移除监听器

我们可以调用removeWatches来移除一个注册在节点上的监听器。同样的，一个ZooKeeper客户端在没有服务器连接的情况下能移除本地的监听器，通过设置本地的标记为true。下面是事件的详细列表监听器成功的被移除后触发：

* 子节点移除事件：调用getChildren增加的监听器。
* 数据移除事件：调用exists或getData增加的监听器。

### ZooKeeper对监听器的保证

对于监听器，ZooKeeper有下列的保障：

* 监听器和另外的事件，另外的监听器和异步的回复是有序的。ZooKeeper 客户端库确保每件事都有序分发。
* 一个客户端看到这个节点的新的数据之前，会先看到他监听的节点的一个监听事件。
* 从ZooKeeper 来的监听事件的顺序对应于ZooKeeper 服务看到的更新的顺序。

### 关于watchers要记住的事情

* Watches是一次触发；如果你得到一个watch事件且想在将来的变化得到通知，必须设置另一个watch。
* 因为watches是一次触发且在获得事件和发送请求得到wathes之间有延迟你不能可靠的看到发生在Zookeeper节点的每一个变化。准备好处理这个案例在获得事件和再次设置watch之间变化多次。(你可能不在意，但是至少认识到它可能发生)
* 一个watch对象，或function/context对，对于指定的通知只能触发一次。例如，如果相同的文件通过exists和getData注册了相同的watch对象并且文件稍后删除了，watch将只会触发文件的删除通知。
* 从服务端断开连接时(比如服务器故障)，将不会得到任何watches直到重新建立连接。因为这个原因session事件被发送到所有watch处理器。使用session事件进入安全模式：断开连接时不接收事件，所以在这个模式里你的程序应该采取保守。

## Zookeeper使用ACLs控制访问

Zookeeper使用ACLs控制访问它的znodes(Zookeeper的数据节点)。ACL实现非常类似于UNIX文件访问权限：它使用权限位允许/不允许一个简单和范围的各种操作。不像标准的UNIX权限，Zookeeper节点不由三个标准范围(用户，组 和 world)限制。Zookeeper没有znode所有者的概念。而是一个ACLs指定一组ids和与这些ids相关联的权限。

还要注意ACL只适用于特定的znode。尤其不适用于children。例如，如果/app对ip:192.168.1.56是只读的并且/app/status是全都可读的，任何人可以读取/app/status；ACLs不是递归控制的。

Zookeeper支持可插拔的权限认证方案。使用scheme:id的形式指定Ids,scheme是id对应的权限认证scheme。例如，ip:172.16.16.1是一个地址为172.16.16.1的id.

客户端连接的Zookeeper并授权自己时，Zookeeper联合所有对应客户端的ids。这些ids在尝试访问节点时核查znodes的ACLs。ACLs由对组成(scheme:expression,permissions)。expression的格式是针对scheme。例如，(ip:19.22.0.0/16, READ)对开头19.22的IP地址的任意客户端提供读取权限。

### ACL权限

Zookeeper支持下面的permissions：

* CREATE：可以创建子节点
* READ：可以从节点获取数据并列出它的子节点
* WRITE：可以向节点设置数据
* DELETE：可以删除一个子节点
* ADMIN：可以设置权限

CREATE和DELELTE权限已经更细粒度的划分了WRITE权限。CREATE和DELETE的案例是：

你想要A在Zookeeper节点上能够set，但是不能CREATE或DELETE子节点。

CREATE而没有DELETE：客户端通过在父节点创建的Zookeeper节点创建请求。你想要所有客户端能add，而只有request processor可以delete。（这有点像文件的APPEND权限）

因为Zookeeper没有文件所有者的权限才有ADMIN权限。在某种意义上ADMIN权限指定实体作为拥有者。Zookeeper不支持LOOKUP权限。每人都有LOOKUP权限。这可让你stat一个节点，但不能做其他的。

### 内嵌的ACL schemes

Zookeeper有下面的schemes：

* world：有单独的id，anyone,代表任何人
* auth:不适用任何id，代表任何授权的用户。
* digest：使用username;password字符串生成MD5哈希作为ACL ID身份。通过发送username:password明文授权。在ACL里使用时expression将会是username:base64编码的SHA1 password摘要。v
* ip:使用客户端IP作为ACL ID身份。

## 可插拔的Zookeeper身份认证

Zookeeper运行在各种不同身份认证shcemes多变的不同环境，所以它有一个完整的可插拔的身份认证框架。甚至内嵌的权限认证schemes使用可插拔的身份认证框架。

理解身份认证框架怎么工作，首先你必须理解两个主要的身份认证操作。框架首先必须授权客户端。通常客户端一连接到服务端就做了并且由验证信息组成。第二个操作通过框架处理找到对应客户端的ACL入口。ACL入口是 对。idspec可以是简单的字符串或者表达式。这是权限插件匹配的实现。这是一个授权插件必须实现的接口：

```
public interface AuthenticationProvider {
String getScheme();
KeeperException.Code handleAuthentication(ServerCnxn cnxn, byte authData[]);
boolean isValid(String id);
boolean matches(String id, String aclExpr);
boolean isAuthenticated();
}
```

第一个方法getScheme返回标示插件的字符串。因为我们支持多个权限认证方法，一个身份认证证书或idspec一直有scheme:前缀。Zookeeper服务器使用认证插件返回的scheme确定sheme适用的ids。

### 一致性保证

Zookeeper是一个高性能可扩展的服务。读取和写入操作都非常快，但读取比写入块。这个读取案例的原因是Zookeeper可以提供老数据，Zookeeper的一致性保证：

**顺序一致性**
客户端的更新将发送到序列

**原子性**
更新成功或失败 -- 没有局部结果

**单一系统影像**
客户端看到的服务端的视图都一样

**可靠性**
一旦更新应用了，它将会一直保持到下次更新覆盖。这个保证有两个推论：
* 如果客户端成功，更新就被应用。客户端看不到失败现象。
* 客户端可以看到任何更新，通过读取请求或成功的更新

**时效性**
系统的客户端视图在特定的时间保证是最新的。

使用这些一致性保证很容易构建更高级的功能如领导选举，阻塞、队列和单独可撤销的锁定在客户端。

## 绑定

Zookeeper客户端类库有两种语言：Java和C。下面的章节介绍他们。

### Java绑定

有两个组成Zookeeper Java绑定的包：org.apache.zookeeper和org.apache.zookeeper.data。其余组成zookeeper的包在内部使用或是服务实现的一部分。org.apache.zookeeper.data包是使用简单容器生成的类。

Zookeeper java客户端使用的主类是zookeeper类。它两个构造函数的不同仅仅是通过可选的session id和password。Zookeeper支持跨进程的session恢复。Java程序可以保持它的session id和密码到稳定的存储、重启和恢复更早程序实例使用的session。

创建Zookeeper的时候，同时创建了两个线程：一个IO线程和事件线程。所有的IO都在在IO线程上(使用Java NIO)。所有的事件回调都在事件线程上。Session在IO线程上维护如重连Zookeeper服务和维护心跳。同步方法调用也在IO线程处理。所有异步调用和watche事件在事件线程上处理。这个设计有几个点需要注意：

* 所有异步调用的完成和watcher回调将被放置在序列中，一次一个。调用者可以做任何他们想做的处理，但是在处理期间不能做其他回调。
* 回调不锁定IO线程的处理或同步调用的处理。
* 同步调用可能是不正确的顺序。例如，假定一个客户端做以下的处理：对节点/a设置watch为true发型一个异步读取，然后在读取的完成回调里执行一个a节点的同步读取。

最后，关闭规则非常简单：一旦一个zookeeper对象关闭或者接受一个致命的时间(SESSION_EXPIRED and AUTH_FAILED)，Zookeeper对象变为无效的。在close上，两个线程停止并且zookeeper handle上的任何进一步的访问都是未定义的行为并拒绝。

## 常见问题和解决方案

现在你知道Zookeeper非常快,简单，等等。。。下面是zookeeper容易陷入的陷阱：

1、如果你正在使用watches，你必须寻找连接的watch事件。Zookeeper客户端从服务端断开连接时，直到重连后才能接收变化通知。如果你正在观察的znode存在，断开连接时如果znode创建或删除将丢失事件。
2、必须测试Zookkeper服务故障。Zookeeper服务只要大多数的服务器正常就可以运行。问题是：你的应用可以处理它吗？现实中连接是可以断的。Zookeeper客户端库负责恢复连接并让你知道发生了什么，但是你必须确定恢复你的状态和失败的未完成请求。在测试的时候找到他，而不是在生产力 - 测试Zookeeper服务器并重启他们。
3、客户端使用的Zookeeper服务列表必须匹配每个Zookeeper服务器的Zookeeper服务列表。
4、当心存储事务日志的地方。Zookeeper的性能关键是事务日志。Zookeeper在返回响应之前必须把事务同步到中介。专用的事务日志装置是保证良好性能的关键。将日志存储进繁忙的装置会影响性能。如果你只有一个存储装置，将追踪文件放置到NFS并增加快照数量；这不能消除问题但能缓解它。
5、正确的设置你的Java最多堆内存。这非常重要避免swapping。不必要的进入磁盘肯定会降低性能。记住，在Zookeeper，每件事都是有序的，所以如果一个请求点击磁盘，所有其他队列的请求也点击磁盘。避免swapping，尝试设置堆大小为物理内存减去操作系统和缓存需要的内存。确定配置最佳堆内存的配置是运行负载测试。如果不能做，就估算一个值。例如，在4G的机器上，保守估计堆内存是3G。
  

