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

这个接口在DataMonitor类里定义并在Executor类里实现。当Executor.exists()被调用时，Executor决定是否启动或者关闭每个需求。回想要求说明当znode不再存在时杀掉可执行文件。

当Executor.closing()被调用时，Executor决定是否关闭它自己应对Zookeeper连接永久的消失。

你可能已经猜到，DataMonitor是调用这些方法的对象,应对Zookeeper状态的变化。

这里是DataMonitorListener.exists()和DataMonitorListener.closing的Executor的实现：

```
public void exists( byte[] data ) {
    if (data == null) {
        if (child != null) {
            System.out.println("Killing process");
            child.destroy();
            try {
                child.waitFor();
            } catch (InterruptedException e) {
            }
        }
        child = null;
    } else {
        if (child != null) {
            System.out.println("Stopping child");
            child.destroy();
            try {
               child.waitFor();
            } catch (InterruptedException e) {
            e.printStackTrace();
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Starting child");
            child = Runtime.getRuntime().exec(exec);
            new StreamWriter(child.getInputStream(), System.out);
            new StreamWriter(child.getErrorStream(), System.err);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public void closing(int rc) {
    synchronized (this) {
        notifyAll();
    }
}
```

### DataMonitor类

DataMonitor类有Zookeeper逻辑的具体内容。它主要是异步和事件驱动。DataMonitor在构造里就开始做事了：

```
public DataMonitor(ZooKeeper zk, String znode, Watcher chainedWatcher,
        DataMonitorListener listener) {
    this.zk = zk;
    this.znode = znode;
    this.chainedWatcher = chainedWatcher;
    this.listener = listener;
    
    // Get things started by checking if the node exists. We are going
    // to be completely event driven
    zk.exists(znode, true, this, null);
}
```

调用Zookeeper.exists检查znode是否存在，设置一个watch,并将自己的引用传入作为完成回调对象。从这个意义上，它开始做事了，因为当watch被触发时发生真正的处理。

> 注意
  
> 不要迷惑watch回调的完成回调。Zookeeper.exists()完成回调，它恰好是DataMonitor对象里的StatCallback.processResult()的实现，当在服务上异步设置watch操作完成时调用。
  
> watch的触发，换句话说，发送事件到Executor对象，因为Executor注册作为Zookeeper对象的Watcher。
  
> 顺便插一句，你要注意DataMonitor还可以注册它自身作为这个特定watch事件的Watcher。这是Zookeeper3.0.3的新特性。在这个例子里，而DataMonitor没有注册作为Watcher。

Zookeeper.exists()操作在服务上完成时，Zookeeper API在客户端调用这个完成回调：

```
public void processResult(int rc, String path, Object ctx, Stat stat) {
    boolean exists;
    switch (rc) {
    case Code.Ok:
        exists = true;
        break;
    case Code.NoNode:
        exists = false;
        break;
    case Code.SessionExpired:
    case Code.NoAuth:
        dead = true;
        listener.closing(rc);
        return;
    default:
        // Retry errors
        zk.exists(znode, true, this, null);
        return;
    }
 
    byte b[] = null;
    if (exists) {
        try {
            b = zk.getData(znode, false, null);
        } catch (KeeperException e) {
            // We don't need to worry about recovering now. The watch
            // callbacks will kick off any exception handling
            e.printStackTrace();
        } catch (InterruptedException e) {
            return;
        }
    }     
    if ((b == null && b != prevData)
            || (b != null && !Arrays.equals(prevData, b))) {
        listener.exists(b);
        prevData = b;
    }
}
```

代码首先检查znode存在性的错误代码,致命错误，和可恢复的错误。如果文件存在，它从znode获取数据，然后如果状态发生改变就调用Executor的exists()回调。注意，没有必要做每个异常处理因为它已经检查等待任何引起错误的事情：如果在调用Zookeeper.getData()之前删除了节点，通过Zookeeper.exists()设置的watch事件触发一个回调；如果有通讯错误，当连接恢复的时候激发连接watch事件。

最后，注意DataMonitor怎么处理watch事件：

```
public void process(WatchedEvent event) {
    String path = event.getPath();
    if (event.getType() == Event.EventType.None) {
        // We are are being told that the state of the
        // connection has changed
        switch (event.getState()) {
        case SyncConnected:
            // In this particular example we don't need to do anything
            // here - watches are automatically re-registered with 
            // server and any watches triggered while the client was 
            // disconnected will be delivered (in order of course)
            break;
        case Expired:
            // It's all over
            dead = true;
            listener.closing(KeeperException.Code.SessionExpired);
            break;
        }
    } else {
        if (path != null && path.equals(znode)) {
            // Something has changed on the node, let's find out
            zk.exists(znode, true, this, null);
        }
    }
    if (chainedWatcher != null) {
        chainedWatcher.process(event);
    }
}
```

