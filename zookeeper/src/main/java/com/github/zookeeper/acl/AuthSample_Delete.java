package com.github.zookeeper.acl;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-25
 * Time: 00:38 am
 */
public class AuthSample_Delete {

    final static String PATH  = "/zk-book-auth_test";
    final static String PATH2 = "/zk-book-auth_test/child";

    public static void main(String[] args) throws Exception {
        ZooKeeper zookeeper1 = new ZooKeeper("127.0.0.1:2181", 5000, null);
        zookeeper1.addAuthInfo("digest", "foo:true".getBytes());
        zookeeper1.create( PATH, "init".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT );
        zookeeper1.create( PATH2, "init".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.EPHEMERAL );

        try {
            ZooKeeper zookeeper2 = new ZooKeeper("127.0.0.1:2181",50000,null);
            zookeeper2.delete( PATH2, -1 );
        } catch ( Exception e ) {
            System.out.println( "删除节点失败: " + e.getMessage() );
        }

        ZooKeeper zookeeper3 = new ZooKeeper("127.0.0.1:2181", 50000, null);
        zookeeper3.addAuthInfo("digest", "foo:true".getBytes());
        zookeeper3.delete( PATH2, -1 );
        System.out.println( "成功删除节点：" + PATH2 );

        /**
         * 当客户端对一个数据节点添加了权限信息后，对于删除操作而言，其作用范围是其子节点。
         * 也就是说，当我们对一个数据节点添加权限信息后，依然可以自由地删除这个节点，但是对于
         * 这个节点的子节点，就必须使用相应的权限信息才能够删除它。
         */
        ZooKeeper zookeeper4 = new ZooKeeper("127.0.0.1:2181", 50000, null);
        zookeeper4.delete( PATH, -1 );
        System.out.println( "成功删除节点：" + PATH );
    }
}
