package cn.jy.operationLog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ES连接常量
 */
@Component //定义配置类
@Data //提供get set方法
@ConfigurationProperties(prefix = "operation-log") //yml配置中的路径
public class OperationLogConfig {


    /**
     * ES连接常量
     */
    @Component //定义配置类
    @Data //提供get set方法
    @ConfigurationProperties(prefix = "operation-log.elastic") //yml配置中的路径
    public static class ElasticConfig {
        /**
         * ES连接地址 例如 192.168.1.1
         */
        private String host;
        /**
         * ES连接端口 例如 9200
         */
        private Integer port;
        /**
         * ES连接用户名 例如 root
         */
        private String userName;
        /**
         * ES连接密码 例如 root
         */
        private String password;
        /**
         * ES连接索引名称 例如 operation-log
         */
        private String indexName;
    }
}
