# StaticCrawler
- 动态页面静态化，并写入redis，便于SEO爬虫读取（ngx判断是爬虫时从缓存读取）。
- 使用htmlunit实现，从初步使用的情况看，效率稍低，不太适合生产。
- 这个项目[prerender](https://github.com/prerender/prerender)可以替代该工程。

### 使用方法：
- 下载、编译、打包
```bash
git clone git@github.com:vcoolwind/StaticCrawler.git
mvn eclipse:eclipse
mvn package
```
- 部署到java web容器
- 浏览器访问可观测相关输出
