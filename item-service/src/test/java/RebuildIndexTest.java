import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
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
public class RebuildIndexTest {
    private RestHighLevelClient client;

    @Resource
    private ItemMapper itemMapper;
    
    private ObjectMapper objectMapper;

    /**
     * 重建索引：删除旧索引 -> 创建新索引 -> 重新导入数据
     */
    @Test
    void testRebuildIndex() throws IOException {
        System.out.println("=== 开始重建索引 ===");
        
        // 1. 删除旧索引
//        deleteIndex();
        
        // 2. 创建新索引
        createIndex();
        
        // 3. 重新导入数据
        syncAllItemsToElasticsearch();
        
        System.out.println("=== 索引重建完成 ===");
    }

    private void deleteIndex() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest("items");
        client.indices().delete(request, RequestOptions.DEFAULT);
        System.out.println("Elasticsearch 索引 'items' 已删除");
    }

    private void createIndex() throws IOException {
        String jsonContent = "{\n" +
                "  \"settings\": {\n" +
                "    \"number_of_shards\": 3,\n" +
                "    \"number_of_replicas\": 1\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "    \"properties\": {\n" +
                "      \"id\": { \"type\": \"keyword\" },\n" +
                "      \"name\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" },\n" +
                "      \"price\": { \"type\": \"integer\" },\n" +
                "      \"stock\": { \"type\": \"integer\" },\n" +
                "      \"image\": { \"type\": \"keyword\", \"index\": false },\n" +
                "      \"category\": { \"type\": \"keyword\" },\n" +
                "      \"brand\": { \"type\": \"keyword\" },\n" +
                "      \"sold\": { \"type\": \"integer\" },\n" +
                "      \"commentCount\": { \"type\": \"integer\", \"index\": false },\n" +
                "      \"isAD\": { \"type\": \"boolean\" },\n" +
                "      \"updateTime\": { \"type\": \"date\" }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        CreateIndexRequest request = new CreateIndexRequest("items");
        request.source(jsonContent, XContentType.JSON);
        client.indices().create(request, RequestOptions.DEFAULT);
        System.out.println("Elasticsearch 索引 'items' 创建成功");
    }

    private void syncAllItemsToElasticsearch() throws IOException {
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        List<Item> items = itemMapper.selectList(queryWrapper);
        
        System.out.println("共查询到 " + items.size() + " 条商品数据");
        
        int batchSize = 100;
        int totalBatches = (items.size() + batchSize - 1) / batchSize;
        
        for (int i = 0; i < totalBatches; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, items.size());
            List<Item> batchItems = items.subList(fromIndex, toIndex);
            
            BulkRequest bulkRequest = new BulkRequest();
            
            for (Item item : batchItems) {
                ItemDoc itemDoc = convertToItemDoc(item);
                String jsonString = objectMapper.writeValueAsString(itemDoc);
                
                IndexRequest indexRequest = new IndexRequest("items")
                        .id(itemDoc.getId())
                        .source(jsonString, XContentType.JSON);
                
                bulkRequest.add(indexRequest);
            }
            
            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            System.out.println("已同步第 " + (i + 1) + "/" + totalBatches + " 批数据");
        }
        
        System.out.println("全部商品数据同步完成，共同步 " + items.size() + " 条记录");
    }

    private ItemDoc convertToItemDoc(Item item) {
        ItemDoc itemDoc = new ItemDoc();
        BeanUtils.copyProperties(item, itemDoc);
        itemDoc.setId(item.getId().toString());
        return itemDoc;
    }

    @BeforeEach
    void setUp(){
        client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.3.106:9200")
        ));
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
