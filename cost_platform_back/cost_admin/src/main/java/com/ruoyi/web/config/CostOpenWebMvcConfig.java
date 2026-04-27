package com.ruoyi.web.config;

import com.ruoyi.web.interceptor.cost.CostOpenTokenAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 开放接口 MVC 配置
 */
@Configuration
public class CostOpenWebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private CostOpenTokenAuthInterceptor costOpenTokenAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(costOpenTokenAuthInterceptor)
                .addPathPatterns("/cost/open/**")
                .excludePathPatterns("/cost/open/auth/token");
    }
}
