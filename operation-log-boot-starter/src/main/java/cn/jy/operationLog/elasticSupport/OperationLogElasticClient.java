package cn.jy.operationLog.elasticSupport;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.jy.operationLog.config.OperationLogConfig;
import cn.jy.operationLog.core.LogRecord;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
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
    OperationLogConfig.ElasticConfig config;

    @PostConstruct
    void init() {
        if (StrUtil.isBlank(config.getIndexName())
                || StrUtil.isBlank(config.getHost())
                || config.getPort() == null
        ) {
            throw new RuntimeException("OperationLogElasticClient 参数不完整!无法启动!");
        }
        initClient();
    }


    /**
     * 保存数据至ES
     *
     * @param logRecord  数据对象实体类
     * @param retryCount 重试次数（ 这个参数的意义在于,ES存在BUG 捕获I/O reactor的异常后会导致SSL中断， 此时再次请求就可以了）
     */
    public void save2ElasticSearch(LogRecord logRecord, int retryCount) {
        try {
            elasticsearchClient.index(IndexRequest.of(builder -> builder.index(config.getIndexName())
                    .id(UUID.fastUUID().toString())
                    .document(logRecord)
            ));
        } catch (IOException e) {
            /*如果保存失败, 则再次进行重试 直到重试次数为0就抛出异常*/
            if (retryCount > 0) {
                log.info("操作日志保存数据失败！ 再次尝试重试保存， 剩余{}次", retryCount - 1);
                save2ElasticSearch(logRecord, retryCount - 1);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    private void initClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(config.getHost(), config.getPort()));

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

