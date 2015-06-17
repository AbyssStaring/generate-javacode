DROP TABLE IF EXISTS `config_system`;
CREATE TABLE `config_system` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) NOT NULL COMMENT '系统名字',
  `nameDesc` varchar(100) NOT NULL COMMENT '系统描述',
  `createDate` datetime NOT NULL COMMENT '创建时间',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_system_name` (`name`)
) ENGINE=InnoDB   DEFAULT CHARSET=utf8 COMMENT = "系统,标识接入的业务系统";


drop TABLE if EXISTS `config_data`;
create TABLE `config_data`(
  `id` bigint(20) not NULL AUTO_INCREMENT,
  `systemName` varchar(20) NOT null COMMENT '系统名字',
  `dataKey` varchar(20) NOT NULL COMMENT '数据唯一标识',
  `dataDesc` varchar(100) NOT NULL COMMENT '数据描述',
  `data` text NOT NULL COMMENT '序列化数据',
  `createDate` datetime NOT NULL COMMENT '创建时间',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  PRIMARY key (`id`),
  unique key `key_systemname_data` (`systemName`,`dataKey`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT="数据配置，根据系统和标识区分唯一一条数据";
