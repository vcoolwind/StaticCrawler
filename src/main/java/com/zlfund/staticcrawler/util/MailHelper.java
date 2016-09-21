package com.zlfund.staticcrawler.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;

import com.zlfund.staticcrawler.CrawlerContants;

public class MailHelper {
    private static BlockingQueue<SimpleEmail> queue = new LinkedBlockingQueue<SimpleEmail>(10000);

    private static final Logger log = Logger.getLogger(MailHelper.class);

    private static boolean isSendStart = false;

    public static void sendEmail(Throwable e) {
        sendEmail(e.getMessage(), getStackMsg(e));
    }

    public static void sendEmail(String subject) {
        sendEmail(subject, subject);
    }

    public static void sendEmail(String subject, Throwable e) {
        sendEmail(subject, getStackMsg(e));
    }

    public static void sendEmail(String subject, String contents, Throwable e) {
        String tmpContents = contents + " \r\n  exception info ---- \r\n" + getStackMsg(e);
        sendEmail(subject, tmpContents);
    }

    public static void sendEmail(String subject, String contents) {
        try {
            sendSimpleEmail(CrawlerContants.EMAIL_HOST, CrawlerContants.EMAIL_USERNAME, CrawlerContants.EMAIL_PASSWORD, subject,
                    contents, CrawlerContants.EMAIL_TOADDRES, CrawlerContants.EMAIL_FROMADDRES);
        } catch(Exception e) {
            log.error("发送邮件失败", e);
        }
    }

    private static void sendSimpleEmail(String host, String username, String password, String subject, String contents,
            String toEmailAddress, String fromEmailAddress) throws EmailException {
        SimpleEmail email = new SimpleEmail();
        email.setHostName(host);
        email.setAuthentication(username, password);
        String[] tos = toEmailAddress.split(";");
        for (String to : tos) {
            // log.info(to);
            if (StringUtils.isNotBlank(to)) {
                email.addTo(to);
            }
        }

        email.setFrom(fromEmailAddress, "StaticCrawler告警");
        email.setSubject(subject + "(from:" + CrawlerUtils.getLocalIPs() + ")");
        email.setContent(contents, "text/plain;charset=GBK");
        try {
            if (!queue.offer(email)) {
                log.warn("邮件添加失败 -- " + subject + ":" + contents);
            }
        } catch(Exception e) {
            log.warn(e);
        }

        if (!isSendStart) {
            sendStart();
        }
    }

    private static synchronized void sendStart() {
        if (!isSendStart) {
            new Thread() {
                @Override
                public void run() {
                    SimpleEmail mail = null;
                    while (true) {
                        try {
                            mail = queue.take();
                            mail.send();
                        } catch(Exception e) {
                            log.warn("邮件发送失败" + mail.getHostName(), e);
                        }
                    }
                }
            }.start();
            isSendStart = true;
        }
    }

    private static String getStackMsg(Throwable e) {

        StringBuffer sb = new StringBuffer();
        sb.append(e.getMessage() + "\r\n");
        StackTraceElement[] stackArray = e.getStackTrace();
        for (StackTraceElement element : stackArray) {
            sb.append(element.toString() + "\r\n");
        }
        return sb.toString();
    }

}
