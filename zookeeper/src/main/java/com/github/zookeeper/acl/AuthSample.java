package com.github.zookeeper.acl;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-25
 * Time: 00:24 am
 */
public class AuthSample {

    final static String PATH = "/zk-book-auth_test";
    public static void main(String[] args) throws Exception {

        ZooKeeper zookeeper = new ZooKeeper("127.0.0.1:2181", 50000, null);
        zookeeper.addAuthInfo("digest", "foo:true".getBytes());
        zookeeper.create( PATH, "init".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.EPHEMERAL );
        Thread.sleep( Integer.MAX_VALUE );
    }
}
