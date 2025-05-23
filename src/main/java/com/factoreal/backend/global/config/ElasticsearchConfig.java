package com.factoreal.backend.global.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String esHost;
    @Value("${elasticsearch.port}")
    private int esPort;
    @Value("${elasticsearch.scheme}")
    private String esScheme;

    @Bean
    public RestHighLevelClient elasticsearchClient() {
        // Build ES client using properties
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(esHost, esPort, esScheme)
                )
        );
    }
}