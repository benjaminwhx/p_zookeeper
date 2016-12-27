# Zookeeper Java实例

## 简单的Watch客户端

为了介绍Zookeeper JAVA API，我们开发了一个简单的watch客户端。这个客户端监测通过启动和停止程序Zookeeper节点的变化。

### 要求

客户端有四个要求：
* 它需要以下参数：
&emsp;&emsp;* Zookeeper服务的地址
&emsp;&emsp;* 然后是监控节点znode的名字
&emsp;&emsp;* 一个可执行文件的参数
&emsp;&emsp;* 输出的文件名
* 它获取与znode相关联的数据并启动可执行文件。
* 如果znode改变，客户端重新获取内容并重新启动可执行文件。

### 程序设计

按照惯例，Zookeeper应用分为两个单元，一个维持连接，另一个监控数据。在这个应用里，Executor类维持Zookeeper连接，DataMonitor类监控Zookeeper树形节点的数据。另外，Executor包含主线程和执行逻辑。它负责什么用户交互存在，以及和你通过参数传入的可执行程序交互和哪个实例停止和重启，根据znode的状态。

### Executor类

Executor对象是实例应用的主容器。它包含Zookeeper对象,DataMonitor，根据上面程序设计的描述。

```
// from the Executor class...
    
public static void main(String[] args) {
    if (args.length < 4) {
        System.err
                .println("USAGE: Executor hostPort znode filename program [args ...]");
        System.exit(2);
    }
    String hostPort = args[0];
    String znode = args[1];
    String filename = args[2];
    String exec[] = new String[args.length - 3];
    System.arraycopy(args, 3, exec, 0, exec.length);
    try {
        new Executor(hostPort, znode, filename, exec).run();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

public Executor(String hostPort, String znode, String filename,
        String exec[]) throws KeeperException, IOException {
    this.filename = filename;
    this.exec = exec;
    zk = new ZooKeeper(hostPort, 3000, this);
    dm = new DataMonitor(zk, znode, null, this);
}

public void run() {
    try {
        synchronized (this) {
            while (!dm.dead) {
                wait();
            }
        }
    } catch (InterruptedException e) {
    }
}
 ```
 
回想一下Executor的工作是启动和停止你通过命令行传入的可执行文件。它这样做通过Zookeeper对象应对激发的事件。就像上面你看到的代码，在Zookeeper构造里Executor将自身作为Watcher参数传入Zookeeper构造。他还将自身作为DataMonitorListener参数传入DataMonitor构造。每个Executor的定义，都实现下面的接口：

```
public class Executor implements Watcher, Runnable, DataMonitor.DataMonitorListener { ...
```

Watcher接口由Zookeeper Java API定义。Zookeeper使用它交流回它的主容器。它只支持一个方法，process()，并且Zookeeper使用它交流主线程感兴趣的通用事件，比如Zookeeper连接的状态或者Zookeeper session。这个例子里Executor简单的将事件指向到DataMonitor决定如何处理他们。简单的说明几点，按照惯例，Executor或者一些类Executor对象“拥有”Zookeeper连接，自由的委托事件到其他事件到其他对象。它还使用这个作为激发watch事件默认的管道（稍后详述）。

```
public void process(WatchedEvent event) {
    dm.process(event);
}
```

DataMonitorListener接口，另一方面，不是Zookeeper API的一部分。它是这个实例应用的自定义接口。DataMonitor对象使用它交流回它的容器，也是Executor对象。DataMonitorListener接口看起来是这样的：

```
public interface DataMonitorListener {
    /**
    * The existence status of the node has changed.
    */
    void exists(byte data[]);

    /**
    * The ZooKeeper session is no longer valid.
    * 
    * @param rc
    * the ZooKeeper reason code
    */
    void closing(int rc);
}
```

