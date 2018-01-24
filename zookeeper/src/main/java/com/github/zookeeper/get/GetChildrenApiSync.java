package com.github.zookeeper.get;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-24
 * Time: 6:59 pm
 */
public class GetChildrenApiSync implements Watcher {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private static ZooKeeper zk = null;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        String path = "/zk-book";
        zk = new ZooKeeper("127.0.0.1:2181", 5000, new GetChildrenApiSync());
        connectedSemaphore.await();

        // 通知一次性，需要反复注册
        zk.exists(path, true);
        zk.create(path, "init".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.exists(path + "/c1", true);
        zk.create(path + "/c1", "initc1".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

        List<String> childrenList = zk.getChildren(path, true);
        System.out.println(childrenList);

        zk.create(path + "/c2", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    zk.delete(path, );
//                } catch (Exception e) {
//                }
//            }
//        }));
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Override
    public void process(WatchedEvent event) {
        if (Event.KeeperState.SyncConnected == event.getState()) {
            if (Event.EventType.None == event.getType() && null == event.getPath()) {
                connectedSemaphore.countDown();
            } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
                try {
                    System.out.println("ReGet Child: " + zk.getChildren(event.getPath(), true));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (event.getType() == Event.EventType.NodeCreated) {
                System.out.println("Node create: " + event.getPath());
            }
        }
    }
}
