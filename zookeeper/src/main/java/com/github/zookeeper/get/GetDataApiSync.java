package com.github.zookeeper.get;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-24
 * Time: 11:22 pm
 */
public class GetDataApiSync implements Watcher {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private static ZooKeeper zk = null;
    private static Stat stat = new Stat();

    public static void main(String[] args) throws Exception {
        //父节点路径
        String path = "/zk-book";
        //创建会话
        zk = new ZooKeeper("127.0.0.1:2181",
                5000, new GetDataApiSync());
        connectedSemaphore.await();
        //创建父节点
        zk.create(path, "123".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        //查看父节点内容
        System.out.println(new String(zk.getData(path, true, stat)));
        //查看父节点的节点状态信息
        System.out.println(stat.getCzxid() + "," + stat.getMzxid() + "," + stat.getVersion());

        zk.setData(path, "456".getBytes(), -1);

        Thread.sleep(Integer.MAX_VALUE);
    }

    @Override
    public void process(WatchedEvent event) {
        if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {
            if (Watcher.Event.EventType.None == event.getType() && null == event.getPath()) {
                connectedSemaphore.countDown();
            } else if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                try {
                    System.out.println(new String(zk.getData(event.getPath(), true, stat)));
                    System.out.println(stat.getCzxid() + "," +
                            stat.getMzxid() + "," +
                            stat.getVersion());
                } catch (Exception e) {
                }
            }
        }
    }
}
