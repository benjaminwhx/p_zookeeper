# Zookeeper阻塞和队列基础教程

## 简介

在这个教程里，我们展示了使用Zookeeper阻塞和生产者-消费者队列的简单实现。我们所说的类分别叫Barrier和Queue。这些实例假定你至少有一台Zookeeer服务正在运行。

每个部分都使用下面的通用代码片段：

```
static ZooKeeper zk = null;
static Integer mutex;

String root;

SyncPrimitive(String address) {
    if(zk == null){
        try {
            System.out.println("Starting ZK:");
            zk = new ZooKeeper(address, 3000, this);
            mutex = new Integer(-1);
            System.out.println("Finished starting ZK: " + zk);
        } catch (IOException e) {
            System.out.println(e.toString());
            zk = null;
        }
    }
}

synchronized public void process(WatchedEvent event) {
    synchronized (mutex) {
        mutex.notify();
    }
}
```

所有类都继承SyncPrimitive。通过这个方式，我们执行步骤都在SyncPrimitive的构造里。为了保持实例简单，我们创建一个Zookeeper对象第一次我们实例化一个Barrier对象或一个Queue对象，并且我们静态一个静态变量引用到这个对象。阻塞和队列的序列实例检查Zookeeper对象是否存在。或者，我们可以用程序创建一个Zookeeper对象并传到Barrier和Queue的构造里。

我们使用process()方法处理watches触发的通知。在下面的讨论中，我们展示设置watches的代码。watch是内部结构使Zookeeper能够通知节点变化到客户端。例如，如果一个客户端正在等待其他客户端离开一个barrier，然后它可以设置一个watch并等待修改特定的节点，它可以表明这是等待的结束。只要仔细看我们的例子这点就会很清晰。

## 阻塞

barrier是一个元件，它能够启用同步计算开始和结束的一组流程。这个实现的一般方法是有一个barrier节点为成为各个处理节点的父节点的目的服务。假如我们叫barrier节点"/b1"。然后每个进程"p"创建一个节点"/b1/p"。一旦足够的进程创建了他们对应的节点，加入的进程就可以开始计算。

在这个例子里，每个进程实例化一个Barrier对象，并且它的构造带有几个参数：

Zookeeper服务的地址(如"zoo1.foo.com:2181")
Zookeeper上barrier节点的路径(如"/b1")
进程组的大小
Barrier的构造将Zookeeper服务地址传入的父类的构造函数。如果zk实例不存在父类就创建一个Zookeeper实例。然后Barrier构造函数在Zookeeper上创建一个barrier节点,它是所有进程节点的父节点，我们把它叫做root(注意这里不是Zookeeper root"/")。

```
/**
 * Barrier constructor
 *
 * @param address
 * @param root
 * @param size
 */
Barrier(String address, String root, int size) {
    super(address);
    this.root = root;
    this.size = size;

    // Create barrier node
    if (zk != null) {
        try {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            System.out
                    .println("Keeper exception when instantiating queue: "
                            + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception");
        }
    }

    // My node name
    try {
        name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString());
    } catch (UnknownHostException e) {
        System.out.println(e.toString());
    }

}
```

进入barrier，一个进程调用enter()。进程在root下创建一个表示它自己的节点，节点名字使用它的主机名的形式。然后它等待直到足够的进程进入到barrier。进程通过使用"getChildren()"检查root节点的子节点数量，并且在这个例子里不到足够的数量时就一直等待通知。当root节点有变化是接收通知，进程必须设置一个watch，并且通过调用"getChildren()"实现他。在这个代码里，我们的"getChildren()"有两个参数。第一个声明节点从哪里读，第二个是一个boolean值能够让进程设置一个watch。这个代码里变量值是true。

```
/**
 * Join barrier
 *
 * @return
 * @throws KeeperException
 * @throws InterruptedException
 */

boolean enter() throws KeeperException, InterruptedException{
    zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL_SEQUENTIAL);
    while (true) {
        synchronized (mutex) {
            List<String> list = zk.getChildren(root, true);

            if (list.size() < size) {
                mutex.wait();
            } else {
                return true;
            }
        }
    }
}
```

注意enter()同时抛出了KeeperException和InterruptedException，所以需要应用catch并处理这些异常。

