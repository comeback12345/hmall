import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CreateIndexTest {

    private RestHighLevelClient client;

    public static void main(String[] args) throws IOException {
        CreateIndexTest test = new CreateIndexTest();
        test.setUp();
        test.testCreateIndex();
        test.tearDown();
    }

    void testCreateIndex() throws IOException {
        String indexName = "items";
        
        String jsonContent = FileUtil.readString(
            "src/main/resources/elasticsearch/items_mapping.json", 
            StandardCharsets.UTF_8
        );
        
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.source(jsonContent, XContentType.JSON);
        
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        
        boolean acknowledged = response.isAcknowledged();
        if (acknowledged) {
            log.info("索引库 {} 创建成功！", indexName);
        } else {
            log.error("索引库 {} 创建失败！", indexName);
        }
    }

    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.3.106:9200")
        ));
    }

    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
