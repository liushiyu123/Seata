package com.macro.cloud.controller;

import io.seata.config.ConfigurationFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Test {

    @GetMapping("test")
    public String test(){

        return "123456";
    }
}
