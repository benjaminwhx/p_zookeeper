package com.github.zookeeper.acl;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-24
 * Time: 4:35 pm
 */
public class AuthSuper {

    final static String PATH = "/zk-book";

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, null);
        zooKeeper.addAuthInfo("digest", "foo:true".getBytes());
        zooKeeper.create(PATH, "init".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.EPHEMERAL);

        ZooKeeper zooKeeper2 = new ZooKeeper("127.0.0.1:2181", 5000, null);
        zooKeeper2.addAuthInfo("digest", "foo:zk-book".getBytes());
        System.out.println(new String(zooKeeper2.getData(PATH, false, null)));

        ZooKeeper zooKeeper3 = new ZooKeeper("127.0.0.1:2181", 5000, null);
        zooKeeper3.addAuthInfo("digest", "foo:false".getBytes());
        System.out.println(zooKeeper3.getData(PATH, false, null));
    }
}
