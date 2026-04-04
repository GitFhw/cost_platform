package com.ruoyi.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 配置，明确使用主库执行迁移。
 * 
 * @author codex
 */
@Configuration
public class FlywayConfig
{
    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource(@Qualifier("masterDataSource") DataSource masterDataSource)
    {
        return masterDataSource;
    }
}
