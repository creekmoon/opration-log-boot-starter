package cn.jy.operationLog.push2es;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

public class Test {
    public static void main(String[] args) {
        System.out.println("Hello world!");


        final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "qwe45678"));
        // Create the low-level client
        RestClient restClient = RestClient
                .builder(new HttpHost("121.5.52.88", 9200))
                .setHttpClientConfigCallback(
                        httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // And create the API client
        ElasticsearchClient client = new ElasticsearchClient(transport);
        Student student = new Student();
        student.setId(1551L);
        student.setName("ywwuyi");
        try {
            client.index(IndexRequest.of(builder -> builder.index("test-220510")
                    .id(String.valueOf(student.getId()))
                    .document(student)
            ));
            System.out.println("发送完毕");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}