package com.zlfund.staticcrawler.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.zlfund.staticcrawler.CrawlerContants;

public class CrawlerUtils {
    private static final Logger log = Logger.getLogger(CrawlerUtils.class);

    /**
     * 抓取对应url的document内容
     * @param url
     * @return
     * @throws IOException 
     * @throws MalformedURLException 
     * @throws FailingHttpStatusCodeException 
     * @throws Exception
     */
    public static String crawle(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        if (StringUtils.isBlank(url)) {
            throw new RuntimeException("para invald:url=" + url);
        }
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");

        WebClient webClient = new WebClient(BrowserVersion.CHROME);

        try {
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setTimeout(360000);
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getCurrentWindow().getEnclosedPage();
            HtmlPage page = webClient.getPage(url);

            String content = page.getDocumentElement().asXml();
            if (content.startsWith("<?xml")) {
                content = StringUtils.substringAfter(content, "\r\n");
            }
            content = StringUtils.remove(content, "//<![CDATA[");
            content = StringUtils.remove(content, "//]]>");
            return content;
        } finally {
            webClient.close();
        }
    }

    /**
     * 保存到redis
     * @param redis_host host of redis
     * @param redis_port  port of redis
     * @param k k of redis
     * @param v v of redis
     * @param expired 过期时间，单位为秒
     */
    public static void save2Redis(String redis_host, String redis_port, String k, String v, String expired) {
        if (StringUtils.isBlank(k)) {
            throw new RuntimeException("para invald: k=" + k + "  v=" + v);
        }
        String t_redis_host = StringUtils.isBlank(redis_host) ? CrawlerContants.DEFAULT_REDIS_HOST : redis_host;
        int t_redis_port = StringUtils.isBlank(redis_port) ? CrawlerContants.DEFAULT_REDIS_PORT : Integer
                .valueOf(redis_port);
        int t_expired = StringUtils.isBlank(expired) ? CrawlerContants.DEFAULT_REDIS_EXPIRED : Integer.valueOf(expired);

        Jedis jedis = new Jedis(t_redis_host, t_redis_port, CrawlerContants.DEFAULT_REDIS_TIMEOUT);
        try {
            jedis.set(k, v);
            String sv = jedis.get(k);
            jedis.expire(k, t_expired);
            if (!v.equals(sv)) {
                throw new RuntimeException("redis value not equals set value with k:" + k);
            }
        } finally {
            jedis.close();
        }
    }

    private static String localIPs = null;

    public static String getLocalIPs() {
        if (localIPs != null) {
            return localIPs;
        } else {

            StringBuilder ipv4s = new StringBuilder();
            StringBuilder ipv6s = new StringBuilder();

            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface current = interfaces.nextElement();
                    if (!current.isUp() || current.isLoopback() || current.isVirtual())
                        continue;
                    Enumeration<InetAddress> addresses = current.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress current_addr = addresses.nextElement();
                        if (current_addr.isLoopbackAddress())
                            continue;
                        if (current_addr instanceof Inet4Address) {
                            ipv4s.append(current_addr.getHostAddress()).append(";");
                        } else if (current_addr instanceof Inet6Address) {
                            ipv6s.append(current_addr.getHostAddress()).append(";");
                        }
                    }
                }
            } catch(Exception e) {
                log.error("getLocalIP error", e);
                MailHelper.sendEmail("getLocalIP error", e);
            }

            if (ipv4s.length() > 0) {
                localIPs = ipv4s.toString();
            } else {
                localIPs = ipv6s.toString();
            }
            return localIPs;
        }
    }
}
