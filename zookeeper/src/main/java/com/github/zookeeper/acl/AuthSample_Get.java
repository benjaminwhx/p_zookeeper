package com.github.zookeeper.acl;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-25
 * Time: 00:25 am
 */
public class AuthSample_Get {

    final static String PATH = "/zk-book-auth_test";
    public static void main(String[] args) throws Exception {

        ZooKeeper zookeeper1 = new ZooKeeper("127.0.0.1:2181",5000,null);
        zookeeper1.addAuthInfo("digest", "foo:true".getBytes());
        zookeeper1.create( PATH, "init".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.EPHEMERAL );

        ZooKeeper zookeeper2 = new ZooKeeper("127.0.0.1:2181",50000,null);
        // org.apache.zookeeper.KeeperException$NoAuthException: KeeperErrorCode = NoAuth for /zk-book-auth_test
        zookeeper2.getData( PATH, false, null );
    }
}
