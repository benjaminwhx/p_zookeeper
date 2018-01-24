# ACL
ACL: Zookeeper提供的一套完善的权限控制机制（Access Control List）来保障数据的安全。

UGO（User、Group和Others）是Unix/Linux文件系统中的一套权限控制机制。UGO其实是一种粗粒度的文件系统权限控制模式，利用UGO智能对三类用户进行权限控制，即文件的创建者，创建者所在的组以及其他所有用户，很显然，UGO无法解决下面这个场景：用户U1创建了文件F1，希望U1所在的用户组G1拥有对F1读写和执行的权限，另一个用户组G2拥有读权限，而另外一个用户U3则没有任何权限。

## 1、ACL介绍
我们可以从三方面来理解ACL机制，分别是：权限模式（Scheme）、授权对象（ID）和权限（Permission），通常使用"scheme:id:permission"来标识一个有效的ACL信息。

### 1.1、权限模式Scheme
权限模式用来确定权限验证过程中使用的检验策略。在ZooKeeper中，开发人员使用最多的就是以下四种权限模式。
* IP：IP模式通过IP地址粒度来进行权限控制，例如"ip:192.168.0.110"。同时IP模式也支持按照网段的方式进行配置，例如"ip:192.168.0.1/24"表示针对192.168.0.*这个IP段进行权限控制。
* Digest：Digest是最常用的权限控制模式，类似于"username:password"，我们设置前需要对明文进行一次加密，使用`DigestAuthenticationProvider.generateDigest(String idPassword)`来加密，例如`foo:zk-book`会加密成`foo:kWN6aNSbjcKWPqjiV7cg0N24raU=`
* World：World是一种最开放的权限控制模式，这种控制模式几乎没有任何作用。它只有一个权限标识，即`world:anyone`
* Super：Super模式，意思是超级用户的意思，是一种特殊的Digest模式。开启它需要在ZooKeeper服务器启动的时候，添加下面的系统属性：`-Dzookeeper.DigestAuthenticationProvider.superDigest=foo:kWN6aNSbjcKWPqjiV7cg0N24raU=`（配置在zkServer.sh里面的start-foreground或者start里面，看你是使用哪个启动）

### 1.2、授权对象ID
授权对象指的是权限赋予的用户或一个指定实体，例如IP地址或是机器等。在不同的权限模式下，授权对象是不同的。

### 1.3、权限Permission
权限分为5大类：
* CREATE（C）：创建权限
* DELETE（D）：删除权限
* READ（R）：读取权限
* WRITE（W）：更新权限
* ADMIN（A）：管理权限

## 2、ACL管理
设置ACL可以通过两种方式：一种方式是通过`create [-s] [-e] path data acl`命令，在创建节点的时候就指定acl，另一种方式是已经存在节点，通过`setAcl path acl`来设置acl。

```
[zk: localhost:2181(CONNECTED) 4] create /aa init digest:foo:kWN6aNSbjcKWPqjiV7cg0N24raU=:cdrwa
[zk: localhost:2181(CONNECTED) 4] create -e /bb  init
[zk: localhost:2181(CONNECTED) 4] setAcl /bb digest:foo:kWN6aNSbjcKWPqjiV7cg0N24raU=:cdrwa
[zk: localhost:2181(CONNECTED) 4] getAcl /bb
```