一旦计算结束，进程调用leave()离开barrier。首先它删除它对应的节点，然后它获取root节点的子节点。如果至少有一个子节点，然后它等待通知(注意调用getChidren()的第二个参数是true，表示Zookeeper必须在root节点设置一个watch)。接到通知时，它再次检查root节点是否有子节点。

```
/**
 * Wait until all reach barrier
 *
 * @return
 * @throws KeeperException
 * @throws InterruptedException
 */

boolean leave() throws KeeperException, InterruptedException{
    zk.delete(root + "/" + name, 0);
    while (true) {
        synchronized (mutex) {
            List<String> list = zk.getChildren(root, true);
                if (list.size() > 0) {
                    mutex.wait();
                } else {
                    return true;
                }
            }
        }
    }
}
```

## 生产者-消费者队列

生产者-消费者队列是一连串的生产和消费的分布式的数据结构重组，生产者创建新元素并添加到队列。消费者进程从集合中删除元素，并处理他们。在这个实现里，元素是简单的integer。队列由一个根节点代表，并添加元素到队列，一个生产者进程创建一个新节点，根节点的子节点。

下面的代码片段是对象的构造。与Barrier对象一样，它首先调用父类的构造，SyncPrimitive，如果Zookeeper对象不存在就创建一个。然后它验证队列的根节点是否存在，并且如果不存在就创建。

```
 /**
 * Constructor of producer-consumer queue
 *
 * @param address
 * @param name
 */
Queue(String address, String name) {
    super(address);
    this.root = name;
    // Create ZK node name
    if (zk != null) {
        try {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            System.out
                    .println("Keeper exception when instantiating queue: "
                            + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception");
        }
    }
}
```

生产者进程调用"produce"添加一个元素到队列，并传入一个integer作为参数。添加元素到队列，使用"create()"方法创建一个新节点，并使用SEQUENCE标记指示Zookeeper追加根节点顺序计数器的值。用这种方法，我们利用队列的元素上的最终顺序，保证最老的元素是下一个消费的元素。

```
/**
 * Add element to the queue.
 *
 * @param i
 * @return
 */

boolean produce(int i) throws KeeperException, InterruptedException{
    ByteBuffer b = ByteBuffer.allocate(4);
    byte[] value;

    // Add child with value i
    b.putInt(i);
    value = b.array();
    zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT_SEQUENTIAL);

    return true;
}
```

消费一个元素，消费者进程获得根节点的子节点，读取最小计数器值的节点，并返回元素。注意如果有冲突，然后两个竞争进程其中的一个将不能删除节点且删除操作会抛出一个异常。

调用getChildren()方法返回词典式排序的子节点列表。因为词典式排序没有必要按照计数器的数字排序，我们需要确定哪个元素是最小的。为了确定哪个是最小的计数器值，我们便利集合，并删除每个节点的前缀"element"。

```
/**
 * Remove first element from the queue.
 *
 * @return
 * @throws KeeperException
 * @throws InterruptedException
 */
int consume() throws KeeperException, InterruptedException{
    int retvalue = -1;
    Stat stat = null;

    // Get the first element available
    while (true) {
        synchronized (mutex) {
            List<String> list = zk.getChildren(root, true);
            if (list.size() == 0) {
                System.out.println("Going to wait");
                mutex.wait();
            } else {
                Integer min = new Integer(list.get(0).substring(7));
                for(String s : list){
                    Integer tempValue = new Integer(s.substring(7));
                    //System.out.println("Temporary value: " + tempValue);
                    if(tempValue < min) min = tempValue;
                }
                System.out.println("Temporary value: " + root + "/element" + min);
                byte[] b = zk.getData(root + "/element" + min,
                            false, stat);
                zk.delete(root + "/element" + min, 0);
                ByteBuffer buffer = ByteBuffer.wrap(b);
                retvalue = buffer.getInt();

                return retvalue;
            }
        }
    }
}
}
```

## 完整代码清单

### SyncPrimitive.java

