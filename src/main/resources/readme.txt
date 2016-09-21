说在前面：
	1、StaticCrawler使用了htmlunit，需要java7环境，所以对应的服务器要安装java7运行环境；
	2、nginx接收到搜索引擎的ajax页面请求，先从redis_key获取；
	3、redis中不存在时，透传给Crawler（url和redis_key）,Crawler解析出目的url并访问，把访问结果存入redis；redis_expired决定了缓存有效期；
	4、由于Crawler响应相对较慢，建议配置定时任务（curl + ua），定时刷新redis；
	


一、在debian6安装java 7和tomcat6
	1、安装java 7 jdk
		echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee /etc/apt/sources.list.d/webupd8team-java.list
		echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list
		apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
		apt-get update
		apt-get install oracle-java7-installer
	
	2、 配置java 7 为默认的java环境
	    apt-get install oracle-java7-set-default
	
	3、查看java -version
		root@debian:/etc/nginx/sites-enabled# java -version
		java version "1.7.0_80"
		Java(TM) SE Runtime Environment (build 1.7.0_80-b15)
		Java HotSpot(TM) 64-Bit Server VM (build 24.80-b11, mixed mode)
	
	4、安装tomcat6
	    apt-get install tomcat6
	
	5、配置tomcat6，修改java路径 JDK_DIRS（vi /etc/init.d/tomcat6）
	    JDK_DIRS="/usr/lib/jvm/java-7-oracle /usr/lib/jvm/java-6-openjdk /usr/lib/jvm/java-6-sun /usr/lib/jvm/java-1.5.0-sun /usr/lib/j2sdk1.5-sun /usr/lib/j2sdk1.5-ibm"    
	6、 配置端口为7070，防止冲突（vi /var/lib/tomcat6/conf/server.xml）
	           <Connector port="7070" protocol="HTTP/1.1" 
	               connectionTimeout="20000" 
	               URIEncoding="UTF-8"
	               redirectPort="8443" />
	       
	7、service tomcat restart，查看是否为java7启动
		root@debian:/var/lib/tomcat6/webapps# ps -ef|grep java
		tomcat6   2733     1  1 15:59 ?        00:00:37 /usr/lib/jvm/java-7-oracle/bin/java ... 	
二、部署StaticCrawler工程到对应服务器
    1、删除服务器的ROOT目录内容 rm /var/lib/tomcat6/webapps/ROOT/ -fr
    2、拷贝   StaticCrawler工程内容到服务器/var/lib/tomcat6/webapps/ROOT/ 目录
    3、重启动tomcat，观察日志是否正常。
    
三、nginx部署，选择使用crawler抓取并缓存举例（配置nginx的相关文件）
	upstream crawler {
	    server 192.168.0.36:7070 ;
	    server 192.168.0.6:7070  ;
	}

    location ~ ^/fund/daogou/ {
        include             /etc/nginx/proxy.conf;
        default_type        text/html;
        if ($http_user_agent ~* "spider|Spider|Baiduspider|Sogou web spider|360Spider|HaosouSpider|Googlebot|Googlebot-Mobile|Googlebot-Image|Mediapartners-Google|Adsbot-Google|Feedfetcher-Google|ysearch|AhrefsBot|bingbot|EasouSpider|ChinasoSpider")
        {
            set $redis_key      crawler_$host$request_uri;
            set $gurl $scheme://$host:$server_port$request_uri;

            redis_pass          pagecache_redis;
            error_page          404 502 = /crawler/;
            break;
        }

        set $redis_key      $host$uri$is_args$args;
        redis_pass          pagecache_redis;
        error_page          404 502 = /update$request_uri;
    }

    location /crawler/ {
        include             /etc/nginx/proxy.conf;
        proxy_set_header    gurl $gurl;
        proxy_set_header    redis_key $redis_key;
        #设置要缓存的位置，不设置就是192.168.0.131
        #proxy_set_header    redis_host 192.168.0.131;
        #proxy_set_header    redis_port 6379;
        #设置要缓存的有效期，不设置默认为7200秒
        #proxy_set_header    redis_expired 7200;

        proxy_pass          http://crawler/crawl;
        break;
    }

    
    