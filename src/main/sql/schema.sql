-- 数据库初始化脚本

-- 创建数据库
CREATE database seckill;
-- 使用数据库
use seckill;
-- 创建秒杀库存表
-- 默认MySQL有多种搜索引擎供使用，但是支持事务的只有InnoDB
CREATE TABLE seckill(
`seckill_id` BIGINT  NOT NULL AUTO_INCREMENT COMMENT '商品库存id',
`name` VARCHAR(120) NOT NULL COMMENT '商品名称',
`number` INT NOT NULL COMMENT '库存数量',
`create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`start_time` TIMESTAMP NOT NULL COMMENT '秒杀开始时间',
`end_time` TIMESTAMP NOT NULL COMMENT '秒杀结束时间',

PRIMARY KEY (seckill_id),
KEY idx_start_time(start_time),
KEY idx_end_time(end_time),
KEY idx_create_time(create_time)
)ENGINE=INNODB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀库存表';

-- 初始化数据
INSERT INTO
  seckill(name,number,start_time,end_time)
VALUES
  ('1元秒杀iPhone X',100,'2018-03-08 00:00:00','2018-03-09 00:00:00'),
  ('200元秒杀vivo X20',50,'2018-03-10 00:00:00','2018-03-11 00:00:00'),
  ('1000元秒杀华为P10',200,'2018-03-12 00:00:00','2018-03-13 00:00:00'),
  ('0元秒杀小米6',100,'2018-03-20 00:00:00','2018-03-21 00:00:00');

-- 秒杀成功明细表
-- 用户认证相关的信息
 CREATE TABLE success_killed(
`seckill_id` BIGINT NOT NULL COMMENT '秒杀商品id',
`user_phone` BIGINT NOT NULL COMMENT '用户手机号',
`state` TINYINT NOT NULL DEFAULT -1 COMMENT '状态标识：-1： 无效，0：成功 1 ： 已付款 2： 已发货',
`create_time` TIMESTAMP NOT NULL COMMENT '创建时间',
PRIMARY KEY (seckill_id,user_phone), /*联合主键*/
KEY ids_create_time(create_time)
)ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='秒杀成功明细表';

-- 连接数据库
mysql -uroot -p;