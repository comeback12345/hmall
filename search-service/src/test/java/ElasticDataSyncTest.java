import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.CollUtils;
import com.hmall.search.SearchApplication;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
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
import java.util.List;

@Slf4j
@SpringBootTest(properties = "spring.profiles.active=dev", classes = SearchApplication.class)
public class ElasticDataSyncTest {

    private RestHighLevelClient client;
    @Autowired
    private ISearchService searchService;

    @Test
    void testSyncAllDataToElasticsearch() throws IOException {
        log.info("开始将MySQL数据同步到Elasticsearch...");
        int pageNo = 1;
        int size = 500;
        int totalCount = 0;

        while (true) {
            Page<Item> page = searchService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(new Page<>(pageNo, size));

            List<Item> items = page.getRecords();
            if (CollUtils.isEmpty(items)) {
                log.info("数据同步完成！共同步 {} 条数据", totalCount);
                return;
            }

            log.info("加载第 {} 页数据，共 {} 条", pageNo, items.size());

            BulkRequest bulkRequest = new BulkRequest("items");
            for (Item item : items) {
                ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
                itemDoc.setId(item.getId().toString());

                bulkRequest.add(new IndexRequest()
                        .id(itemDoc.getId())
                        .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON));
            }

            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            totalCount += items.size();
            pageNo++;
        }
    }

    @BeforeEach
    void setUp() {
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
