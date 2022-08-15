package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;
@SpringBootTest
public class HotelIndexTest {
    private RestHighLevelClient client;

    @Test
    void testInit(){
        System.out.println(client);
    }
   //创建
    @Test
    void createHotelIndex() throws IOException {
        //1创建Request对象
        CreateIndexRequest request = new CreateIndexRequest("hotel");
        //2准备请求参数：dsl语句
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        //3发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }
    //删除
    @Test
    void testDeleteHotelIndex() throws IOException {
        DeleteIndexRequest request =new DeleteIndexRequest("hotel");
        client.indices().delete(request,RequestOptions.DEFAULT);
    }
    //查询
    @Test
    void testExistsHotelIndex() throws IOException {
        GetIndexRequest request=new GetIndexRequest("hotel");
        boolean exists = client.indices().exists(request,RequestOptions.DEFAULT);
        System.err.println(exists?"索引库已经存在":"索引库不存在");
    }

    @BeforeEach
    void setUp(){
      this.client =new RestHighLevelClient(RestClient.builder(
              HttpHost.create("http://192.168.255.130:9200")
      ));
    }
    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
