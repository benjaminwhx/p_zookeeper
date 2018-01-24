package com.github.zookeeper.get;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-24
 * Time: 11:24 pm
 */
public class GetDataApiAsync implements Watcher {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private static ZooKeeper zk;

    public static void main(String[] args) throws Exception {

        String path = "/zk-book";
        zk = new ZooKeeper("127.0.0.1:2181",
                5000, new GetDataApiAsync());
        connectedSemaphore.await();

        zk.create( path, "123".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL );

        zk.getData( path, true, new IDataCallback(), null );

        zk.setData( path, "123".getBytes(), -1 );

        Thread.sleep( Integer.MAX_VALUE );
    }

    @Override
    public void process(WatchedEvent event) {
        if (Event.KeeperState.SyncConnected == event.getState()) {
            if (Event.EventType.None == event.getType() && null == event.getPath()) {
                connectedSemaphore.countDown();
            } else if (event.getType() == Event.EventType.NodeDataChanged) {
                try {
                    zk.getData( event.getPath(), true, new IDataCallback(), null );
                } catch (Exception e) {}
            }
        }
    }
}

class IDataCallback implements AsyncCallback.DataCallback{

    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        System.out.println(rc + ", " + path + ", " + new String(data));
        System.out.println(stat.getCzxid()+","+
                stat.getMzxid()+","+
                stat.getVersion());
    }
}
