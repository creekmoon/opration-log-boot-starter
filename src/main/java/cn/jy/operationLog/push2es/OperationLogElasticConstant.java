package cn.jy.operationLog.push2es;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ES连接常量
 */
@Component //定义配置类
@Data //提供get set方法
@ConfigurationProperties(prefix = "operation-log.elasticSearch") //yml配置中的路径
public class OperationLogElasticConstant {
    private String hostName;
    private Integer port;
    private String userName;
    private String password;
    private String indexName;
}
