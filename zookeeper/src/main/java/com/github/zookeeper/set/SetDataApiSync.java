package com.github.zookeeper.set;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-24
 * Time: 11:30 pm
 */
public class SetDataApiSync implements Watcher {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private static ZooKeeper zk;

    public static void main(String[] args) throws Exception {

        String path = "/zk-book";
        zk = new ZooKeeper("127.0.0.1:2181",
                5000, new SetDataApiSync());
        connectedSemaphore.await();

        zk.create( path, "123".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL );
        zk.getData( path, true, null );

        //第一次修改：
        //数据版本都是从0开始计数的，所以严格地讲，“-1”并不是一个合法的数据版本，他仅仅是一个标识符，
        //如果客户端传入的版本参数是“-1”，就是告诉Zookeeper服务器，客户端需要基于数据的最新版本进行更新操作。
        Stat stat = zk.setData( path, "456".getBytes(), -1 );
        System.out.println(stat.getCzxid()+","+
                stat.getMzxid()+","+
                stat.getVersion());
        //第二次修改：
        Stat stat2 = zk.setData( path, "456".getBytes(), stat.getVersion() );
        System.out.println(stat2.getCzxid()+","+
                stat2.getMzxid()+","+
                stat2.getVersion());
        //第三次修改：
        try {
            zk.setData( path, "456".getBytes(), stat.getVersion() );
        } catch ( KeeperException e ) {
            System.out.println("Error: " + e.code() + "," + e.getMessage());
        }
        Thread.sleep( Integer.MAX_VALUE );
    }

    @Override
    public void process(WatchedEvent event) {
        if (Event.KeeperState.SyncConnected == event.getState()) {
            if (Event.EventType.None == event.getType() && null == event.getPath()) {
                connectedSemaphore.countDown();
                int i = 0;
                System.out.println("process:" + i++);
            }
        }
    }
}
