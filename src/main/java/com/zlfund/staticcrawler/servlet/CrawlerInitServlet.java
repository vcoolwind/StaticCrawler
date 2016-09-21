package com.zlfund.staticcrawler.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.zlfund.staticcrawler.CrawlerContants;

@SuppressWarnings("serial")
public class CrawlerInitServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(CrawlerInitServlet.class);

    @Override
    public void init() throws ServletException {
        log.warn("System init start...");

        String EMAIL_HOST = this.getInitParameter("EMAIL_HOST");
        log.warn("EMAIL_HOST is " + EMAIL_HOST);
        if (StringUtils.isNotBlank(EMAIL_HOST)) {
            CrawlerContants.EMAIL_HOST = EMAIL_HOST;
        }

        String EMAIL_USERNAME = this.getInitParameter("EMAIL_USERNAME");
        log.warn("EMAIL_USERNAME is " + EMAIL_USERNAME);
        if (StringUtils.isNotBlank(EMAIL_USERNAME)) {
            CrawlerContants.EMAIL_USERNAME = EMAIL_USERNAME;
        }

        String EMAIL_PASSWORD = this.getInitParameter("EMAIL_PASSWORD");
        log.warn("EMAIL_PASSWORD is " + EMAIL_PASSWORD);
        if (StringUtils.isNotBlank(EMAIL_PASSWORD)) {
            CrawlerContants.EMAIL_PASSWORD = EMAIL_PASSWORD;
        }

        String EMAIL_TOADDRES = this.getInitParameter("EMAIL_TOADDRES");
        log.warn("EMAIL_TOADDRES is " + EMAIL_TOADDRES);
        if (StringUtils.isNotBlank(EMAIL_TOADDRES)) {
            CrawlerContants.EMAIL_TOADDRES = EMAIL_TOADDRES;
        }

        String EMAIL_FROMADDRES = this.getInitParameter("EMAIL_FROMADDRES");
        log.warn("EMAIL_FROMADDRES is " + EMAIL_FROMADDRES);
        if (StringUtils.isNotBlank(EMAIL_FROMADDRES)) {
            CrawlerContants.EMAIL_FROMADDRES = EMAIL_FROMADDRES;
        }

        String DEFAULT_REDIS_HOST = this.getInitParameter("DEFAULT_REDIS_HOST");
        log.warn("DEFAULT_REDIS_HOST is " + DEFAULT_REDIS_HOST);
        if (StringUtils.isNotBlank(DEFAULT_REDIS_HOST)) {
            CrawlerContants.DEFAULT_REDIS_HOST = DEFAULT_REDIS_HOST;
        }

        String DEFAULT_REDIS_PORT = this.getInitParameter("DEFAULT_REDIS_PORT");
        log.warn("DEFAULT_REDIS_PORT is " + DEFAULT_REDIS_PORT);
        if (StringUtils.isNotBlank(DEFAULT_REDIS_PORT)) {
            CrawlerContants.DEFAULT_REDIS_PORT = Integer.valueOf(DEFAULT_REDIS_PORT);
        }

        String DEFAULT_REDIS_EXPIRED = this.getInitParameter("DEFAULT_REDIS_EXPIRED");
        log.warn("DEFAULT_REDIS_EXPIRED is " + DEFAULT_REDIS_EXPIRED);
        if (StringUtils.isNotBlank(DEFAULT_REDIS_EXPIRED)) {
            CrawlerContants.DEFAULT_REDIS_EXPIRED = Integer.valueOf(DEFAULT_REDIS_EXPIRED);
        }

        log.warn("System init end.");
    }
}
