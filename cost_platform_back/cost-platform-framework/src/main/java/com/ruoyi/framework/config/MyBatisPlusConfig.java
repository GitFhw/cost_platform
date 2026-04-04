package com.ruoyi.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * MyBatis-Plus 基础配置。
 *
 * <p>当前项目仍保留若依原生的 PageHelper 和 XML Mapper，用于兼容现有模块；
 * 新增或重构的核算模块可逐步接入 BaseMapper、LambdaQueryWrapper 和 MP 分页能力。</p>
 */
@Configuration
public class MyBatisPlusConfig
{
    /**
     * 注册 MyBatis-Plus 拦截器。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor()
    {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
