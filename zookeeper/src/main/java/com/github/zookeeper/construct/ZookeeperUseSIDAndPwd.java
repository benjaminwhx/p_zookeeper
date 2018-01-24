package com.github.zookeeper.construct;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-24
 * Time: 1:00 pm
 *
 * 创建一个复用sessionId和sessionPassword的ZooKeeper对象实例
 */
public class ZookeeperUseSIDAndPwd implements Watcher {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);

    public static void main(String[] args) throws IOException, InterruptedException {
        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new ZookeeperUseSIDAndPwd());
        connectedSemaphore.await();

        long sessionId = zooKeeper.getSessionId();
        byte[] pwd = zooKeeper.getSessionPasswd();

        System.out.println("sessionId: " + sessionId + ", pwd: " + new String(pwd));
        // 使用非法的sessionId和pwd
        zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new ZookeeperUseSIDAndPwd(), 1L, "test".getBytes());
        // 使用正确的sessionId和pwd
        zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new ZookeeperUseSIDAndPwd(), sessionId, pwd);
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        System.out.println("Receive watched event: " + watchedEvent);
        if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
            connectedSemaphore.countDown();
        }
    }
}
