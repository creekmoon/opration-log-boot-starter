package cn.creekmoon.operationLog.example;


import cn.creekmoon.operationLog.config.EnableOperationLog;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;


@MapperScan("cn.jy.operationLog.example")
@Import(cn.hutool.extra.spring.SpringUtil.class)
@EnableOperationLog
@SpringBootApplication()
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }


}
