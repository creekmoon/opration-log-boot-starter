package cn.jy.operationLog.push2es;

import cn.hutool.core.lang.UUID;
import cn.jy.operationLog.core.LogRecord;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import junit.framework.Assert;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;


@Component //定义配置类
@Slf4j
public class OperationLogElasticClient {
    volatile CredentialsProvider cachedCredentialsProvider = null;

    /*ES 低级别Client*/
    RestClient restClient = null;
    /*ES JAVA API*/
    ElasticsearchClient elasticsearchClient = null;
    @Autowired
    OperationLogElasticConstant config;

    @PostConstruct
    void init() {
        Assert.assertNotNull(config.getIndexName());
        Assert.assertNotNull(config.getPort());
        Assert.assertNotNull(config.getHostName());
        initClient();
    }

    public void save2ElasticSearch(LogRecord logRecord){
        try {
            elasticsearchClient.index(IndexRequest.of(builder -> builder.index(config.getIndexName())
                    .id(UUID.fastUUID().toString())
                    .document(logRecord)
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(config.getHostName(), config.getPort()));

        if (config.getUserName() != null && config.getPassword() != null) {
            /*如果没有配置用户名和密码,就跳过这个*/
            builder.setHttpClientConfigCallback(
                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(loadCredentialsProvider())
            );
        }
        // Create the low-level client
        restClient = builder.build();
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // And create the API client
        elasticsearchClient = new ElasticsearchClient(transport);
    }


    private CredentialsProvider loadCredentialsProvider() {
        if (cachedCredentialsProvider == null) {
            synchronized (OperationLogElasticClient.class) {
                if (cachedCredentialsProvider == null) {
                    cachedCredentialsProvider = new BasicCredentialsProvider();
                    cachedCredentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(config.getUserName(), config.getPassword()));
                }
            }
        }
        return cachedCredentialsProvider;
    }
}

