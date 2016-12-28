# Zookeeper部署和管理员配额

## 配额

Zookeeper包含namespace和bytes quotas。你可以使用ZookeeperMain类设置quotas。Zookeeper打印WARN消息如果用户超过分配给他们的quota。消息打印在Zookeeper的log里。

```
$java -cp zookeeper.jar:src/java/lib/log4j-1.2.15.jar/conf:src/java/lib/jline-0.9.94.jar \ org.apache.zookeeper.ZooKeeperMain -server host:port
```

上面的命令提供给你了一个使用quotas的命令行选项。

## 设置配额

你可以使用setquota设置Zookeeper节点上的quota。它有一个设置配额的选项 -n(namespace)和-b(bytes)。

Zookeeper quota存储在Zookeeper自身的/zookeeper/quota。只有管理员才可以设置更改/zookeeper/quota，禁用其他用户。

## 配额清单

你可以使用listquota列出Zookeeper节点上的quota。

## 删除配额

你可以使用delquota删除Zookeeper节点上的quota。