package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.apache.lucene.index.IndexReader;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;
@SpringBootTest
public class HotelDocumentTest {
    private RestHighLevelClient client;
    @Autowired
    private IHotelService iHotelService;
  //新增转换
    @Test
    void testAddDocument() throws IOException {
        //根据id查询
        Hotel hotel = iHotelService.getById(61083L);
        //转换
        HotelDoc hotelDoc = new HotelDoc(hotel);
        //1准备Request对象
        IndexRequest request = new IndexRequest("hotel").id(hotel.getId().toString());
        //2准备json文档
        request.source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        //3发送请求
       client.index(request,RequestOptions.DEFAULT);
    }
   //查询
    @Test
    void testGetDocumentById() throws IOException {
        //1准备request
        GetRequest request = new GetRequest("hotel", "61083");
        //2发送请求得到响应
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //3解析响应结果
        String json = response.getSourceAsString();
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }
    //更新
    @Test
    void testUpdateDocument() throws IOException {
        //1准备request
        UpdateRequest request = new UpdateRequest("hotel", "61083");
        //2准备参数
        request.doc(
              "price","952",
              "starName","四钻"
        );
        //3发送请求
        client.update(request ,RequestOptions.DEFAULT);

    }

    @Test
    void testDeleteDocument() throws IOException {
        //1准备request
        DeleteRequest request = new DeleteRequest("hotel", "61083");
        //2发送请求
        client.delete(request,RequestOptions.DEFAULT);

    }

    @Test
    void testBulkRequest() throws IOException {
        //批量查询所有数据
        List<Hotel> hotels = iHotelService.list();
        //1准备request
        BulkRequest request = new BulkRequest();
        //2准备参数添加多个发送请求的Request
        for (Hotel hotel:hotels) {
            //转换文档类型
            HotelDoc hotelDoc = new HotelDoc(hotel);
            //创建新增文档的request对象
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc),XContentType.JSON));
        }
        //3发送请求
        client.bulk(request,RequestOptions.DEFAULT);
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
