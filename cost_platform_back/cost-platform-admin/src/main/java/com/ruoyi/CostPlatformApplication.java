package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class CostPlatformApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CostPlatformApplication.class, args);
        System.out.println("Cost Platform started successfully.");
    }
}
