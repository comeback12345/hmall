import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.CollUtils;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SyncDataToElasticsearch {

    private RestHighLevelClient client;

    public static void main(String[] args) throws IOException {
        SyncDataToElasticsearch test = new SyncDataToElasticsearch();
        test.setUp();
        test.testSyncAllDataToElasticsearch();
        test.tearDown();
    }

    void testSyncAllDataToElasticsearch() throws IOException {
        log.info("开始将MySQL数据同步到Elasticsearch...");
        
        String jdbcUrl = "jdbc:mysql://192.168.3.106:3306/hm-item?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
        String username = "root";
        String password = "123";
        
        int pageNo = 1;
        int size = 500;
        int totalCount = 0;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            String sql = "SELECT id, name, price, stock, image, category, brand, spec, sold, comment_count, isAD, status, create_time, update_time, creater, updater FROM item WHERE status = 1 LIMIT ? OFFSET ?";
            
            while (true) {
                int offset = (pageNo - 1) * size;
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, size);
                    stmt.setInt(2, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Item> items = new ArrayList<>();
                        while (rs.next()) {
                            Item item = new Item();
                            item.setId(rs.getLong("id"));
                            item.setName(rs.getString("name"));
                            item.setPrice(rs.getInt("price"));
                            item.setStock(rs.getInt("stock"));
                            item.setImage(rs.getString("image"));
                            item.setCategory(rs.getString("category"));
                            item.setBrand(rs.getString("brand"));
                            item.setSold(rs.getInt("sold"));
                            item.setCommentCount(rs.getInt("comment_count"));
                            item.setIsAD(rs.getBoolean("isAD"));
                            items.add(item);
                        }
                        
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
            }
        } catch (Exception e) {
            log.error("数据同步失败", e);
            throw new IOException("数据同步失败", e);
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
