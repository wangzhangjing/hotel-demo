package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) {
        try {
            //1??????request??????
            SearchRequest request = new SearchRequest("hotel");
            //2??????dsl??????
            buildBasicQuery(params, request);
            //2.2??????
            int page = params.getPage();
            int size = params.getSize();
            request.source().from(page).size(size);
            //2.3??????
            String location = params.getLocation();
            if (location !=null && !location.equals("")){
                request.source().sort(SortBuilders
                        .geoDistanceSort("location",new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS)
                );
            }
            //3????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public Map<String, List<String>> filters(RequestParams params) {
        try {
            SearchRequest request =new SearchRequest("hotel");
            buildBasicQuery(params, request);
            request.source().size(0);
            buildAggregation(request);
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            Map<String,List<String>> result =new HashMap<>();

            Aggregations aggregations = response.getAggregations();
            //??????????????????????????????
            List<String> brandList = getAggByName(aggregations,"brandAgg");
            result.put("??????",brandList);
            //??????????????????????????????
            List<String> cityList = getAggByName(aggregations,"cityAgg");
            result.put("??????",cityList);
            //??????????????????????????????
            List<String> starList = getAggByName(aggregations,"starAgg");
            result.put("??????",starList);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<String> getAggByName(Aggregations aggregations,String aggName) {
        Terms brandTerms = aggregations.get(aggName);
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        List<String> brandList =new ArrayList<>();
        for (Terms.Bucket bucket:buckets) {
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }

    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("cityAgg")
                .field("city")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("starAgg")
                .field("starName.keyword")
                .size(100)
        );
    }

    private void buildBasicQuery(RequestParams params, SearchRequest request) {
        //??????BooleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // ???????????????
        String key = params.getKey();
        if (key ==null || "".equals(key)){
            boolQuery.must(QueryBuilders.matchAllQuery());
        }else {
            boolQuery.must(QueryBuilders.matchQuery("all",key));
        }
        //????????????
        //????????????
        if (params.getCity() != null && params.getCity().equals("")){
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        //????????????
        if (params.getBrand() != null && params.getBrand().equals("")){
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        //????????????
        if (params.getStarName() != null && params.getStarName().equals("")){
            boolQuery.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }
        //??????
        if (params.getMinPrice() !=null && params.getMaxPrice() != null){
            boolQuery.filter(QueryBuilders.rangeQuery("price")
                    .gte(params.getMinPrice())
                    .lte(params.getMaxPrice()));
        }
        //????????????
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders
                .functionScoreQuery(
                        //????????????
                        boolQuery,
                        //FunctionScore?????????
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                //??????FunctionScore??????
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        //????????????
                                        QueryBuilders.termQuery("isAD",true),
                                        //????????????
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });
        request.source().query(functionScoreQueryBuilder);
    }

    private PageResult handleResponse(SearchResponse response) {
        //4????????????
        SearchHits searchHits = response.getHits();
        //4.1???????????????
        long total = searchHits.getTotalHits().value;
        System.out.println("????????????"+total+"?????????");
        //4.2????????????
        SearchHit[] hits = searchHits.getHits();
        //4.3??????
        List<HotelDoc> hotels =new ArrayList<>();
        for (SearchHit hit:hits) {
            //????????????source
            String json = hit.getSourceAsString();
            //?????????
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //???????????????
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length >0){
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }
            hotels.add(hotelDoc);
        }
        return new PageResult(total,hotels);
    }
}
