package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

@SpringBootTest
public class HotellSearchTest {
    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        //1准备request对象
        SearchRequest request = new SearchRequest("hotel");
        //2准备dsl语句
        request.source().query(QueryBuilders.matchAllQuery());
        //3发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4解析响应
        handleResponse(response);
    }
    @Test
    void testMatch() throws IOException {
        //1准备request对象
        SearchRequest request = new SearchRequest("hotel");
        //2准备dsl语句
        request.source().query(QueryBuilders.matchQuery("all","如家"));
        //3发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }
    @Test
    void testBool() throws IOException {
        //1准备request对象
        SearchRequest request = new SearchRequest("hotel");
        //2准备dsl语句
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("city","深圳"));
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(250));
        request.source().query(boolQuery);
        //3发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }
    @Test
    void testPageAndSort() throws IOException {
        int page =2 ,size =5;
        //1准备request对象
        SearchRequest request = new SearchRequest("hotel");
        //2准备dsl语句
        //2.1
        request.source().query(QueryBuilders.matchAllQuery());
        //2.2升序 sort
        request.source().sort("price", SortOrder.ASC);
        //2.3 分页 from size
        request.source().from((page-1)*size).size(size);
        //3发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }
    @Test
    void testHighlight() throws IOException {
        int page =2 ,size =5;
        //1准备request对象
        SearchRequest request = new SearchRequest("hotel");
        //2准备dsl语句
        //2.1
        request.source().query(QueryBuilders.matchQuery("all","如家"));
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        //3发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }


    private void handleResponse(SearchResponse response) {
        //4解析响应
        SearchHits searchHits = response.getHits();
        //4.1获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到"+total+"条数据");
        //4.2文档数组
        SearchHit[] hits = searchHits.getHits();
        //4.3遍历
        for (SearchHit hit:hits) {
            //获取文档source
            String json = hit.getSourceAsString();
            //序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
//            if (highlightFields == null || highlightFields.size() ==0){}
            if (!CollectionUtils.isEmpty(highlightFields)){
            //根据名字获取高亮结果
            HighlightField highlightField = highlightFields.get("name");
            if (highlightField != null){
            //获取高亮值
            String name = highlightField.getFragments()[0].string();
            //覆盖非高亮结果
            hotelDoc.setName(name);
            }}
            System.out.println(hotelDoc);
        }
    }
    @Test
    void testAggregation() throws IOException {
        //1.准备request
        SearchRequest request = new SearchRequest("hotel");
        //2准备dsl
        //2.1size为0清除文档数据
        request.source().size(0);
        //聚合
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(10)
        );
        //3发起请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms brandTerms = aggregations.get("brandAgg");
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        for (Terms.Bucket bucket:buckets) {
            String key = bucket.getKeyAsString();
            System.out.println(key);
        }
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
