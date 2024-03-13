package com.ytl.vos.gateway.sbc.util;

import java.util.Random;

/**
 * @author kf-zhanghui
 * @date 2023/7/11 14:40
 */
public class RandomStringGeneratorUtil {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final String[] PREFIXES = {
            "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
            "150", "151", "152", "153", "155", "156", "157", "158", "159",
            "186", "187", "188", "189"
    };

    /**
     * 生成随机话单ID的字符串
     * @param length
     * @return
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    /**
     * 生成随机手机号的工具类
     * @return
     */
    public static String generateRandomPhoneNumber() {
        Random random = new Random();
        String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
        StringBuilder phoneNumber = new StringBuilder(prefix);

        for (int i = 0; i < 8; i++) {
            phoneNumber.append(random.nextInt(10));
        }

        return phoneNumber.toString();
    }

    public static void main(String[] args) {
        String s = generateRandomPhoneNumber();
        System.out.println(s);
    }

}
