package com.zlfund.staticcrawler;

public class CrawlerContants {
    public static String EMAIL_HOST = "mail.jjmmw.com";

    public static String EMAIL_USERNAME = "service";

    public static String EMAIL_PASSWORD = "4006788887jjmmw";

    public static String EMAIL_FROMADDRES = "service@jjmmw.com";

    public static String EMAIL_TOADDRES = "wangyf@jjmmw.com";

    // 10S读写超时
    public static int DEFAULT_REDIS_TIMEOUT = 10 * 1000;

    public static String DEFAULT_REDIS_HOST = "192.168.0.131";

    public static int DEFAULT_REDIS_PORT = 6379;

    /**
     * 默认2个小时超时
     */
    public static int DEFAULT_REDIS_EXPIRED = 2 * 3600;

}
