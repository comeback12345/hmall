import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Elasticsearch 索引管理测试（不需要 Spring 容器）
 */
public class ElasticIndexManageTest {
    private RestHighLevelClient client;

    @Test
    void testDeleteIndex() throws IOException {
        // 删除整个索引
        DeleteIndexRequest request = new DeleteIndexRequest("items");
        client.indices().delete(request, RequestOptions.DEFAULT);
        System.out.println("Elasticsearch 索引 'items' 已删除，所有数据已清空");
    }

    @Test
    void testCheckIndexExists() throws IOException {
        // 检查索引是否存在
        GetIndexRequest request = new GetIndexRequest("items");
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println("索引 'items' 存在：" + exists);
    }

    @BeforeEach
    void setUp(){
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.3.106:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
