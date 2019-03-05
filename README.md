# zookeeper-practice  
  
## zookeeper学习实践  
  
### 内含Curator以及Watcher操作demo，基于zookeeper实现的分布式锁demo  
  
[1.发展需求](#1发展需求)  
[2.什么是zookeeper？](#2什么是zookeeper)  
[3.zookeeper的安装部署(集群)](#3zookeeper的安装部署集群)  
[4.zookeeper的数据特点](#4zookeeper的数据特点)  
[5.zookeeper的集群](#5zookeeper的集群)  
[6.ZAB协议](#6zab协议)  
[7.Leader选举](#7leader选举)  
[8.事件机制](#8事件机制)  

### 1.发展需求
  
在分布式架构下，当服务越来越多，规模越来越大时，对应的机器数量也越来越大，单靠人工来管理和维护服务及地址的配置地址信息会越来越困难，单点故障的问题也开始凸显出来，一旦服务路由或者负载均衡服务器宕机，依赖他的所有服务均将失效。  
此时，需要一个能够动态注册和获取服务信息的地方，来统一管理服务名称和其对应的服务器列表信息，称之为服务配置中心。  
服务提供者在启动时，将其提供的服务名称、服务器地址注册到服务配置中心，服务消费者通过服务配置中心来获得需要调用的服务的机器列表。通过相应的负载均衡算法，选取其中一台服务器进行调用。  
当服务器宕机或者下线时，相应的机器需要能够动态地从服务配置中心里面移除，并通知相应的服务消费者，否则服务消费者就有可能因为调用到已经失效服务而发生错误。在这个过程中，服务消费者只有在第一次调用服务时需要查询服务配置中心，然后将查询到的信息缓存到本地，后面的调用直接使用本地缓存的服务地址列表信息，而不需要重新发起请求道服务配置中心去获取相应的服务地址列表，直到服务的地址列表有变更(机器上线或者下线)。  
  
### 2.什么是zookeeper？  
  
zookeeper是一个开源的分布式协调中间件，由雅虎公司创建，是google chubby的开源实现。zookeeper的设计目标是将那些复杂且容易出错的分布式一致性服务封装起来，并且以一些列简单易用的接口提供给用户使用。实现了负载均衡，分布式锁等功能。  

### 3.zookeeper的安装部署(集群)  
  
下载 zookeeper 安装包:
 http://apache.fayea.com/zookeeper/ 下载完成，通过 tar -zxvf 解压 
>常用命令  
1.启动ZK服务:  
bin/zkServer.sh start  
2.查看 ZK 服务状态:  
bin/zkServer.sh status   
3.停止 ZK 服务:   
bin/zkServer.sh stop   
4.重启 ZK 服务:  
bin/zkServer.sh restart  
5.连接服务器  
zkCli.sh -timeout 0 -r -server ip:port  
  
初次使用zookeeper，需要将conf目录下的zoo_sample.cfg文件copy一份重命名为zoo.cfg,修改dataDir目录，dataDir表示日志文件存放的路径。  
在 zookeeper 集群中，各个节点总共有三种角色，分别是: leader，follower，observer  
集群模式我们采用模拟 3 台机器来搭建 zookeeper 集群。 分别复制安装包到三台机器上并解压，同时 copy 一份 zoo.cfg。  
1. 修改配置文件  
修改端口  
server.1=IP1:2888:3888 【2888:访问 zookeeper 的端口; 3888:重新选举 leader 的端口】  
server.2=IP2.2888:3888  
server.3=IP3.2888:2888  
server.A=B:C:D:其中 
A 是一个数字，表示这个是第几号服务器; B 是这个服务器的 ip 地址;  
C 表示的是这个服务器与集群中的 Leader 服务器交换信息的端口;  
D 表示的是万一集群中的 Leader 服务器挂了，需要一个端口来重新进行选举，选出一个新的 Leader，  
而这个端口就是用来执行选举时服务器相互通信的端口。如果是伪集群的配置方式，由于 B 都是一样，
所以不同的 Zookeeper 实例通信端口号不能一样，所以要给它们分配不同的端口号。 
  
2. 新建 datadir 目录，设置 myid  
在每台 zookeeper 机器上，我们都需要在数据目录(dataDir) 下创建一个 myid 文件，该文件只有一行内容，对应每台机 器的 Server ID 数字;比如 server.1 的 myid 文件内容就是1。【必须确保每个服务器的 myid 文件中的数字不同，并且 和自己所在机器的 zoo.cfg 中 server.id 的 id 值一致，id 的 范围是 1~255】  
3. 启动 zookeeper  
  
### 4.zookeeper的数据特点  
  
zookeeper以znode为基本数据存储单元，所有znode节点呈现出类似文件目录的层次结构。  
znode节点可以分为以下四种：  

>PERSISTENT                持久化节点  
PERSISTENT_SEQUENTIAL     顺序自动编号持久化节点，这种节点会根据当前已存在的节点数自动加1  
EPHEMERAL                 临时节点， 客户端session超时这类节点就会被自动删除  
EPHEMERAL_SEQUENTIAL      临时自动编号节点  
  
节点的特性：  
>同级节点有唯一性  
临时节点不能有子节点  
节点拥有版本  

### 5.zookeeper的集群  
在 zookeeper 中，客户端会随机连接到 zookeeper 集群中的一个节点，如果是读请求，就直接从当前节点中读取数据，如果是写请求，那么请求会被转发给 Leader 提交事务， 然后 Leader 会广播事务，只要有超过半数节点写入成功， 那么写请求就会被提交(类 2PC 事务)。  
![](https://github.com/YufeizhangRay/image/blob/master/zookeeper/2PC.jpeg)  
  
所有事务请求必须由一个全局唯一的服务器来协调处理， 这个服务器就是 Leader 服务器，其他的服务器就是 Follower。Leader 服务器把客户端的请求转化成一个事务 Proposal(提议)，并把这个 Proposal 分发给集群中的 所有 Follower 服务器。之后 Leader 服务器需要等待所有 Follower 服务器的反馈，一旦超过半数的 Follower 服务器 进行了正确的反馈，那么 Leader 就会再次向所有的 Follower 服务器发送 Commit 消息，要求各个 Follower 节点对前面的一个 Proposal 进行提交;  
  
#### Leader  
Leader 服务器是整个 zookeeper 集群的核心，主要的工作任务有两项  
>1. 事务请求的唯一调度和处理者，保证集群事务处理的顺序性
>2. 集群内部各服务器的调度者  
  
#### Follower  
Follower 角色的主要职责是  
>1. 处理客户端非事务请求、转发事务请求给Leader服务器  
>2. 参与事务请求 Proposal 的投票(需要半数以上服务器通过才能通知 Leader commit 数据; Leader 发起的提案， 要求 Follower 投票)  
>3. 参与 Leader 选举的投票  
  
#### Observer  
Observer 是 zookeeper3.3 开始引入的一个全新的服务器角色，从字面来理解，该角色充当了观察者的角色。观察 zookeeper 集群中的最新状态变化并将这些状态变化同步到 observer 服务器上。Observer 的工作原理与 Follower 角色基本一致，而它和 Follower 角色唯一的不同在于 observer 不参与任何形式的投票，包括事务请求 Proposal 的投票和 Leader 选举的投票。简单来说，observer 服务器只提供非事务请求服务，通常在于不影响集群事务处理能力的前提下提升集群非事务处理的能力。  
  
#### 集群组成  
通常 zookeeper 是由 2n+1 台 server 组成，每个 server 都知道彼此的存在。对于 2n+1 台 server，只要有 n+1 台(大多数)server 可用，整个系统保持可用。一个 zookeeper 集群如果要对外提供可用的服务，那么集群中必须要有过半的机器正常工作并且彼此之间能够正常通信，基于这个特性，如果向搭建一个能够允许 F 台机器 down 掉的集群，那么就要部署2F+1台服务器构成的 zookeeper 集群。因此 3 台机器构成的 zookeeper 集群，能够在挂掉一台机器后依然正常工作。一个 5 台机器集群的服务，能够对 2 台机器怪调的情况下进行容灾。如果一台由 6 台服务构成的集群，同样只能挂掉 2 台机器。因此，5 台和 6 台在容灾能力上并没有明显优势，反而增加了网络通信负担。系统启动时，集群中的 server 会选举出一台 server 为 Leader，其它的就作为 Follower(这里先不考虑 observer 角色)。 之所以要满足这样一个等式，是因为一个节点要成为集群 中的 leader，需要有超过及群众过半数的节点支持，这个涉及到 leader 选举算法。同时也涉及到事务请求的提交投票。  

### 6.ZAB协议  
ZAB(Zookeeper Atomic Broadcast) 协议是为分布式协调服务 ZooKeeper 专门设计的一种支持崩溃恢复的原子广播协议。在 ZooKeeper 中，主要依赖 ZAB 协议来实现分布式数据一致性，基于该协议，ZooKeeper 实现了一种主备模式的系统架构来保持集群中各个副本之间的数据一致性。  
ZAB 协议包含两种基本模式，分别是  
>1. 崩溃恢复 
>2. 消息广播  
  
当整个集群在启动时，或者当 Leader 节点出现网络中断、 崩溃等情况时，ZAB 协议就会进入恢复模式并选举产生新的 Leader，当 Leader 服务器选举出来后，并且集群中有过半的机器和该 Leader 节点完成数据同步后，ZAB 协议就会退出恢复模式。当集群中已经有过半的 Follower 节点完成了和 Leader 状态同步以后，那么整个集群就进入了消息广播模式。这个时候，在 Leader 节点正常工作时，启动一台新的服务器加入到集群，那这个服务器会直接进入数据恢复模式，和 Leader 节点进行数据同步。同步完成后即可正常对外提供非事务请求的处理。  
  
#### 消息广播的实现原理  
  
消息广播的过程实际上是一个 简化版本的二阶段提交(2PC)过程  
>1. Leader 接收到事务消息请求后，将消息赋予一个全局唯一的64 位自增 id，叫:zxid，通过 zxid 的大小比较既可以实现因果有序这个特征  
>2. Leader 为每个 Follower 准备了一个 FIFO 队列(通过TCP协议来实现，以实现了全局有序这一个特点)将带有 zxid 的消息作为一个提案(proposal)分发给所有的 Follower  
>3. 当 Follower 接收到 proposal，先把 proposal 写到磁盘，写入成功以后再向 Leader 回复一个 ack  
>4. 当 Leader 接收到合法数量(超过半数节点)的 ACK 后，Leader 就会向这些 Follower 发送 commit 命令，同时会在本地执行该消息  
>5. 当 Follower 收到消息的 commit 命令以后，会提交该消息  
![](https://github.com/YufeizhangRay/image/blob/master/zookeeper/%E6%B6%88%E6%81%AF%E5%B9%BF%E6%92%AD.jpeg)  
  
Leader 的投票过程，不需要 Observer 的 ack，也就是 Observer 不需要参与投票过程，但是 Observer 必须要同步 Leader 的数据从而在处理请求的时候保证数据的一致性。    

#### 崩溃恢复(数据恢复)  
  
ZAB 协议的这个基于原子广播协议的消息广播过程，在正常情况下是没有任何问题的，但是一旦 Leader 节点崩溃，或者由于网络问题导致 Leader 服务器失去了过半的Follower 节点的联系，那么就会进入到崩溃恢复模式。在 ZAB 协议中，为了保证程序的正确运行，整个恢复过程结束后需要选举出一个新的 Leader 为了使 Leader 挂了后系统能正常工作，需要解决以下两个问题:  
  
>1. 已经被处理的消息不能丢失  
>当 Leader 收到合法数量 Follower 的 ACKs 后，就向 各个 Follower 广播 commit 命令，同时也会在本地 执行 commit 并向连接的客户端返回「成功」。但是如 果在各个 Follower 在收到 commit 命令前 Leader 就挂了，导致剩下的服务器并没有执行都这条消息。  
![](https://github.com/YufeizhangRay/image/blob/master/zookeeper/%E5%B4%A9%E6%BA%83%E6%81%A2%E5%A4%8D.jpeg)  
Leader 对事务消息发起 commit 操作，该消息在 Follower1 上执行了，但是 Follower2 还没有收到 commit，就已经挂了，而实际上客户端已经收到该事务消息处理成功的回执了。所以在 zab 协议下需要保证所有机器都要执行这个事务消息。  
  
>2. 被丢弃的消息不能再次出现  
>当 Leader 接收到消息请求生成 proposal 后就挂了，其他 Follower 并没有收到此 proposal，因此经过恢复模式重新选了 leader 后，这条消息是被跳过的。 此时，之前挂了的 Leader 重新启动并注册成了 Follower，他保留了被跳过消息的 proposal 状态，与整个系统的状态是不一致的，需要将其删除。  
  
ZAB 协议需要满足上面两种情况，就必须要设计一个 Leader 选举算法:能够确保已经被 Leader 提交的事务 proposal 能够提交、同时丢弃已经被跳过的事务 proposal。 
针对这个要求,如果 Leader 选举算法能够保证新选举出来的 Leader 服务器拥有集群中所有机器最高编号(ZXID 最大)的事务 proposal，那么就可以保证这个新选举出来的 Leader 一定具有已经提交的提案。因为所有提案被 commit 之前必须有超过半数的 Follower ACK，即必须有超过半数节点的服务器的事务日志上有该提案的 proposal，因此，只要有合法数量的节点正常工作，就必然有一个节点保存了所有被 commit 消息的 proposal 状态。  
另外一个，zxid 有 64 位，高 32 位是 epoch 编号，低 32 位是消息计数器，每接收到一条消息 这个值+1，新 Leader 选举后这个值重置为 0。这样设计的好处在于老的 Leader 挂了以后重启，它不会被选举为 Leader，因为此时它的 zxid 肯定小于当前新的 Leader。当老的 Leader 作为 Follower 接入新的 Leader 后，新的 Leader 会让它将所有的拥有旧的 epoch 号的未被 commit 的 proposal 清除。  
epoch:可以理解为当前集群所处的年代或者周期，每个 Leader 就像皇帝，都有自己的年号，所以每次改朝换代， Leader 变更之后，都会在前一个年代的基础上加 1。这样 就算旧的 Leader 崩溃恢复之后，也没有人听他的了，因为 Follower 只听从当前年代的 Leader 的命令。  
  
### 7.Leader选举  
  
配置选举算法，选举算法有 3 种，可以通过在 zoo.cfg 里面进行配置，默认是 fast 选举(FastLeaderElection)。  

Leader 选举分两种  
>启动的时候的 Leader 选举   
Leader 崩溃的时候的的选举  
  
#### 服务器启动时的 Leader 选举  

每个节点启动的时候状态都是 LOOKING，处于观望状态， 接下来就开始进行选主流程  
进行 Leader 选举，至少需要两台机器，我们选取 3 台机器组成的服务器集群为例。在集群初始化阶段，当有一台服务器 Server1 启动时，它本身是无法进行和完成 Leader 选举，当第二台服务器 Server2 启 动时，这个时候两台机器可以相互通信，每台机器都试图 找到 Leader，于是进入 Leader 选举过程。选举过程如下  
>(1) 每个 Server 发出一个投票。由于是初始情况，Server1 和 Server2 都会将自己作为 Leader 服务器来进行投票，每次投票会包含所推举的服务器的 myid 和 ZXID、epoch，使用(myid, ZXID,epoch)来表示，此时 Server1 的投票为(1, 0, 0)，Server2 的投票为(2, 0, 0)，然后各自将这个投票发给集群中其他机器。  
(2) 接受来自各个服务器的投票。集群的每个服务器收到投票后，首先判断该投票的有效性，如检查是否是本轮投票(epoch)、是否来自 LOOKING 状态的服务器。  
(3) 处理投票。针对每一个投票，服务器都需要将别人的投票和自己的投票进行PK，PK规则如下:  
>>i.优先检查 epoch 的大小，epoch 比较大的服务器优先作为 Leader  
ii. 如果 epoch 相同，检查 ZXID。ZXID 比较大的服务器优先作为 Leader  
iii. 如果 ZXID 相同，那么就比较 myid。myid 较大的 服务器作为 Leader 服务器。  
对于 Server1 而言，它的投票是(1, 0, 0)，接收 Server2 的投票为(2, 0, 0)，首先会比较两者的 epoch，均为 0，在比较ZXID，也均为0，于是再比较 myid，此时 Server2 的 myid 最大，于是更新自己的投票为(2, 0, 0)，然后重新投票，对于 Server2 而言， 它不需要更新自己的投票，只是再次向集群中所有机器发出上一次投票信息即可。  
(4) 统计投票。每次投票后，服务器都会统计投票信息， 判断是否已经有过半机器接受到相同的投票信息，对于 Server1、Server2 而言，都统计出集群中已经有两台机器接受了(2, 0, 0)的投票信息，此时便认为已经选出了 Leader。  
(5) 改变服务器状态。一旦确定了 Leader，每个服务器就会更新自己的状态，如果是 Follower，那么就变更为 FOLLOWING，如果是 Leader，就变更为 LEADING。  

#### 运行过程中的 Leader 选举  
  
当集群中的 leader 服务器出现宕机或者不可用的情况时， 那么整个集群将无法对外提供服务，而是进入新一轮的 Leader 选举，服务器运行期间的 Leader 选举和启动时期 的 Leader 选举基本过程是一致的。  
>(1) 变更状态。Leader 挂后，余下的非 Observer 服务器都会将自己的服务器状态变更为 LOOKING，然后开始进入 Leader 选举过程。  
(2) 每个 Server 会发出一个投票。在运行期间，每个服务器上的 ZXID 可能不同，此时假定 Server1 的 ZXID 为 123，Server3 的 ZXID 为 122;在第一轮投票中，Server1 和 Server3 都会投自己，产生投票(1, 123, 1)，(3, 122, 1)， 然后各自将投票发送给集群中所有机器。接收来自各个服务器的投票。与启动时过程相同。  
(3) 处理投票。与启动时过程相同，此时，Server1 将会成 为 Leader。  
(4) 统计投票。与启动时过程相同。  
(5) 改变服务器的状态。与启动时过程相同。  
  
### 8.事件机制  
  
Watcher 监听机制是 Zookeeper 中非常重要的特性，我们基于 zookeeper 上创建的节点，可以对这些节点绑定监听事件，比如可以监听节点数据变更、节点删除、子节点状态变更等事件，通过这个事件机制，可以基于 zookeeper 实现分布式锁、集群管理等功能。  
  
#### watcher 特性:   
当数据发生变化的时候 zookeeper 会产生一个 watcher 事件，并且会发送到客户端。但是客户端只会收到一次通知。如果后续这个节点再次发生变化，那么之前设置 watcher 的客户端不会再次收到消息。 watcher 是一次性的操作，可以通过循环监听去达到永久监听效果。  
  
#### 如何注册事件机制  
通过这三个操作来绑定事件 :getData、Exists、getChildren  
  
#### 如何触发事件  
凡是事务类型的操作，都会触发监听事件。create /delete /setData  
  
#### watcher 事件类型  
>None (-1), 客户端链接状态发生变化的时候，会收到 none 的事件  
NodeCreated (1), 创建节点的事件  
NodeDeleted (2), 删除节点事件  
NodeDataChanged (3), 节点数据发生变更   
NodeChildrenChanged (4); 子节点被创建、被删除、会发生事件触发  
  
#### 什么样的操作会产生什么类型的事件  
```
                                   zk-persis-Ray (监听事件)              zk-persis-Ray/child (监听事件)
create(/zk-persis-Ray)             NodeCreated(exists/getData)          无
delete(/zk-persis-Ray)             NodeDeleted(exists/getData)          无
setData(/zk-persis-Ray)            NodeDataChanged(exists/getData)      无
create (/zk-persis-Ray/children)   NodeChildrenChanged(getchild)        NodedCreated
delete (/zk-persis-Ray/children)   NodeChildrenChanged(getchild)        NodedDeleted
setData(/zk-persis-Ray/children)                                        NodeDataChanged
```
#### 事件大致原理图解  
>客户端会在服务端上面注册监听事件，本身也会对其进行保存。  
服务端对事件进行绑定，在事件被触发的时候通过process方法回调。  
  
![](https://github.com/YufeizhangRay/image/blob/master/zookeeper/%E6%80%BB%E5%8E%9F%E7%90%86%E5%9B%BE.jpeg)  
  
>客户端的exist方法会将request进行标记，并设置为使用监听。同时生成watchRegistration，组装到zookeeper基本的通信单元packet中，并将packet加入到outgoingqueued队列。  
zookeeper在构造方法中会启动SendThread线程，从outgoingqueued队列中取得数据包，序列化(并没有序列化整个watchRegistration)requestHeader、request，并通过Netty的通道发送。    
  
![](https://github.com/YufeizhangRay/image/blob/master/zookeeper/%E7%9B%91%E5%90%AC%E7%BB%91%E5%AE%9A.jpeg)  
  
>服务端收到数据后会对反序列化的packet进行处理，组装Request。  
构建Processor链条，提供不同的处理职能。  
处理Request，拿到stat状态信息，获取watcher，绑定事件。  
服务端通过Netty通道返回信息。  
  
![](https://github.com/YufeizhangRay/image/blob/master/zookeeper/%E6%9C%8D%E5%8A%A1%E7%AB%AF.jpeg)  
  
>客户端收到信息后，SendThread通过readResponse拿到packet。  
通过finishPacket方法注册事件，将事件保存在ZkWatcherManager。  
  
![](https://github.com/YufeizhangRay/image/blob/master/zookeeper/%E5%AE%A2%E6%88%B7%E7%AB%AF.jpeg)   
  
事件触发  
通过watchManager的triggerWatch来触发事件。  
>1.封装watchedEvent  
2.查询Watcher  
3.调用process方法触发watcher  
>>process方法将watchedEvent包装成watcherEvent调用sendResponse方法发送给客户端，由客户端实现真正的事件调用逻辑。  
  
>客户端从ZkWatcherManager中取出对应的watcher加入waitingEvent队列。  
EventThread的run方法会不断的从队列中取出事件，然后使用processEvent方法中的process方法处理事件。  
  
