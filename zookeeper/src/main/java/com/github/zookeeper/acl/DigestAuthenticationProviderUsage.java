package com.github.zookeeper.acl;

import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.security.NoSuchAlgorithmException;

/**
 * User: benjamin.wuhaixu
 * Date: 2018-01-24
 * Time: 4:26 pm
 */
public class DigestAuthenticationProviderUsage {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println(DigestAuthenticationProvider.generateDigest("foo:zk-book"));
    }
}
