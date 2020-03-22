SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for coded_address
-- ----------------------------
DROP TABLE IF EXISTS `coded_address`;
CREATE TABLE `coded_address` (
  `code` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '编码',
  `address` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '地址',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for item
-- ----------------------------
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
  `code` char(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `dest_code` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '目标地址编码',
  `create_at` datetime NOT NULL,
  `pack_time` datetime DEFAULT NULL COMMENT '打包时间，空值即未打包',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='快件';

-- ----------------------------
-- Table structure for package
-- ----------------------------
DROP TABLE IF EXISTS `package`;
CREATE TABLE `package` (
  `code` char(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '编号',
  `dest_code` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '目标地址编码',
  `create_at` datetime NOT NULL COMMENT '创建时间',
  `operator` int NOT NULL COMMENT '操作者（用户id）',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='包裹';

-- ----------------------------
-- Table structure for package_deleted
-- ----------------------------
DROP TABLE IF EXISTS `package_deleted`;
CREATE TABLE `package_deleted` (
  `code` char(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '编号',
  `dest_code` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '目标地址编码',
  `create_at` datetime NOT NULL COMMENT '删除记录创建时间',
  `creator` int NOT NULL COMMENT '原创建者',
  `delete_at` datetime DEFAULT NULL,
  `operator` int NOT NULL COMMENT '操作者（用户id）',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='包裹删除记录';

-- ----------------------------
-- Table structure for package_item_op
-- ----------------------------
DROP TABLE IF EXISTS `package_item_op`;
CREATE TABLE `package_item_op` (
  `id` int NOT NULL AUTO_INCREMENT,
  `package_code` char(10) NOT NULL COMMENT '包裹编号',
  `item_code` varchar(20) NOT NULL COMMENT '快件编号',
  `op_type` tinyint NOT NULL COMMENT '操作类型，1=增加快件，2=删除快件',
  `op_time` datetime NOT NULL COMMENT '操作时间',
  `operator` int NOT NULL COMMENT '操作者',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8 COMMENT='包裹快件操作记录';

-- ----------------------------
-- Table structure for package_item_rel
-- ----------------------------
DROP TABLE IF EXISTS `package_item_rel`;
CREATE TABLE `package_item_rel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `package_code` char(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '包裹编号',
  `item_code` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '物件编号',
  `create_at` datetime NOT NULL COMMENT '关联创建时间',
  `operator` int NOT NULL COMMENT '操作者（用户id）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `rel_unique` (`package_code`,`item_code`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8 COMMENT='包裹快件关联';

-- ----------------------------
-- Table structure for scheme
-- ----------------------------
DROP TABLE IF EXISTS `scheme`;
CREATE TABLE `scheme` (
  `id` int NOT NULL,
  `company` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '公司名称',
  `item_code_pattern` varchar(600) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '快递编号验证正则表达式',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '编号',
  `name` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '手机号码',
  `password` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '登录密码',
  `create_at` datetime NOT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COMMENT='用户';
