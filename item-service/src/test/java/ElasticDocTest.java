import cn.hutool.core.bean.BeanUtil;
import com.hmall.item.ItemApplication;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;


@SpringBootTest(properties = "spring.profiles.active=local",classes = ItemApplication.class)
public class ElasticDocTest {

    private RestHighLevelClient client;
    @Autowired
    private IItemService itemService;
    @Test
    void testIndexDoc() throws IOException {
        Item item = itemService.getById(100000011127L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        IndexRequest request = new IndexRequest("item").id(itemDoc.getId());
        request.source("{}", XContentType.JSON);
        client.index(request,RequestOptions.DEFAULT);
    }

    @BeforeEach
    void setUp(){
        client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.154.128:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }

}
