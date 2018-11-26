package cn.zyf.zookeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ConnectionDemo {

	public static void main(String[] args) {
		
		try {
			final CountDownLatch countDownLatch=new CountDownLatch(1);
			ZooKeeper zooKeeper = 
					new ZooKeeper("192.168.188.135:2181,192.168.188.136:2181,192.168.188.137:2181", 
							4000, new Watcher() {
								
								@Override
								public void process(WatchedEvent event) {
									if(Event.KeeperState.SyncConnected==event.getState()){
		                                //如果收到了服务端的响应事件，连接成功
		                                countDownLatch.countDown();
		                            }
								}
							});
			countDownLatch.await();
			System.out.println(zooKeeper.getState());//CONNECTED
			
			//添加节点
            zooKeeper.create("/zk-persis-Ray","0".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
            Thread.sleep(1000);
            Stat stat=new Stat();

            //得到当前节点的值
            byte[] bytes=zooKeeper.getData("/zk-persis-Ray",null,stat);
            System.out.println(new String(bytes));

            //修改节点值
            zooKeeper.setData("/zk-persis-Ray","1".getBytes(),stat.getVersion());

            //得到当前节点的值
            byte[] bytes1=zooKeeper.getData("/zk-persis-Ray",null,stat);
            System.out.println(new String(bytes1));

            zooKeeper.delete("/zk-persis-Ray",stat.getVersion());

			zooKeeper.close();
			
			System.in.read();
		} catch (IOException | InterruptedException | KeeperException e) {
			e.printStackTrace();
		}
	}
}
