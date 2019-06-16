package com.zlfund.staticcrawler.util;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.UrlUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * desc: okd自动登录
 *
 * @author 王彦锋
 * @date 2019/6/16 9:42
 */
public class OkdLogin {
    private static final Logger log = Logger.getLogger(OkdLogin.class);

    private static WebClient webClient = new WebClient(BrowserVersion.CHROME);
    private static HashMap<String, String> allCookieMap = new HashMap<>();

    private static final String OKD_DOMAIN = "paas.pazh.com";
    private static final String SCHEME = "https://";

    static {
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setTimeout(360000);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
    }

    private static void firstStep(String targetUrl) throws IOException {
        URL url = new URL(targetUrl);
        WebRequest requestSettings = new WebRequest(url, HttpMethod.GET);
        setCookie(requestSettings);
        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage page = webClient.getPage(requestSettings);

        WebResponse response = page.getWebResponse();
        List<NameValuePair> pairs = response.getResponseHeaders();

        HashMap<String, String> cookieMap = genCookieMap(pairs);
        allCookieMap.putAll(cookieMap);
    }

    /**
     * 给requestSettings设置cookie，使用默认的所有已知cookie进行构造
     *
     * @param requestSettings
     */
    private static void setCookie(WebRequest requestSettings) {
        StringBuilder cookies = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : allCookieMap.entrySet()) {
            if (!first) {
                cookies.append("; ");
            }
            cookies.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        requestSettings.setAdditionalHeader("Cookie", cookies.toString());
    }

    private static String secondStep(String targetUrl) throws IOException {
        firstStep(targetUrl);
        String loginUrl = targetUrl + "/auth/login";
        URL url = new URL(loginUrl);
        WebRequest requestSettings = new WebRequest(url, HttpMethod.GET);
        setCookie(requestSettings);

        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage page = webClient.getPage(requestSettings);
        List<NameValuePair> pairs = page.getWebResponse().getResponseHeaders();
        String gurl = UrlUtils.decode(page.getWebResponse().getWebRequest().getUrl().toString());
        int start = gurl.indexOf("?") + 1;
        String then = gurl.substring(start).substring("then=".length());

        HashMap<String, String> secondCookieMap = genCookieMap(pairs);
        allCookieMap.putAll(secondCookieMap);
        return then;
    }

    private static void thirdStep(String targetUrl) throws IOException {
        String then = secondStep(targetUrl);

        String okdLoginUrl = SCHEME + OKD_DOMAIN + "/login";
        URL url = new URL(okdLoginUrl);
        WebRequest requestSettings = new WebRequest(url, HttpMethod.POST);
        setCookie(requestSettings);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("then", then));
        params.add(new NameValuePair("username", "admin"));
        params.add(new NameValuePair("password", "admin"));
        params.add(new NameValuePair("csrf", allCookieMap.get("csrf")));
        requestSettings.setRequestParameters(params);
        //httpunit不能很好处理post后响应302，手动处理。
        webClient.getOptions().setRedirectEnabled(false);
        Page page = null;
        try {
            page = webClient.getPage(requestSettings);
        } catch (FailingHttpStatusCodeException e) {
            WebResponse response = e.getResponse();
            String location = response.getResponseHeaderValue("location");
            if (!location.startsWith(SCHEME)) {
                location = SCHEME + OKD_DOMAIN + location;
            }
            HashMap<String, String> thirdCookieMap = genCookieMap(response.getResponseHeaders());
            allCookieMap.putAll(thirdCookieMap);
            thirdStepCascade(location);
        }
    }

    private static void thirdStepCascade(String targetUrl) throws IOException {
        URL url = new URL(targetUrl);
        WebRequest requestSettings = new WebRequest(url, HttpMethod.GET);

        setCookie(requestSettings);
        webClient.getOptions().setRedirectEnabled(false);
        HtmlPage page = null;
        try {
            page = webClient.getPage(requestSettings);
            WebResponse response = page.getWebResponse();
            HashMap<String, String> cookieMap = genCookieMap(response.getResponseHeaders());
            allCookieMap.putAll(cookieMap);
        } catch (FailingHttpStatusCodeException e) {
            WebResponse response = e.getResponse();
            String location = response.getResponseHeaderValue("location");
            if (!location.startsWith(SCHEME)) {
                location = SCHEME + OKD_DOMAIN + location;
            }
            HashMap<String, String> cookieMap = genCookieMap(response.getResponseHeaders());
            allCookieMap.putAll(cookieMap);

            String stateColumn = "state=";
            String splitStr = "&";
            for (String v : location.split(splitStr)) {
                if (v.startsWith(stateColumn)) {
                    String stateToken = v.substring(stateColumn.length());
                    allCookieMap.put("state-token", stateToken);
                    break;
                }
            }
            thirdStepCascade(location);
        }
    }

    private static HashMap<String, String> genCookieMap(List<NameValuePair> pairs) {
        HashMap<String, String> cookieMap = new HashMap<>(16);
        for (NameValuePair pair : pairs) {
            String keyp = pair.getName();
            if ("Set-Cookie".equals(keyp)) {
                String[] values = pair.getValue().split("; ");
                for (String value : values) {
                    if (value.toUpperCase().startsWith("PATH") || value.startsWith("HttpOnly") || value.startsWith("Secure")) {
                        continue;
                    }
                    log.debug(keyp + "=" + value);
                    int keyStart = value.indexOf("=");
                    if (keyStart > 0) {
                        String cookieKey = value.substring(0, keyStart);
                        String cookieValue = value.substring(keyStart + 1);
                        log.debug(cookieKey + "=" + cookieValue);
                        cookieMap.put(cookieKey, cookieValue);
                    }
                }
            }
        }
        return cookieMap;
    }

    private static void login(String subModelUrl) throws IOException {
        thirdStep(subModelUrl);
        System.out.println(allCookieMap.toString());
        //登录成功后刷新cookie
        firstStep(subModelUrl);

        //allCookieMap就是后续要使用的对象，从中获取指定的cookie。
        System.out.println(allCookieMap.toString());
    }

    public static void main(String[] args) {
        try {
            String clusterConsoleUrl = SCHEME + "console.apps.paas.pazh.com";
            login(clusterConsoleUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
