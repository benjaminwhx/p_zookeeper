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
 */
public class Simple implements Watcher {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);

    public static void main(String[] args) throws IOException {
        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new Simple());
        // 初始化是一个异步的过程，这时候还没有初始化好，状态是CONNECTING
        System.out.println(zooKeeper.getState());
        try {
            connectedSemaphore.await();
        } catch (InterruptedException e) {

        }
        // 初始化好了。状态是CONNECTED
        System.out.println("Zookeeper session established: " + zooKeeper.getState());
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        System.out.println("Receive watched event: " + watchedEvent);
        if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
            connectedSemaphore.countDown();
        }
    }
}
