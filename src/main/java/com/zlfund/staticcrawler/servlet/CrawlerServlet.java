package com.zlfund.staticcrawler.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.zlfund.staticcrawler.util.CrawlerUtils;
import com.zlfund.staticcrawler.util.MailHelper;

public class CrawlerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(CrawlerServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String message = null;
        String gurl = get(req, "gurl");
        String redis_key = get(req, "redis_key");
        String redis_host = get(req, "redis_host");
        String redis_port = get(req, "redis_port");
        String redis_expired = get(req, "redis_expired");

        String paras = "[gurl=" + gurl + "] [redis_key=" + redis_key + "] [redis_host=" + redis_host + "] [redis_port="
                + redis_port + "] [redis_expired=" + redis_expired + "]";
        log.info("paras:" + paras);

        if (StringUtils.isBlank(gurl)) {
            message = "[CrawlerServlet]gurl is blank[" + paras + "]";
            log.error(message);
            MailHelper.sendEmail(message);
            resp.sendError(404);
            return;
        }

        try {
            new URL(gurl);
        } catch(MalformedURLException e) {
            message = "[CrawlerServlet]gurl is invalid[" + paras + "]";
            log.error(message);
            MailHelper.sendEmail(message);
            resp.sendError(404);
            return;
        }
        String respContents = null;
        try {
            respContents = CrawlerUtils.crawle(gurl);
        } catch(FailingHttpStatusCodeException fsc) {
            message = "[CrawlerUtils.crawle]crawle fail[" + paras + "]";
            log.error(message, fsc);
            MailHelper.sendEmail(message, fsc);
            resp.sendError(fsc.getStatusCode());
            return;
        } catch(Exception e) {
            message = "[CrawlerUtils.crawle]crawle fail[" + paras + "]";
            log.error(message, e);
            MailHelper.sendEmail(message, e);
            resp.sendError(500);
            return;
        }

        if (StringUtils.isBlank(respContents)) {
            message = "[CrawlerUtils.crawle]crawle respContents is blank[" + paras + "]";
            log.error(message);
            MailHelper.sendEmail(message);
            resp.sendError(502);
            return;
        }

        if (StringUtils.isBlank(redis_key)) {
            message = "redis_key is blank, do not write redis[" + paras + "]";
            log.error(message);
            MailHelper.sendEmail(message, respContents);
        } else {
            try {
                CrawlerUtils.save2Redis(redis_host, redis_port, redis_key, respContents, redis_expired);
                message = "save2Redis ok! " + paras + "\r\n";
                log.info(message);
            } catch(Exception e) {
                message = "save2Redis fail! " + paras + "\r\n" + respContents;
                log.error(message, e);
                MailHelper.sendEmail(message = "save2Redis fail! " + paras, e);
            }
        }

        writeContent(resp, respContents);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    protected void writeContent(HttpServletResponse resp, String content) throws IOException {
        // resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");
        if (resp != null && StringUtils.isNotBlank(content)) {
            try {
                byte[] outBytes = content.getBytes("UTF-8");
                resp.getOutputStream().write(outBytes);
            } catch(UnsupportedEncodingException e) {
                MailHelper.sendEmail(e);
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    private String get(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        if (StringUtils.isBlank(v)) {
            v = req.getParameter(name);
        }
        return v;
    }
}
