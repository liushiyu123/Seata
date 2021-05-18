# 分布式事务 seata-demo

#### 版本介绍
**JDK**：1.8  
**spring-cloud.version**：Hoxton.SR8  
**alibaba.version**： 2.2.1.RELEASE  
**seata.version**：1.3.0  
**nacos**: 1.3.1


#### 整合步骤

1.  [下载nacos](https://github.com/alibaba/nacos/releases)我下载的nacos-server-1.3.2.zip
2.  [seata](https://github.com/seata/seata/releases)我下载的是 seata-server-1.3.0.zip
3.  创建数据库 server [数据库脚本](https://github.com/seata/seata/blob/1.3.0/script/server/db/mysql.sql) 同时每一个参与事务的数据库需要添加一张表 [下载地址](https://github.com/seata/seata/blob/1.3.0/script/client/at/db/mysql.sql)
4.  上传seata配置到nacos
    1.  下载[nacos-config.sh](https://github.com/seata/seata/tree/develop/script/config-center/nacos)文件,放到解压文件conf文件夹下
    2.  修改conf目录下file.conf配置文件，主要是修改自定义事务组名称，事务日志存储模式为db，并修改数据库连接信息
		```
		service {
		  #vgroup->rgroup
		  vgroup_mapping.tx-service-group = "default" #修改事务组名称为：tx-service-group，和客户端自定义的名称对应
		  #only support single node
		  default.grouplist = "127.0.0.1:8091"
		  #degrade current not support
		  enableDegrade = false
		  #disable
		  disable = false
		  #unit ms,s,m,h,d represents milliseconds, seconds, minutes, hours, days, default permanent
		  max.commit.retry.timeout = "-1"
		  max.rollback.retry.timeout = "-1"
		}
		## transaction log store, only used in seata-server
		store {
		  ## store mode: file、db、redis
		  mode = "db"
		  ## database store property
		  db {
			## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp)/HikariDataSource(hikari) etc.
			datasource = "druid"
			## mysql/oracle/postgresql/h2/oceanbase etc.
			dbType = "mysql"
			driverClassName = "com.mysql.jdbc.Driver"
			url = "jdbc:mysql://192.168.240.129:3306/seat_server"
			user = "root"
			password = "Root_123456"
			minConn = 5
			maxConn = 30
			globalTable = "global_table"
			branchTable = "branch_table"
			lockTable = "lock_table"
			queryLimit = 100
			maxWait = 5000
		  }
		}
		```
    3.  修改conf目录下 registry.conf配置，指明注册中心为nacos，及修改nacos连接信息。
		```
		registry {
		  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
		  type = "nacos"
		  nacos {
			application = "seata-server"
			serverAddr = "127.0.0.1:8848"
			group = "default"
			namespace = ""
			cluster = "default"
			username = "nacos"
			password = "nacos"
		  }
		}

		config {
		  # file、nacos 、apollo、zk、consul、etcd3  SEATA_GROUP
		  type = "nacos"

		  nacos {
			serverAddr = "127.0.0.1:8848"
			namespace = "default"
			group = ""
			username = "nacos"
			password = "nacos"
		  }
		}
		```
    4. 上传配置到nacos: 在conf目录下打开git bash here 执行nacos-config.sh
         输入命令 sh nacos-config.sh -h 127.0.0.1
		 ![seata-config](https://oscimg.oschina.net/oscnet/up-a378b7a609d451d9b38c64a910a09e6ae58.png "seata-config")
5.  创建client服务
	1. 	创建seat_order库，用来存储订单信息
	2.	创建seat_storage库，用来存储库存信息
	3. 	创建seat_account，用来存储账户信息
	4. 三个库添加日志回滚表

完整数据库示意图

![](https://oscimg.oschina.net/oscnet/up-44a3d4d55fe430b6d615979aeae29d20555.png)

	5. 	创建seata-order-service（订单服务），seata-storage-service（库存服务），seata-account-service（账户服务）
> 配置内容大同小异以order服务的配置为例,修改bootstrap.yml文件
 ```yaml
server:
  port: 8180

spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: "nacos"
        password: "nacos"
      config:
        server-addr: 127.0.0.1:8848
        username: "nacos"
        password: "nacos"
mybatis:
  mapperLocations: classpath:mapper/*.xml
 ```
>修改application.yml文件
```yaml
seata:
  enabled: true
  application-id: seata-order-service
  tx-service-group: my_test_tx_group #tx-service-group需要与conf目录下file.conf文件下名称一致
  config:
    type: nacos
    nacos:
      namespace:
      serverAddr: 127.0.0.1:8848
      group: SEATA_GROUP
      username: "nacos"
      password: "nacos"
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace:
      username: "nacos"
      password: "nacos"
spring:
  datasource:
    #driver-class-name: com.mysql.jdbc.Driver
    username: root
    password: 'Root_123456'
    url: jdbc:mysql://192.168.240.129:3306/seat_order?characterEncoding=utf-8&useSSL=false
```
>添加主方法
```java
@RestController
@RequestMapping(value = "/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @GetMapping("/create")
    @GlobalTransactional
    public CommonResult create(Order order) {
        orderService.create(order);
        return new CommonResult("订单创建成功!", 200);
    }
}
```
6.  启动nacos，seata，seata-order-service，seata-storage-service，seata-account-service
```java
2020-09-21 16:17:08.307  INFO 29768 --- [           main] i.s.s.a.GlobalTransactionScanner         : Initializing Global Transaction Clients ... 
2020-09-21 16:17:08.394  INFO 29768 --- [           main] i.s.core.rpc.netty.NettyClientBootstrap  : NettyClientBootstrap has started
2020-09-21 16:17:08.394  INFO 29768 --- [           main] i.s.s.a.GlobalTransactionScanner         : Transaction Manager Client is initialized. applicationId[seata-order-service] txServiceGroup[my_test_tx_group]
2020-09-21 16:17:08.403  INFO 29768 --- [           main] io.seata.rm.datasource.AsyncWorker       : Async Commit Buffer Limit: 10000
2020-09-21 16:17:08.403  INFO 29768 --- [           main] i.s.rm.datasource.xa.ResourceManagerXA   : ResourceManagerXA init ...
2020-09-21 16:17:08.408  INFO 29768 --- [           main] i.s.core.rpc.netty.NettyClientBootstrap  : NettyClientBootstrap has started
2020-09-21 16:17:08.408  INFO 29768 --- [           main] i.s.s.a.GlobalTransactionScanner         : Resource Manager is initialized. applicationId[seata-order-service] txServiceGroup[my_test_tx_group]
2020-09-21 16:17:08.408  INFO 29768 --- [           main] i.s.s.a.GlobalTransactionScanner         : Global Transaction Clients are initialized. 
```
![](https://oscimg.oschina.net/oscnet/up-f1fe7b1994bbf355a540eab47e2ba191e32.png)
3.  测试分布式事务

------------
库的初始状态  
![](https://oscimg.oschina.net/oscnet/up-6163777a8d0accd3ea4e74ca7c660da35b3.png)  
访问 http://localhost:8180/order/create?userId=1&productId=1&count=10&money=100
```java
2020-09-21 16:27:22.497  INFO 29768 --- [nio-8180-exec-9] i.seata.tm.api.DefaultGlobalTransaction  : Begin new global transaction [192.168.240.1:8091:51345170083352576]
2020-09-21 16:27:22.497  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->下单开始
2020-09-21 16:27:22.508  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中扣减库存开始
2020-09-21 16:27:22.530  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中扣减库存结束
2020-09-21 16:27:22.530  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中扣减余额开始
2020-09-21 16:27:22.543  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中扣减余额结束
2020-09-21 16:27:22.543  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中修改订单状态开始
2020-09-21 16:27:22.550  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中修改订单状态结束
2020-09-21 16:27:22.550  INFO 29768 --- [nio-8180-exec-9] c.m.cloud.service.impl.OrderServiceImpl  : ------->下单结束
2020-09-21 16:27:22.552  INFO 29768 --- [nio-8180-exec-9] i.seata.tm.api.DefaultGlobalTransaction  : [192.168.240.1:8091:51345170083352576] commit status: Committed
2020-09-21 16:27:22.560  INFO 29768 --- [h_RMROLE_1_6_16] i.s.c.r.p.c.RmBranchCommitProcessor      : rm client handle branch commit process:xid=192.168.240.1:8091:51345170083352576,branchId=51345170112712704,branchType=AT,resourceId=jdbc:mysql://192.168.240.129:3306/seat_order,applicationData=null
2020-09-21 16:27:22.560  INFO 29768 --- [h_RMROLE_1_6_16] io.seata.rm.AbstractRMHandler            : Branch committing: 192.168.240.1:8091:51345170083352576 51345170112712704 jdbc:mysql://192.168.240.129:3306/seat_order null
2020-09-21 16:27:22.560  INFO 29768 --- [h_RMROLE_1_6_16] io.seata.rm.AbstractRMHandler            : Branch commit result: PhaseTwo_Committed
2020-09-21 16:27:22.622  INFO 29768 --- [h_RMROLE_1_7_16] i.s.c.r.p.c.RmBranchCommitProcessor      : rm client handle branch commit process:xid=192.168.240.1:8091:51345170083352576,branchId=51345170297262080,branchType=AT,resourceId=jdbc:mysql://192.168.240.129:3306/seat_order,applicationData=null
2020-09-21 16:27:22.622  INFO 29768 --- [h_RMROLE_1_7_16] io.seata.rm.AbstractRMHandler            : Branch committing: 192.168.240.1:8091:51345170083352576 51345170297262080 jdbc:mysql://192.168.240.129:3306/seat_order null
2020-09-21 16:27:22.622  INFO 29768 --- [h_RMROLE_1_7_16] io.seata.rm.AbstractRMHandler            : Branch commit result: PhaseTwo_Committed
```
------------
数据库状态  
![](https://oscimg.oschina.net/oscnet/up-218f74e932c99a5d6cc04a49657e42d9681.png)  
模拟异常
```java
    /**
     * 扣减账户余额
     */
    @Override
    public void decrease(Long userId, BigDecimal money) {
        LOGGER.info("------->account-service中扣减账户余额开始");
        //模拟超时异常，全局事务回滚
        try {
            Thread.sleep(30*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        accountDao.decrease(userId,money);
        LOGGER.info("------->account-service中扣减账户余额结束");
    }
```
会发现数据库没有变化，并且控制台打印日志如下
```java

2020-09-21 16:29:53.921  INFO 29768 --- [nio-8180-exec-3] i.seata.tm.api.DefaultGlobalTransaction  : Begin new global transaction [192.168.240.1:8091:51345805197447168]
2020-09-21 16:29:53.921  INFO 29768 --- [nio-8180-exec-3] c.m.cloud.service.impl.OrderServiceImpl  : ------->下单开始
2020-09-21 16:29:53.931  INFO 29768 --- [nio-8180-exec-3] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中扣减库存开始
2020-09-21 16:29:53.957  INFO 29768 --- [nio-8180-exec-3] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中扣减库存结束
2020-09-21 16:29:53.957  INFO 29768 --- [nio-8180-exec-3] c.m.cloud.service.impl.OrderServiceImpl  : ------->order-service中扣减余额开始
2020-09-21 16:29:56.099  INFO 29768 --- [h_RMROLE_1_8_16] i.s.c.r.p.c.RmBranchRollbackProcessor    : rm handle branch rollback process:xid=192.168.240.1:8091:51345805197447168,branchId=51345805231001600,branchType=AT,resourceId=jdbc:mysql://192.168.240.129:3306/seat_order,applicationData=null
2020-09-21 16:29:56.100  INFO 29768 --- [h_RMROLE_1_8_16] io.seata.rm.AbstractRMHandler            : Branch Rollbacking: 192.168.240.1:8091:51345805197447168 51345805231001600 jdbc:mysql://192.168.240.129:3306/seat_order
2020-09-21 16:29:56.142  INFO 29768 --- [h_RMROLE_1_8_16] i.s.r.d.undo.AbstractUndoLogManager      : xid 192.168.240.1:8091:51345805197447168 branch 51345805231001600, undo_log deleted with GlobalFinished
2020-09-21 16:29:56.143  INFO 29768 --- [h_RMROLE_1_8_16] io.seata.rm.AbstractRMHandler            : Branch Rollbacked result: PhaseTwo_Rollbacked
2020-09-21 16:29:56.147  INFO 29768 --- [nio-8180-exec-3] i.seata.tm.api.DefaultGlobalTransaction  : [192.168.240.1:8091:51345805197447168] rollback status: Rollbacked
2020-09-21 16:29:56.167 ERROR 29768 --- [nio-8180-exec-3] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is feign.RetryableException: Read timed out executing GET http://seata-account-service/account/decrease?userId=1&money=100] with root cause

java.net.SocketTimeoutException: Read timed out
...
```
https://gitee.com/shiliang_feng/seate-demo/tree/master 源码地址  
参考文档  
https://juejin.im/post/6844904001528397831  
https://www.pianshen.com/article/32571721859/  
官方文档
https://github.com/seata/seata