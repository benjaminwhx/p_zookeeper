# Zookeeper JMX

## JMX

Apache Zookeeper已经扩展了对JMX的支持，允许你展示和管理Zookeeper整体服务。

本篇文章假定你有JMX的基础知识。查看Sun JMX Technology页面快速学习JMX。

查看JMX管理指南纤细了解建立VM实例的本地和远程管理。默认包含的zkServer.sh只支持本地管理 - 查看链接文档启用支持远程管理(超过了本文档的范围)。

## 启动启用JMX的Zookeeper

org.apache.zookeeper.server.quorum.QuorumPeerMain类将启动了一个JMX可管理的Zookeeper服务。这个类在初始化期间注册适当的MBeans支持实例的JMX监控和管理。查看bin/zkServer.sh使用QuorumPeerMain启动Zookeeper的实例。

## 运行JMX控制台

有很多可用的JMX控制台可以连接到运行的服务。对于这个实例我们使用Sun的jconsole。

Java JDK有一个简单的JMX控制台叫做jconsole，它可以用于连接Zookeeper并检查运行的服务。一旦你使用QuorumPeerMain启动jconsole启动了Zookeeper，它通常在JDK_HOME/bin/jconsole。

当"新连接"窗口展示连接到本地进程或使用远程连接。

默认，VM的"overview"标签被展示。选择"MBeans"标签。

你现在应该在左边看org.apache.ZookeeperService。扩展这一项取决于你怎么启动服务，你将能够监控和管理相关的各种服务功能。

还要注意Zookeeper还注册log4j Mbeans。在左手边相同的部分你会看到"log4j"。展开它通过JMX管理log4j。特别感兴趣的是能够动态改变日志级别通过自定义appender和root阀值。禁用Log4j MBean注册可以在启动Zookeeper时传送 -Dzookeeper.jmx.log4j.disable=true到JVM。

## Zookeeper MBean 参考

这个表格详细列出了复制的Zookeeper整体里的服务器的JMX。这是生产环境典型案例。

| MBean         | MBean对象名字  | 描述    |
|:------------- |:-------------|:---------------------|
|Quorum|ReplicateServer_id<#>| 代表Quorum或全体 - 所有集群成员的父级。注意对象名字包含JMX连接的服务的"myid" |
|LocalPeer RemotePeer| replica.<#> | 代表一个本地或远程对的。注意对象名字包含服务的"myid" |
|Leader|Leader|表明父级复制品是领导者并且提供那个服务的属性/操作。注意Leader是ZookeeperServer的子类，所以它提供与ZooeeperServer节点相关的所有信息|
|Follower|Follower|表明父级复制品是一个跟随者并且提供那个服务的属性/操作。注意Follower是ZookeeperServer的子类，所以它提供与ZookeeperServer节点相关的所有信息。|
|DataTree|InMemoryDataTree|内存中znode数据库的统计信息，还有访问数据的统计信息的操作。InMemoryDataTrees是ZookeeperServer节点的子类|
|ServerCnxn|<session_id>|每个客户端连接的统计信息，和那些连接的操作。注意对象名字是连接的十六进制的session id。|

​这个表格详细列举了单台服务的JMX。通常单台只用于开发环境。

| MBean         | MBean对象名字  | 描述    |
|:------------- |:-------------|:---------------------|
|ZookeeperServer|StandaloneServer_port<#>|运行的服务的统计信息，还有重置这些属性的操作。注意对象名包含服务的客户端端口。|
|DataTree|InMemoryDataTree|znode内存数据库的统计信息，还有访问数据统计信息的操作。|
|ServerCnxn|<session_id>|每个客户端连接的统计信息，还有那些连接的操作。注意对象名字是连接的十六进制形式的session id|