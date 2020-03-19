/*
Navicat MySQL Data Transfer

Source Server         : dev
Source Server Version : 80019
Source Host           : localhost:3306
Source Database       : sorting

Target Server Type    : MYSQL
Target Server Version : 80019
File Encoding         : 65001

Date: 2020-03-19 19:24:28
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for coded_dest
-- ----------------------------
DROP TABLE IF EXISTS `coded_dest`;
CREATE TABLE `coded_dest` (
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '编码',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '地址',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of coded_dest
-- ----------------------------

-- ----------------------------
-- Table structure for item
-- ----------------------------
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
  `code` char(10) NOT NULL,
  `dest_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标地址编码',
  `create_at` datetime NOT NULL,
  `pack_time` datetime DEFAULT NULL COMMENT '打包时间，空值即未打包',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快件';

-- ----------------------------
-- Records of item
-- ----------------------------
INSERT INTO `item` VALUES ('123', '123456', '2020-03-19 16:19:13', '2020-03-18 21:01:41');
INSERT INTO `item` VALUES ('124', '123456', '2020-03-19 16:29:13', '2020-03-18 21:03:41');

-- ----------------------------
-- Table structure for package
-- ----------------------------
DROP TABLE IF EXISTS `package`;
CREATE TABLE `package` (
  `code` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '编号',
  `dest_code` varchar(30) NOT NULL COMMENT '目标地址编码',
  `create_at` datetime NOT NULL COMMENT '创建时间',
  `operator` int NOT NULL COMMENT '操作者（用户id）',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='包裹';

-- ----------------------------
-- Records of package
-- ----------------------------
INSERT INTO `package` VALUES ('12345', '666', '2020-03-19 11:29:09', '12');
INSERT INTO `package` VALUES ('123456', '666', '2020-03-19 11:29:55', '12');
INSERT INTO `package` VALUES ('1234567890', '123456', '2020-03-18 12:47:16', '1');
INSERT INTO `package` VALUES ('1234567891', '123456', '2020-03-18 12:47:17', '1');
INSERT INTO `package` VALUES ('1234567892', '123456', '2020-03-18 12:47:18', '1');
INSERT INTO `package` VALUES ('1234567893', '123456', '2020-03-18 12:47:19', '1');
INSERT INTO `package` VALUES ('1234567894', '123456', '2020-03-18 12:47:20', '1');
INSERT INTO `package` VALUES ('1234567895', '123456', '2020-03-18 12:48:20', '1');
INSERT INTO `package` VALUES ('1234567896', '123456', '2020-03-18 12:49:20', '1');
INSERT INTO `package` VALUES ('888', '888', '2020-03-19 16:03:49', '1');
INSERT INTO `package` VALUES ('888888888', '123456', '2020-03-18 21:48:20', '1');

-- ----------------------------
-- Table structure for package_deleted
-- ----------------------------
DROP TABLE IF EXISTS `package_deleted`;
CREATE TABLE `package_deleted` (
  `code` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '编号',
  `dest_code` varchar(30) NOT NULL COMMENT '目标地址编码',
  `create_at` datetime NOT NULL COMMENT '删除记录创建时间',
  `creator` int NOT NULL COMMENT '原创建者',
  `delete_at` datetime DEFAULT NULL,
  `operator` int NOT NULL COMMENT '操作者（用户id）',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='包裹删除记录';

-- ----------------------------
-- Records of package_deleted
-- ----------------------------
INSERT INTO `package_deleted` VALUES ('888', '8881', '2020-03-19 18:08:53', '1', '2020-03-19 18:50:35', '1');

-- ----------------------------
-- Table structure for package_item_rel
-- ----------------------------
DROP TABLE IF EXISTS `package_item_rel`;
CREATE TABLE `package_item_rel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `package_code` char(10) NOT NULL COMMENT '包裹编号',
  `item_code` char(10) NOT NULL COMMENT '物件编号',
  `create_at` datetime NOT NULL COMMENT '关联创建时间',
  `operator` int NOT NULL COMMENT '操作者（用户id）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='包裹物件关联';

-- ----------------------------
-- Records of package_item_rel
-- ----------------------------
INSERT INTO `package_item_rel` VALUES ('1', '1234567896', '123', '2020-03-18 21:02:26', '1');
INSERT INTO `package_item_rel` VALUES ('2', '1234567896', '124', '2020-03-18 21:02:37', '1');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(10) NOT NULL COMMENT '姓名',
  `phone` varchar(11) NOT NULL COMMENT '手机号码',
  `password` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码',
  `create_at` datetime NOT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES ('1', '张三', '18112345678', '1411678a0b9e25ee2f7c8b2f7ac92b6a74b3f9c5', '2020-03-17 12:37:18');
