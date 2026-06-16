/*
 Navicat Premium Dump SQL

 Source Server         : text
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : restaurant_chain

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 04/06/2026 10:31:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '所属分店ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序值',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store`(`store_id` ASC) USING BTREE,
  CONSTRAINT `fk_categories_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜品分类' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of categories
-- ----------------------------
INSERT INTO `categories` VALUES (1, 1, '精品凉菜', 1, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (2, 1, '招牌热菜', 2, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (3, 1, '时令素菜', 3, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (4, 1, '养生汤羹', 4, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (5, 1, '主食面点', 5, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (6, 2, '精品凉菜', 1, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (7, 2, '招牌热菜', 2, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (8, 2, '时令素菜', 3, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (9, 2, '养生汤羹', 4, NULL, '2026-04-29 08:17:07');
INSERT INTO `categories` VALUES (10, 2, '主食面点', 5, NULL, '2026-04-29 08:17:07');

-- ----------------------------
-- Table structure for dish_recipes
-- ----------------------------
DROP TABLE IF EXISTS `dish_recipes`;
CREATE TABLE `dish_recipes`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配方ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '门店ID',
  `dish_id` bigint UNSIGNED NOT NULL COMMENT '菜品ID',
  `material_id` bigint UNSIGNED NOT NULL COMMENT '原材料ID',
  `used_quantity` decimal(10, 2) NOT NULL COMMENT '单份用量',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_store_dish_material`(`store_id` ASC, `dish_id` ASC, `material_id` ASC) USING BTREE,
  INDEX `idx_dish`(`dish_id` ASC) USING BTREE,
  INDEX `fk_recipe_material`(`material_id` ASC) USING BTREE,
  CONSTRAINT `fk_recipe_dish` FOREIGN KEY (`dish_id`) REFERENCES `dishes` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_recipe_material` FOREIGN KEY (`material_id`) REFERENCES `materials` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_recipe_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜品配方用料表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dish_recipes
-- ----------------------------
INSERT INTO `dish_recipes` VALUES (1, 1, 1, 1, 80.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (2, 1, 1, 12, 10.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (3, 1, 1, 13, 10.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (4, 1, 1, 14, 10.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (5, 1, 1, 16, 15.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (6, 1, 2, 4, 180.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (7, 1, 2, 13, 8.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (8, 1, 2, 14, 8.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (9, 1, 3, 2, 120.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (10, 1, 3, 12, 15.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (11, 1, 3, 18, 15.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (12, 1, 4, 3, 70.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (13, 1, 4, 12, 8.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (14, 1, 4, 16, 20.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (15, 1, 5, 1, 100.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (16, 1, 5, 15, 8.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (17, 1, 5, 16, 25.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (18, 1, 6, 8, 150.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (19, 1, 6, 15, 6.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (20, 1, 7, 9, 200.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (21, 1, 7, 13, 15.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (22, 1, 8, 5, 100.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (23, 1, 8, 6, 2.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (24, 1, 9, 7, 150.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (25, 1, 9, 10, 40.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (26, 1, 10, 11, 180.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (27, 1, 10, 6, 1.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (28, 2, 12, 21, 90.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (29, 2, 12, 26, 15.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (30, 2, 13, 21, 80.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (31, 2, 13, 29, 5.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (32, 2, 14, 22, 100.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (33, 2, 14, 15, 6.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (34, 2, 15, 25, 200.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (35, 2, 16, 7, 130.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (36, 2, 16, 23, 150.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (37, 2, 17, 24, 160.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `dish_recipes` VALUES (38, 2, 17, 14, 15.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');

-- ----------------------------
-- Table structure for dishes
-- ----------------------------
DROP TABLE IF EXISTS `dishes`;
CREATE TABLE `dishes`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '所属分店ID',
  `category_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜品名称',
  `price` decimal(10, 2) NOT NULL COMMENT '单价',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '份' COMMENT '单位',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片地址',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `is_available` tinyint NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_category`(`store_id` ASC, `category_id` ASC) USING BTREE,
  INDEX `idx_store_available`(`store_id` ASC, `is_available` ASC) USING BTREE,
  INDEX `fk_dishes_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_dishes_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_dishes_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜品信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dishes
-- ----------------------------
INSERT INTO `dishes` VALUES (1, 1, 1, '凉拌牛肉', 42.00, '份', '/images/71518f5d-f77c-41d1-9f6b-1e4a50712217.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-13 10:22:27');
INSERT INTO `dishes` VALUES (2, 1, 1, '凉拌黄瓜', 14.00, '份', '/images/11775a39-ffd1-4ecb-b906-786d66fdca6a.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:11:42');
INSERT INTO `dishes` VALUES (3, 1, 2, '红烧肉', 58.00, '份', '/images/e1727072-415b-4d4a-90a3-ce755f82789c.png', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:11:48');
INSERT INTO `dishes` VALUES (4, 1, 2, '鱼香肉丝', 32.00, '份', '/images/dd923d59-f035-44e9-83e1-44f0d7fe550b.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:11:54');
INSERT INTO `dishes` VALUES (5, 1, 2, '水煮肉片', 56.00, '份', '/images/9d9adc44-9a96-43a2-af30-4fe6ac915489.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:12:01');
INSERT INTO `dishes` VALUES (6, 1, 3, '干煸豆角', 24.00, '份', '/images/eb481df4-587f-4572-b5ba-5136b645c1c9.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:12:07');
INSERT INTO `dishes` VALUES (7, 1, 3, '蒜蓉青菜', 20.00, '份', '/images/eb97b528-d414-454f-945b-e88874f830c3.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:12:13');
INSERT INTO `dishes` VALUES (8, 1, 4, '番茄蛋汤', 16.00, '份', '/images/b4221026-a9b0-49a4-92c3-06a7c61242ed.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:12:24');
INSERT INTO `dishes` VALUES (9, 1, 4, '海带排骨汤', 36.00, '份', '/images/8e910479-2e8f-4e71-b858-2193ac1a59b6.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:12:40');
INSERT INTO `dishes` VALUES (10, 1, 5, '扬州炒饭', 20.00, '份', '/images/dfeaf0b8-37f1-46ac-bf0a-0d1681d63a63.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:12:47');
INSERT INTO `dishes` VALUES (11, 1, 5, '手工水饺', 26.00, '份', '/images/8496d55b-7e09-41d1-8af7-7b62a13aa79b.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:12:54');
INSERT INTO `dishes` VALUES (12, 2, 6, '口水鸡', 38.00, '份', '/images/07050274-7a1e-4c1d-a5b4-36a6e01bd87a.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:13:01');
INSERT INTO `dishes` VALUES (13, 2, 7, '宫保鸡丁', 36.00, '份', '/images/37d1e6ac-e5d6-4efc-9121-50a749303271.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:13:09');
INSERT INTO `dishes` VALUES (14, 2, 7, '小炒黄牛肉', 68.00, '份', '/images/321e30e5-738e-476f-aa67-7387ab51c621.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:13:21');
INSERT INTO `dishes` VALUES (15, 2, 8, '清炒时蔬', 22.00, '份', '/images/23fa27ff-03b2-4b89-b02c-1cd0c6c7049d.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:13:35');
INSERT INTO `dishes` VALUES (16, 2, 9, '冬瓜排骨汤', 34.00, '份', '/images/653ffab0-7cec-4ba7-af9e-8b093f5b52a9.jpg', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:13:41');
INSERT INTO `dishes` VALUES (17, 2, 10, '葱油拌面', 18.00, '份', '/images/fe1e9a2a-554e-4eca-8cd9-78c7e65eb850.png', NULL, 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:13:48');

-- ----------------------------
-- Table structure for fixed_costs
-- ----------------------------
DROP TABLE IF EXISTS `fixed_costs`;
CREATE TABLE `fixed_costs`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '固定成本ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '门店ID',
  `cost_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型:rent/utility/wage/other',
  `amount` decimal(12, 2) NOT NULL COMMENT '金额',
  `cost_date` date NOT NULL COMMENT '发生日期',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_date`(`store_id` ASC, `cost_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '固定运营成本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fixed_costs
-- ----------------------------
INSERT INTO `fixed_costs` VALUES (1, 1, 'rent', 8000.00, '2026-05-01', '5月房租', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `fixed_costs` VALUES (2, 1, 'utility', 680.00, '2026-05-10', '水电', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `fixed_costs` VALUES (3, 1, 'wage', 12000.00, '2026-05-15', '工资', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `fixed_costs` VALUES (4, 2, 'rent', 7200.00, '2026-05-01', '房租', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `fixed_costs` VALUES (5, 2, 'utility', 560.00, '2026-05-10', '水电', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `fixed_costs` VALUES (6, 2, 'wage', 10500.00, '2026-05-15', '工资', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');

-- ----------------------------
-- Table structure for login_logs
-- ----------------------------
DROP TABLE IF EXISTS `login_logs`;
CREATE TABLE `login_logs`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '用户ID',
  `store_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '分店ID，总部为NULL',
  `login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录IP',
  `login_device` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录设备',
  `login_status` tinyint NOT NULL DEFAULT 1 COMMENT '1-成功 0-失败',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_store_time`(`store_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 178 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of login_logs
-- ----------------------------

-- ----------------------------
-- Table structure for material_consumptions
-- ----------------------------
DROP TABLE IF EXISTS `material_consumptions`;
CREATE TABLE `material_consumptions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消耗ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '门店ID',
  `order_id` bigint UNSIGNED NOT NULL COMMENT '订单ID',
  `dish_id` bigint UNSIGNED NOT NULL COMMENT '菜品ID',
  `material_id` bigint UNSIGNED NOT NULL COMMENT '原材料ID',
  `used_quantity` decimal(10, 2) NOT NULL COMMENT '消耗数量',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order`(`order_id` ASC) USING BTREE,
  INDEX `idx_store_material`(`store_id` ASC, `material_id` ASC) USING BTREE,
  CONSTRAINT `fk_consume_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_consume_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原材料消耗记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of material_consumptions
-- ----------------------------
INSERT INTO `material_consumptions` VALUES (1, 1, 1, 5, 1, 100.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (2, 1, 1, 5, 16, 25.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (3, 1, 1, 4, 3, 70.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (4, 1, 1, 7, 9, 200.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (5, 1, 1, 8, 5, 100.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (6, 2, 2, 13, 21, 80.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (7, 2, 2, 14, 22, 100.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (8, 2, 2, 16, 7, 130.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (9, 2, 2, 17, 24, 160.00, '2026-05-28 08:13:57', NULL);
INSERT INTO `material_consumptions` VALUES (10, 1, 4, 2, 4, 180.00, '2026-06-02 08:51:31', NULL);
INSERT INTO `material_consumptions` VALUES (11, 1, 4, 2, 13, 8.00, '2026-06-02 08:51:31', NULL);
INSERT INTO `material_consumptions` VALUES (12, 1, 4, 2, 14, 8.00, '2026-06-02 08:51:31', NULL);
INSERT INTO `material_consumptions` VALUES (13, 1, 4, 3, 2, 120.00, '2026-06-02 08:51:31', NULL);
INSERT INTO `material_consumptions` VALUES (14, 1, 4, 3, 12, 15.00, '2026-06-02 08:51:31', NULL);
INSERT INTO `material_consumptions` VALUES (15, 1, 4, 3, 18, 15.00, '2026-06-02 08:51:32', NULL);

-- ----------------------------
-- Table structure for material_inventory
-- ----------------------------
DROP TABLE IF EXISTS `material_inventory`;
CREATE TABLE `material_inventory`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '门店ID',
  `material_id` bigint UNSIGNED NOT NULL COMMENT '原材料ID',
  `current_stock` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '当前库存',
  `cost_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '移动加权成本价',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_store_material`(`store_id` ASC, `material_id` ASC) USING BTREE,
  INDEX `fk_inventory_material`(`material_id` ASC) USING BTREE,
  CONSTRAINT `fk_inventory_material` FOREIGN KEY (`material_id`) REFERENCES `materials` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_inventory_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原材料实时库存表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of material_inventory
-- ----------------------------
INSERT INTO `material_inventory` VALUES (1, 1, 1, 2000.00, 42.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (2, 1, 2, 2880.00, 28.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (3, 1, 3, 3000.00, 30.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (4, 1, 4, 4820.00, 4.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (5, 1, 5, 5000.00, 5.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (6, 1, 6, 50.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (7, 1, 7, 3000.00, 35.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (8, 1, 8, 1500.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (9, 1, 9, 2000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (10, 1, 10, 1000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (11, 1, 11, 5000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (12, 1, 12, 485.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (13, 1, 13, 492.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (14, 1, 14, 492.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (15, 1, 15, 200.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (16, 1, 16, 5000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (17, 1, 17, 1000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (18, 1, 18, 1985.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (19, 1, 19, 1000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (20, 1, 20, 2000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (21, 2, 21, 3000.00, 25.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (22, 2, 22, 2000.00, 55.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (23, 2, 23, 5000.00, 3.00, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (24, 2, 24, 5000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (25, 2, 25, 2000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (26, 2, 26, 500.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (27, 2, 27, 200.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (28, 2, 28, 1000.00, NULL, '2026-05-28 08:13:57');
INSERT INTO `material_inventory` VALUES (29, 2, 29, 1000.00, NULL, '2026-05-28 08:13:57');

-- ----------------------------
-- Table structure for materials
-- ----------------------------
DROP TABLE IF EXISTS `materials`;
CREATE TABLE `materials`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '原材料ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '所属门店ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原材料名称',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'g' COMMENT '单位：g/kg/份/瓶/ml/个',
  `warning_stock` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '库存预警阈值',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store`(`store_id` ASC) USING BTREE,
  CONSTRAINT `fk_materials_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原材料信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of materials
-- ----------------------------
INSERT INTO `materials` VALUES (1, 1, '牛肉', 'g', 500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (2, 1, '五花肉', 'g', 1000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (3, 1, '猪瘦肉', 'g', 1000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (4, 1, '黄瓜', 'g', 2000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (5, 1, '番茄', 'g', 2000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (6, 1, '鸡蛋', '个', 50.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (7, 1, '排骨', 'g', 1500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (8, 1, '豆角', 'g', 1500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (9, 1, '上海青', 'g', 2000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (10, 1, '海带', 'g', 1000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (11, 1, '大米', 'g', 5000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (12, 1, '生姜', 'g', 500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (13, 1, '大蒜', 'g', 500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (14, 1, '葱', 'g', 500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (15, 1, '干辣椒', 'g', 200.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (16, 1, '食用油', 'ml', 5000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (17, 1, '盐', 'g', 1000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (18, 1, '生抽', 'ml', 2000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (19, 1, '老抽', 'ml', 1000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (20, 1, '料酒', 'ml', 2000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (21, 2, '鸡肉', 'g', 2000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (22, 2, '黄牛肉', 'g', 1500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (23, 2, '冬瓜', 'g', 2500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (24, 2, '面条', 'g', 5000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (25, 2, '时令绿叶菜', 'g', 2000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (26, 2, '花生', 'g', 500.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (27, 2, '花椒', 'g', 200.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (28, 2, '香醋', 'ml', 1000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `materials` VALUES (29, 2, '白糖', 'g', 1000.00, NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');

-- ----------------------------
-- Table structure for operation_logs
-- ----------------------------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '分店ID',
  `operator_id` bigint UNSIGNED NOT NULL COMMENT '操作人ID',
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作对象类型(order/table等)',
  `target_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作对象ID',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作动作(settle/discount/clear_table)',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '详细内容/JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_time`(`store_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_operator`(`operator_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '关键操作日志' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of operation_logs
-- ----------------------------

-- ----------------------------
-- Table structure for order_item_logs
-- ----------------------------
DROP TABLE IF EXISTS `order_item_logs`;
CREATE TABLE `order_item_logs`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `order_item_id` bigint UNSIGNED NOT NULL COMMENT '关联 order_items.id',
  `order_id` bigint UNSIGNED NOT NULL COMMENT '订单ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '分店ID',
  `dish_id` bigint UNSIGNED NOT NULL COMMENT '菜品ID',
  `action_type` enum('add','append','modify','return') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'add-首次加菜 append-追加 modify-改数量 return-退菜',
  `change_quantity` int NOT NULL COMMENT '变动数量(正加负减)',
  `return_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退菜原因',
  `operator_id` bigint UNSIGNED NOT NULL COMMENT '操作人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_order_item_id`(`order_item_id` ASC) USING BTREE,
  CONSTRAINT `fk_logs_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_logs_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜品操作流水(完整追溯)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of order_item_logs
-- ----------------------------

-- ----------------------------
-- Table structure for order_items
-- ----------------------------
DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` bigint UNSIGNED NOT NULL COMMENT '订单ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '分店ID',
  `dish_id` bigint UNSIGNED NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜品名称快照',
  `price` decimal(10, 2) NOT NULL COMMENT '下单单价快照',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '当前有效数量',
  `status` enum('active','returned') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'active' COMMENT 'active-供应中 returned-已全退',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜品备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次添加时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_dish`(`order_id` ASC, `dish_id` ASC) USING BTREE,
  INDEX `idx_order`(`order_id` ASC) USING BTREE,
  CONSTRAINT `fk_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单菜品当前明细' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of order_items
-- ----------------------------
INSERT INTO `order_items` VALUES (1, 1, 1, 5, '水煮肉片', 56.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (2, 1, 1, 4, '鱼香肉丝', 32.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (3, 1, 1, 7, '蒜蓉青菜', 20.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (4, 1, 1, 8, '番茄蛋汤', 16.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (5, 2, 2, 13, '口水鸡', 38.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (6, 2, 2, 15, '小炒黄牛肉', 68.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (7, 2, 2, 16, '清炒时蔬', 22.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (8, 2, 2, 17, '冬瓜排骨汤', 34.00, 1, 'active', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `order_items` VALUES (9, 3, 2, 17, '葱油拌面', 18.00, 1, 'active', NULL, '2026-05-11 09:57:35', '2026-05-11 09:57:35');
INSERT INTO `order_items` VALUES (10, 3, 2, 16, '冬瓜排骨汤', 34.00, 1, 'active', NULL, '2026-05-11 09:57:37', '2026-05-11 09:57:37');
INSERT INTO `order_items` VALUES (11, 3, 2, 14, '小炒黄牛肉', 68.00, 1, 'active', NULL, '2026-05-11 09:57:38', '2026-05-11 09:57:38');
INSERT INTO `order_items` VALUES (12, 4, 1, 2, '凉拌黄瓜', 14.00, 1, 'active', NULL, '2026-06-02 08:51:18', '2026-06-02 08:51:18');
INSERT INTO `order_items` VALUES (13, 4, 1, 3, '红烧肉', 58.00, 1, 'active', NULL, '2026-06-02 08:51:20', '2026-06-02 08:51:20');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务订单编号',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '分店ID',
  `table_id` bigint UNSIGNED NOT NULL COMMENT '餐桌ID',
  `table_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '冗余桌号',
  `status` enum('in_progress','settled') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'in_progress' COMMENT '进行中/已结账',
  `total_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '原始总金额',
  `discount_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '折扣/抹零金额',
  `final_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付方式(cash/card/scan)',
  `settled_at` datetime NULL DEFAULT NULL COMMENT '结账时间',
  `settled_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '收银员ID',
  `created_by` bigint UNSIGNED NOT NULL COMMENT '开单服务员ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_store_status`(`store_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_store_table`(`store_id` ASC, `table_id` ASC) USING BTREE,
  INDEX `fk_orders_table`(`table_id` ASC) USING BTREE,
  CONSTRAINT `fk_orders_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_table` FOREIGN KEY (`table_id`) REFERENCES `tables` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, 'ORD2026042901', 1, 2, 'A02', 'settled', 124.00, 0.00, 124.00, 'CASH', '2026-05-11 09:54:06', 4, 3, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `orders` VALUES (2, 'ORD2026042902', 2, 6, 'B02', 'settled', 162.00, 2.00, 160.00, 'cash', NULL, 5, 3, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `orders` VALUES (3, 'ORD20260511095728676867', 2, 6, 'B02', 'settled', 120.00, 2.00, 118.00, 'CASH', '2026-05-11 09:57:45', 5, 5, '2026-05-11 09:57:28', '2026-05-11 09:57:28');
INSERT INTO `orders` VALUES (4, 'ORD20260602085116307367', 1, 1, 'A01', 'settled', 72.00, 0.00, 72.00, 'CASH', '2026-06-02 08:51:31', 1, 1, '2026-06-02 08:51:16', '2026-06-02 08:51:16');

-- ----------------------------
-- Table structure for purchases
-- ----------------------------
DROP TABLE IF EXISTS `purchases`;
CREATE TABLE `purchases`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '采购ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '门店ID',
  `material_id` bigint UNSIGNED NOT NULL COMMENT '原材料ID',
  `quantity` decimal(10, 2) NOT NULL COMMENT '采购数量',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '采购单价',
  `total_price` decimal(10, 2) NOT NULL COMMENT '总金额',
  `supplier` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商',
  `purchase_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采购时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_material`(`store_id` ASC, `material_id` ASC) USING BTREE,
  INDEX `fk_purchases_material`(`material_id` ASC) USING BTREE,
  CONSTRAINT `fk_purchases_material` FOREIGN KEY (`material_id`) REFERENCES `materials` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_purchases_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原材料采购记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of purchases
-- ----------------------------
INSERT INTO `purchases` VALUES (1, 1, 1, 2000.00, 42.00, 84000.00, '肉批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (2, 1, 2, 3000.00, 28.00, 84000.00, '肉批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (3, 1, 3, 3000.00, 30.00, 90000.00, '肉批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (4, 1, 4, 5000.00, 4.00, 20000.00, '菜批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (5, 1, 5, 5000.00, 5.00, 25000.00, '菜批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (6, 1, 7, 3000.00, 35.00, 105000.00, '肉批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (7, 2, 21, 3000.00, 25.00, 75000.00, '禽批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (8, 2, 22, 2000.00, 55.00, 110000.00, '肉批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');
INSERT INTO `purchases` VALUES (9, 2, 23, 5000.00, 3.00, 15000.00, '菜批', '2026-05-28 08:13:57', NULL, '2026-05-28 08:13:57', '2026-05-28 08:13:57');

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码(程序鉴权用)',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `role_code`(`role_code` ASC) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of roles
-- ----------------------------
INSERT INTO `roles` VALUES (1, 'admin_global', '总部管理员', '查看全部门店数据，不可编辑', '2026-04-28 14:30:33');
INSERT INTO `roles` VALUES (2, 'admin_store', '分店管理员', '管理本店员工/菜品/餐桌/数据', '2026-04-28 14:30:33');
INSERT INTO `roles` VALUES (3, 'waiter', '服务员', '移动端开台/点餐/加菜/退菜', '2026-04-28 14:30:33');
INSERT INTO `roles` VALUES (4, 'cashier', '收银', 'PC端看单/核单/结账/清台', '2026-04-28 14:30:33');
INSERT INTO `roles` VALUES (5, 'chef', '厨师', '查看菜品制作单/确认出餐', '2026-05-28 08:21:32');
INSERT INTO `roles` VALUES (6, 'purchaser', '采购员', '原材料采购/入库管理', '2026-05-28 08:29:34');

-- ----------------------------
-- Table structure for stores
-- ----------------------------
DROP TABLE IF EXISTS `stores`;
CREATE TABLE `stores`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分店ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分店名称',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-营业 0-停用',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分店信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of stores
-- ----------------------------
INSERT INTO `stores` VALUES (1, '聚福楼中餐·城东店', '东昌路188号', '0553-2233666', 1, NULL, '2026-04-29 08:17:07', '2026-06-02 10:23:22');
INSERT INTO `stores` VALUES (2, '聚福楼中餐·城西店', '西湖大道99号', '0553-2233888', 1, NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统全局配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, 'order_prefix', 'ORD', '订单前缀', '2026-04-29 08:17:07');
INSERT INTO `sys_config` VALUES (2, 'store_name', '聚福楼中餐连锁', '店铺名称', '2026-04-29 08:17:07');
INSERT INTO `sys_config` VALUES (3, 'auto_print', '1', '自动打印小票', '2026-04-29 08:17:07');

-- ----------------------------
-- Table structure for tables
-- ----------------------------
DROP TABLE IF EXISTS `tables`;
CREATE TABLE `tables`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '餐桌ID',
  `store_id` bigint UNSIGNED NOT NULL COMMENT '所属分店ID',
  `table_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '桌号',
  `area` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区域',
  `capacity` int NULL DEFAULT 4 COMMENT '容纳人数',
  `status` enum('free','occupied') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'free' COMMENT '空闲/占用',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_store_table`(`store_id` ASC, `table_number` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_tables_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '餐桌信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tables
-- ----------------------------
INSERT INTO `tables` VALUES (1, 1, 'A01', '大厅', 4, 'free', NULL, '2026-04-29 08:17:07', '2026-06-02 09:16:01');
INSERT INTO `tables` VALUES (2, 1, 'A02', '大厅', 4, 'free', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `tables` VALUES (3, 1, 'A03', '大厅', 6, 'free', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `tables` VALUES (4, 1, '包厢1', '包厢', 8, 'free', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `tables` VALUES (5, 2, 'B01', '大厅', 4, 'free', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `tables` VALUES (6, 2, 'B02', '大厅', 2, 'free', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');
INSERT INTO `tables` VALUES (7, 2, '包厢2', '包厢', 10, 'free', NULL, '2026-04-29 08:17:07', '2026-04-29 08:17:07');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `store_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '所属分店ID，总部为NULL',
  `role_id` bigint UNSIGNED NOT NULL COMMENT '角色ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号(全局唯一)',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'bcrypt加密密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-在职 0-禁用(离职)',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '软删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_store_role`(`store_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `fk_users_role`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_users_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_users_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工账号' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, NULL, 1, 'admin', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '系统管理员', '13900000001', 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:50:02');
INSERT INTO `users` VALUES (2, 1, 2, 'store1_admin', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '张店长', '13900000002', 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:51:43');
INSERT INTO `users` VALUES (3, 1, 3, 'waiter1', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '李服务员', '13900000003', 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:52:25');
INSERT INTO `users` VALUES (4, 1, 4, 'cashier1', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '王收银', '13900000004', 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:52:33');
INSERT INTO `users` VALUES (5, 2, 2, 'store2_admin', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '刘店长', '13900000005', 1, NULL, '2026-04-29 08:17:07', '2026-05-11 09:52:39');
INSERT INTO `users` VALUES (6, 1, 5, 'chef1', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '厨师1', '13900000006', 1, NULL, '2026-05-28 08:31:10', '2026-05-28 08:33:26');
INSERT INTO `users` VALUES (7, 1, 6, 'buyer1', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '采购1', '13900000007', 1, NULL, '2026-05-28 08:31:47', '2026-05-28 08:33:32');
INSERT INTO `users` VALUES (8, 2, 5, 'chef2', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '厨师2', '13900000008', 1, NULL, '2026-05-28 08:32:17', '2026-05-28 08:33:36');
INSERT INTO `users` VALUES (9, 2, 6, 'buyer2', '$2a$10$6zkpFwDQVbQyefUruqdwGeagzbtSXVyK9JbVz37JaoktSUnoXV8yS', '采购2', '13900000009', 1, NULL, '2026-05-28 08:32:46', '2026-05-28 08:33:48');

SET FOREIGN_KEY_CHECKS = 1;
