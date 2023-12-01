package cn.creekmoon.operationLog.config;

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

    // 暂时没有任何需要配置的
}
