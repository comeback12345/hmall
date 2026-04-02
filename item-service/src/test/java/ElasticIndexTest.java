import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hmall.item.domain.po.Item;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.search.domain.po.ItemDoc;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@SpringBootTest(classes = com.hmall.item.ItemApplication.class)
public class ElasticIndexTest {
    private RestHighLevelClient client;

    @Resource
    private ItemMapper itemMapper;
    
    private ObjectMapper objectMapper;

    @Test
    void testConnection() {
        System.out.println("client="+client);
    }

    @Test
    void testCreat() throws IOException {
        //准备request
        CreateIndexRequest request = new CreateIndexRequest("items");
        //准备请求参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        //发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGet() throws IOException {
        //准备request
        GetIndexRequest request = new GetIndexRequest("items");
        //发送请求
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    @Test
    void testDelete() throws IOException {
        //准备 request
        DeleteIndexRequest request = new DeleteIndexRequest("items");
        //发送请求
        client.indices().delete(request, RequestOptions.DEFAULT);
        System.out.println("Elasticsearch 索引 'items' 已删除，所有数据已清空");
    }

    /**
     * 删除 Elasticsearch 中的所有商品数据（保留索引结构）
     */
    @Test
    void testDeleteAllItemsData() throws IOException {
        // 方案 1: 直接删除整个索引（最简单）
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("items");
        client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        System.out.println("Elasticsearch 索引已删除，所有数据已清空");
        
        // 如果需要保留索引结构，可以先删除再重新创建
        // CreateIndexRequest createIndexRequest = new CreateIndexRequest("items");
        // createIndexRequest.source(MAPPING_TEMPLATE, XContentType.JSON);
        // client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        // System.out.println("索引已重新创建");
    }

    /**
     * 全量同步 MySQL 数据到 Elasticsearch
     */
    @Test
    void testSyncAllItemsToElasticsearch() throws IOException {
        // 1. 创建索引（如果不存在）
        createIndexIfNotExists();
        
        // 2. 查询 MySQL 中的所有商品数据（只查询状态正常的商品）
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        List<Item> items = itemMapper.selectList(queryWrapper);
        
        System.out.println("共查询到 " + items.size() + " 条商品数据");
        
        // 3. 分批同步到 Elasticsearch（每批 100 条）
        int batchSize = 100;
        int totalBatches = (items.size() + batchSize - 1) / batchSize;
        
        for (int i = 0; i < totalBatches; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, items.size());
            List<Item> batchItems = items.subList(fromIndex, toIndex);
            
            BulkRequest bulkRequest = new BulkRequest();
            
            for (Item item : batchItems) {
                // 转换为 ItemDoc
                ItemDoc itemDoc = convertToItemDoc(item);
                String jsonString = objectMapper.writeValueAsString(itemDoc);
                
                IndexRequest indexRequest = new IndexRequest("items")
                        .id(itemDoc.getId())
                        .source(jsonString, XContentType.JSON);
                
                bulkRequest.add(indexRequest);
            }
            
            // 执行批量插入
            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            System.out.println("已同步第 " + (i + 1) + "/" + totalBatches + " 批数据");
        }
        
        System.out.println("全部商品数据同步完成，共同步 " + items.size() + " 条记录");
    }

    /**
     * 将 Item 转换为 ItemDoc（使用 search-service 的 ItemDoc）
     */
    private ItemDoc convertToItemDoc(Item item) {
        ItemDoc itemDoc = new ItemDoc();
        BeanUtils.copyProperties(item, itemDoc);
        itemDoc.setId(item.getId().toString());
        return itemDoc;
    }

    /**
     * 创建索引（如果不存在）
     */
    private void createIndexIfNotExists() throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest("items");
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        
        if (!exists) {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("items");
            createIndexRequest.source(MAPPING_TEMPLATE, XContentType.JSON);
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            System.out.println("Elasticsearch 索引 'items' 创建成功");
        } else {
            System.out.println("Elasticsearch 索引 'items' 已存在");
        }
    }

    @BeforeEach
    void setUp(){
        client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.3.106:9200")
        ));
        
        // 配置 ObjectMapper 支持 Java 8 日期时间类型
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    private static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\",\n" +
            "        \"search_analyzer\": \"ik_smart\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\",\n" +
            "        \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
