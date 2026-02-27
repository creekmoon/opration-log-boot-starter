package cn.creekmoon.operationLog.heatmap;

import cn.creekmoon.operationLog.profile.ProfileProperties;
import cn.creekmoon.operationLog.profile.ProfileService;
import cn.creekmoon.operationLog.profile.ProfileCollector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 热力图和用户画像自动配置类
 */
@Configuration
@EnableConfigurationProperties({HeatmapProperties.class, ProfileProperties.class})
@ConditionalOnClass(StringRedisTemplate.class)
public class HeatmapConfiguration {

    /**
     * 热力图服务
     */
    @Bean
    @ConditionalOnMissingBean(HeatmapService.class)
    @ConditionalOnProperty(prefix = "operation-log.heatmap", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HeatmapService heatmapService(StringRedisTemplate redisTemplate, HeatmapProperties properties) {
        return new HeatmapServiceImpl(redisTemplate, properties);
    }

    /**
     * 热力图数据收集器
     */
    @Bean
    @ConditionalOnMissingBean(HeatmapCollector.class)
    @ConditionalOnProperty(prefix = "operation-log.heatmap", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HeatmapCollector heatmapCollector(HeatmapService heatmapService, HeatmapProperties properties) {
        return new HeatmapCollector(heatmapService, properties);
    }

    /**
     * 用户画像服务
     */
    @Bean
    @ConditionalOnMissingBean(ProfileService.class)
    @ConditionalOnProperty(prefix = "operation-log.profile", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ProfileService profileService(StringRedisTemplate redisTemplate, ProfileProperties properties) {
        return new ProfileServiceImpl(redisTemplate, properties);
    }

    /**
     * 用户画像数据收集器
     */
    @Bean
    @ConditionalOnMissingBean(ProfileCollector.class)
    @ConditionalOnProperty(prefix = "operation-log.profile", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ProfileCollector profileCollector(ProfileService profileService, ProfileProperties properties) {
        return new ProfileCollector(profileService, properties);
    }
}
