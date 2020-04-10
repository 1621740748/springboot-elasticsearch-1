## 基于SpringBoot整合ElasticSearch

### 1 [master分支](https://github.com/suxiongwei/springboot-elasticsearch)：使用Spring Data Elasticsearch实现对ES的基础操作

- 运行测试类：[ElasticsearchApplicationTests](https://github.com/suxiongwei/springboot-elasticsearch/blob/master/src/test/java/com/sxw/elasticsearch/ElasticsearchApplicationTests.java)

> 测试类记录了常用的操作API

- 官网文档地址：[Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)

### 2 [elasticsearch-jest分支](https://github.com/suxiongwei/springboot-elasticsearch/tree/elasticsearch-jest): 使用 Jest 客户端实现的电影搜索网站

#### 2.1 电影数据的获取

抓取了豆瓣的不同类型的电影作为搜索的基础数据，数据在项目的文件夹下可以找到，大家学习的时候就不用再去抓取了。电影数据路径：https://github.com/suxiongwei/springboot-elasticsearch/tree/elasticsearch-jest/src/main/resources/data

#### 2.2 启动项目

1. 启动 ElasticSearch 6.X+
2. 修改 application.yml 中的 spring.elasticsearch.jest.uris 参数
3. 启动 SpringBoot 项目
4. 运行测试类[DouBanMovieTest](https://github.com/suxiongwei/springboot-elasticsearch/blob/elasticsearch-jest/src/test/java/com/sxw/elasticsearch/crawler/DouBanMovieTest.java)的savaMovieToES方法初始化数据到es中
5. 访问 localhost:8080 开始搜索

#### 2.3 演示效果

- 通过elasticsearch-head查看导入的数据

  ![电影基础数据](https://github.com/suxiongwei/springboot-elasticsearch/blob/elasticsearch-jest/src/main/resources/static/img/es_data.jpg)

- 搜索页面

  ![搜索页面](https://github.com/suxiongwei/springboot-elasticsearch/blob/elasticsearch-jest/src/main/resources/static/img/search.jpg)

