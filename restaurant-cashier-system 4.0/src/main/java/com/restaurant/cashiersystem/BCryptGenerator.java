package com.restaurant.cashiersystem;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码加密工具
 * 用于生成数据库可存储的加密密码
 */
public class BCryptGenerator {

    // 官方推荐加密器
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static void main(String[] args) {
        // 你要加密的明文密码
        String rawPassword = "123456";

        // 加密
        String encodedPassword = encoder.encode(rawPassword);

        // 输出结果（直接复制这个存数据库）
        System.out.println("明文密码：" + rawPassword);
        System.out.println("BCrypt加密后：" + encodedPassword);
    }
}