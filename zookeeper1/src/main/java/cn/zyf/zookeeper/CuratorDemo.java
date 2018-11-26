package cn.zyf.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

public class CuratorDemo {

	public static void main(String[] args) throws Exception {
		CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
				.connectString("192.168.188.135:2181,192.168.188.136:2181,192.168.188.137:2181")
				.sessionTimeoutMs(4000).retryPolicy(new ExponentialBackoffRetry(1000, 3))
				.namespace("curator").build();

		curatorFramework.start();

		// 结果: /curator/Ray/node1
		// 原生api中，必须是逐层创建，也就是父节点必须存在，子节点才能创建
		
//		 curatorFramework.create().creatingParentsIfNeeded().
//		 withMode(CreateMode.PERSISTENT). forPath("/Ray/node1","1".getBytes());
		 
		// 删除
//		curatorFramework.delete().deletingChildrenIfNeeded().forPath("/Ray/node1");

		Stat stat = new Stat();
		curatorFramework.getData().storingStatIn(stat).forPath("/Ray/node1");

		curatorFramework.setData().withVersion(stat.getVersion()).forPath("/Ray/node1", "xx".getBytes());

		curatorFramework.close();

	}

}
