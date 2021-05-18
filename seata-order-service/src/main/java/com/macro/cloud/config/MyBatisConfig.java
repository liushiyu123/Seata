package com.macro.cloud.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.macro.cloud.dao")
public class MyBatisConfig {
}