如果客户端Zookeeper类库可以在session超时之前重建通讯通道，所有的session的watches将自动的重连服务。在编程指南里查看Zookeeper Watches了解更多。在这个函数里有一点低，当DataMonitor获取znode的事件时，它调用Zookeeper.exists()去找出发生了什么变化。

## 完整代码

### Executor.java

```
/**
 * A simple example program to use DataMonitor to start and
 * stop executables based on a znode. The program watches the
 * specified znode and saves the data that corresponds to the
 * znode in the filesystem. It also starts the specified program
 * with the specified arguments when the znode exists and kills
 * the program if the znode goes away.
 */
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class Executor
    implements Watcher, Runnable, DataMonitor.DataMonitorListener
{
    String znode;

    DataMonitor dm;

    ZooKeeper zk;

    String filename;

    String exec[];

    Process child;

    public Executor(String hostPort, String znode, String filename,
            String exec[]) throws KeeperException, IOException {
        this.filename = filename;
        this.exec = exec;
        zk = new ZooKeeper(hostPort, 3000, this);
        dm = new DataMonitor(zk, znode, null, this);
    }

    /**
     * @param args
     */
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

    /***************************************************************************
     * We do process any events ourselves, we just need to forward them on.
     *
     * @see org.apache.zookeeper.Watcher#process(org.apache.zookeeper.proto.WatcherEvent)
     */
    public void process(WatchedEvent event) {
        dm.process(event);
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

    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }

    static class StreamWriter extends Thread {
        OutputStream os;

        InputStream is;

        StreamWriter(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            start();
        }

        public void run() {
            byte b[] = new byte[80];
            int rc;
            try {
                while ((rc = is.read(b)) > 0) {
                    os.write(b, 0, rc);
                }
            } catch (IOException e) {
            }

        }
    }

    public void exists(byte[] data) {
        if (data == null) {
            if (child != null) {
                System.out.println("Killing process");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                }
            }
            child = null;
        } else {
            if (child != null) {
                System.out.println("Stopping child");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("Starting child");
                child = Runtime.getRuntime().exec(exec);
                new StreamWriter(child.getInputStream(), System.out);
                new StreamWriter(child.getErrorStream(), System.err);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

### DataMonitor.java

```
/**
 * A simple class that monitors the data and existence of a ZooKeeper
 * node. It uses asynchronous ZooKeeper APIs.
 */
import java.util.Arrays;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

public class DataMonitor implements Watcher, StatCallback {

    ZooKeeper zk;

    String znode;

    Watcher chainedWatcher;

    boolean dead;

    DataMonitorListener listener;

    byte prevData[];

    public DataMonitor(ZooKeeper zk, String znode, Watcher chainedWatcher,
            DataMonitorListener listener) {
        this.zk = zk;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
        // Get things started by checking if the node exists. We are going
        // to be completely event driven
        zk.exists(znode, true, this, null);
    }

    /**
     * Other classes use the DataMonitor by implementing this method
     */
    public interface DataMonitorListener {
        /**
         * The existence status of the node has changed.
         */
        void exists(byte data[]);

        /**
         * The ZooKeeper session is no longer valid.
         *
         * @param rc
         *                the ZooKeeper reason code
         */
        void closing(int rc);
    }

    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
            case SyncConnected:
                // In this particular example we don't need to do anything
                // here - watches are automatically re-registered with 
                // server and any watches triggered while the client was 
                // disconnected will be delivered (in order of course)
                break;
            case Expired:
                // It's all over
                dead = true;
                listener.closing(KeeperException.Code.SessionExpired);
                break;
            }
        } else {
            if (path != null && path.equals(znode)) {
                // Something has changed on the node, let's find out
                zk.exists(znode, true, this, null);
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }

    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (rc) {
        case Code.Ok:
            exists = true;
            break;
        case Code.NoNode:
            exists = false;
            break;
        case Code.SessionExpired:
        case Code.NoAuth:
            dead = true;
            listener.closing(rc);
            return;
        default:
            // Retry errors
            zk.exists(znode, true, this, null);
            return;
        }

        byte b[] = null;
        if (exists) {
            try {
                b = zk.getData(znode, false, null);
            } catch (KeeperException e) {
                // We don't need to worry about recovering now. The watch
                // callbacks will kick off any exception handling
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }
        if ((b == null && b != prevData)
                || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;
        }
    }
}  
```