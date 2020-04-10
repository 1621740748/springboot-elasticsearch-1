package com.sxw.elasticsearch.crawler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sxw.elasticsearch.pojo.dto.DouBanMovieDTO;
import com.sxw.elasticsearch.repository.IDouBanMovieRepository;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.indices.type.TypeExist;
import io.searchbox.strings.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 苏雄伟 [suxiongwei@kaoshixing.com]
 * @description
 * @date 2019-09-05 11:44
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class DouBanMovieTest {

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    IDouBanMovieRepository douBanMovieRepository;
    @Autowired
    private JestClient client;

    /**
     * 根据类型获取电影数据
     * https://movie.douban.com/j/chart/top_list?type=1&interval_id=100%3A90&action=&start=0&limit=200000
     * @throws IOException
     */
    @Test
    public void getMovieByType() throws Exception {
        JSONArray Jarray = getCategoryByLocalJson();
        for (Object o : Jarray) {
            JSONObject jsonObject = (JSONObject) o;
            String url = "https://movie.douban.com/j/chart/top_list?type=TYPE&interval_id=100%3A90&action=&start=START&limit=LIMIT";
            url = url.replace("TYPE", jsonObject.get("type").toString().replaceAll("\"",""))
                    .replace("START",0 + "")
                    .replace("LIMIT", 1000000 + "");

            RequestEntity requestEntity = RequestEntity.get(new URI(url)).build();
            ResponseEntity<String> responseEntity = this.restTemplate.exchange(requestEntity, String.class);
            String body = responseEntity.getBody();
            String name = jsonObject.get("type_name").toString().replaceAll("\"","") + ".json";
            File file = new File("/Users/suxiongwei/Desktop/" + name);
            if (!file.exists()){
                file.createNewFile();
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(body);
                bufferedWriter.close();
            }
        }
    }

    /**
     * 将爬取保存到本地的电影数据保存到es中
     */
    @Test
    public void savaMovieToES() throws IOException {
        File file = new File("/Users/smzdm/project/springboot-elasticsearch/src/main/resources/data");
        File[] files = file.listFiles();
        for (File file1 : files) {
            if (file1.getName().equals("movie_category.json")){
                continue;
            }
            FileReader fileReader = new FileReader(file1);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                // 一次读入一行数据
                System.out.println(line);
                stringBuilder.append(line);
            }
            List<DouBanMovieDTO> movieDTOS = JSONArray.parseArray(stringBuilder.toString(), DouBanMovieDTO.class);
            movieDTOS.forEach(movie -> douBanMovieRepository.save(movie));
        }

    }


    /**
     * 获取电影分类
     * 爬取后的数据为：[{"type":"11","type_name":"剧情"},{"type":"24","type_name":"喜剧"},{"type":"5","type_name":"动作"},{"type":"13","type_name":"爱情"},{"type":"17","type_name":"科幻"},{"type":"25","type_name":"动画"},{"type":"10","type_name":"悬疑"},{"type":"19","type_name":"惊悚"},{"type":"20","type_name":"恐怖"},{"type":"1","type_name":"纪录片"},{"type":"23","type_name":"短片"},{"type":"6","type_name":"情色"},{"type":"26","type_name":"同性"},{"type":"14","type_name":"音乐"},{"type":"7","type_name":"歌舞"},{"type":"28","type_name":"家庭"},{"type":"8","type_name":"儿童"},{"type":"2","type_name":"传记"},{"type":"4","type_name":"历史"},{"type":"22","type_name":"战争"},{"type":"3","type_name":"犯罪"},{"type":"27","type_name":"西部"},{"type":"16","type_name":"奇幻"},{"type":"15","type_name":"冒险"},{"type":"12","type_name":"灾难"},{"type":"29","type_name":"武侠"},{"type":"30","type_name":"古装"},{"type":"18","type_name":"运动"},{"type":"31","type_name":"黑色电影"}]
     * @throws IOException
     */
    public JsonArray getCategory() throws IOException {
        //1. 根据TOP250网站的URL直接解析内容，获得目标电影的信息
        //1.1 准备目标网站的URL地址
        String url = "https://movie.douban.com/chart";

        //1.2 利用jsoup直接通过URL获取目标网页的HTML文本文档
        Document document = Jsoup.connect(url).get();

        //1.3 锁定目标信息所在的位置
        Element elementById = document.getElementsByClass("types").first();
        //1.3.1 在页面中我们需要的信息有两种：该页面中的全部电影信息（25条）ID为content的DIV中，
        //包括其中每条电影的基本信息class属性为info的DIV中
        Elements elements = elementById.getElementsByTag("a");
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;
        for (Element element : elements) {
            jsonObject = new JsonObject();
            Attributes attributes = element.attributes();
            // /typerank?type_name=剧情&type=11&interval_id=100:90&action=
            String hrefValue = attributes.get("href").replace("/typerank?","");
            Map map = getUrlParams(hrefValue);
            jsonObject.addProperty("type", (String) map.get("type"));
            jsonObject.addProperty("type_name", (String) map.get("type_name"));
            jsonArray.add(jsonObject);
        }
        log.info("电影分类数据：{}",jsonArray.toString());
        return jsonArray;
    }

    /**
     * 从json中读取电影分类数据
     * @return
     */
    public JSONArray getCategoryByLocalJson() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/data/movie_category.json");
        byte[] b = new byte[1024];
        inputStream.read(b);
        inputStream.close();
        return JSONArray.parseArray(new String(b));
    }

    /**
     * 将url参数转换成map
     * @param param aa=11&bb=22&cc=33
     * @return
     */
    public static Map<String, Object> getUrlParams(String param) {
        Map<String, Object> map = new HashMap<String, Object>(0);
        if (StringUtils.isBlank(param)) {
            return map;
        }
        String[] params = param.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] p = params[i].split("=");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;
    }

    public String createIndex(String indices) throws IOException {
        //判断索引是否存在
        TypeExist indexExist = new TypeExist.Builder(indices).build();
        JestResult result = client.execute(indexExist);
        System.out.println("index exist result " + result.getJsonString());
        Object indexFound = result.getValue("found");

        if (indexFound != null && indexFound.toString().equals("false")) {
            //index 不存在,创建 index
            System.out.println("index found == false");
            JestResult createIndexresult = client.execute(new CreateIndex.Builder(indices).build());
            System.out.println("create index:"+createIndexresult.isSucceeded());
            if(createIndexresult.isSucceeded()) {
                return "ok";
            }else{
                return "create index fail";
            }
        }else{
            return "ok";
        }
    }

    /**
     * 创建索引时指定分词器
     * 这里指定了：ik 和 pinyin
     *
     * 不指定分词器时：搜索 suduyujiqing 没有任何搜索结果
     * 加上pinyin分词器后会返回 速度与激情 相关的电影
     * @throws IOException
     */
    @Test
    public void createMapping() throws IOException {
        String INDEX = "douban_movie";
        String TYPE = "movie";

        JSONObject objSource = new JSONObject().fluentPut("properties", new JSONObject()
                .fluentPut("title", new JSONObject()
                        .fluentPut("type", "text")
                        .fluentPut("analyzer", "ik_max_word")
                        .fluentPut("analyzer", "pinyin")
                )
                .fluentPut("actors", new JSONObject()
                        .fluentPut("type", "text")
                        .fluentPut("analyzer", "ik_max_word")
                        .fluentPut("analyzer", "pinyin")
                )
        );
        log.info(objSource.toJSONString());
        // 构造PutMapping
        PutMapping putMapping = new PutMapping.Builder(INDEX,TYPE, objSource.toJSONString()).build();
        JestResult mapingResult = client.execute(putMapping);
        log.info(mapingResult.getJsonString());
    }


    public static void main(String[] args) throws IOException {
        DouBanMovieTest douBanMovieTest = new DouBanMovieTest();
        douBanMovieTest.getCategoryByLocalJson();
    }

}