```
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class SyncPrimitive implements Watcher {

    static ZooKeeper zk = null;
    static Integer mutex;

    String root;

    SyncPrimitive(String address) {
        if(zk == null){
            try {
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                mutex = new Integer(-1);
                System.out.println("Finished starting ZK: " + zk);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }
        }
        //else mutex = new Integer(-1);
    }

    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            //System.out.println("Process: " + event.getType());
            mutex.notify();
        }
    }

    /**
     * Barrier
     */
    static public class Barrier extends SyncPrimitive {
        int size;
        String name;

        /**
         * Barrier constructor
         *
         * @param address
         * @param root
         * @param size
         */
        Barrier(String address, String root, int size) {
            super(address);
            this.root = root;
            this.size = size;

            // Create barrier node
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                                CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out
                            .println("Keeper exception when instantiating queue: "
                                    + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }

            // My node name
            try {
                name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString());
            } catch (UnknownHostException e) {
                System.out.println(e.toString());
            }

        }

        /**
         * Join barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        boolean enter() throws KeeperException, InterruptedException{
            zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);

                    if (list.size() < size) {
                        mutex.wait();
                    } else {
                        return true;
                    }
                }
            }
        }

        /**
         * Wait until all reach barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        boolean leave() throws KeeperException, InterruptedException{
            zk.delete(root + "/" + name, 0);
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                        if (list.size() > 0) {
                            mutex.wait();
                        } else {
                            return true;
                        }
                    }
                }
        }
    }

    /**
     * Producer-Consumer queue
     */
    static public class Queue extends SyncPrimitive {

        /**
         * Constructor of producer-consumer queue
         *
         * @param address
         * @param name
         */
        Queue(String address, String name) {
            super(address);
            this.root = name;
            // Create ZK node name
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                                CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out
                            .println("Keeper exception when instantiating queue: "
                                    + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }
        }

        /**
         * Add element to the queue.
         *
         * @param i
         * @return
         */

        boolean produce(int i) throws KeeperException, InterruptedException{
            ByteBuffer b = ByteBuffer.allocate(4);
            byte[] value;

            // Add child with value i
            b.putInt(i);
            value = b.array();
            zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT_SEQUENTIAL);

            return true;
        }


        /**
         * Remove first element from the queue.
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */
        int consume() throws KeeperException, InterruptedException{
            int retvalue = -1;
            Stat stat = null;

            // Get the first element available
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                    if (list.size() == 0) {
                        System.out.println("Going to wait");
                        mutex.wait();
                    } else {
                        Integer min = new Integer(list.get(0).substring(7));
                        for(String s : list){
                            Integer tempValue = new Integer(s.substring(7));
                            //System.out.println("Temporary value: " + tempValue);
                            if(tempValue < min) min = tempValue;
                        }
                        System.out.println("Temporary value: " + root + "/element" + min);
                        byte[] b = zk.getData(root + "/element" + min,
                                    false, stat);
                        zk.delete(root + "/element" + min, 0);
                        ByteBuffer buffer = ByteBuffer.wrap(b);
                        retvalue = buffer.getInt();

                        return retvalue;
                    }
                }
            }
        }
    }

    public static void main(String args[]) {
        if (args[0].equals("qTest"))
            queueTest(args);
        else
            barrierTest(args);

    }

    public static void queueTest(String args[]) {
        Queue q = new Queue(args[1], "/app1");

        System.out.println("Input: " + args[1]);
        int i;
        Integer max = new Integer(args[2]);

        if (args[3].equals("p")) {
            System.out.println("Producer");
            for (i = 0; i < max; i++)
                try{
                    q.produce(10 + i);
                } catch (KeeperException e){

                } catch (InterruptedException e){

                }
        } else {
            System.out.println("Consumer");

            for (i = 0; i < max; i++) {
                try{
                    int r = q.consume();
                    System.out.println("Item: " + r);
                } catch (KeeperException e){
                    i--;
                } catch (InterruptedException e){

                }
            }
        }
    }

    public static void barrierTest(String args[]) {
        Barrier b = new Barrier(args[1], "/b1", new Integer(args[2]));
        try{
            boolean flag = b.enter();
            System.out.println("Entered barrier: " + args[2]);
            if(!flag) System.out.println("Error when entering the barrier");
        } catch (KeeperException e){

        } catch (InterruptedException e){

        }

        // Generate random integer
        Random rand = new Random();
        int r = rand.nextInt(100);
        // Loop for rand iterations
        for (int i = 0; i < r; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
        try{
            b.leave();
        } catch (KeeperException e){

        } catch (InterruptedException e){

        }
        System.out.println("Left barrier");
    }
}
```