import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.item.ItemApplication;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
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
        //根据id查询数据库数据
        Item item = itemService.getById(100000011127L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        //准备request
        IndexRequest request = new IndexRequest("item").id(itemDoc.getId());
        //准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //发送请求
        client.index(request,RequestOptions.DEFAULT);
    }

    @Test
    void testGetDoc() throws IOException {
        //准备request
        GetRequest request = new GetRequest("item","100000011127");
        //发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //解析响应结果
        String json = response.getSourceAsString();
        ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
        System.out.println(doc);
    }

    @Test
    void testDeleteDoc() throws IOException {
        //准备request
        DeleteRequest request = new DeleteRequest("item","100000011127");
        //发送请求
         client.delete(request, RequestOptions.DEFAULT);
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
