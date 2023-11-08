package cn.creekmoon.operationLog.example.controller;

import cn.creekmoon.operationLog.core.OperationLog;
import cn.creekmoon.operationLog.core.OperationLogContext;
import cn.creekmoon.operationLog.example.model.TCargo;
import cn.creekmoon.operationLog.example.service.ExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;


@RestController("/example")
public class ExampleController {


    public static TCargo cargo = new TCargo();

    @Autowired
    ExampleService exampleService;

    @GetMapping("/test")
    @OperationLog
    public String test() throws Exception {
        cargo.setCargoNo("TEST-1");
        cargo.setCreateTime(new Date());
        OperationLogContext.follow(this::getLatestCargo);
        cargo.setCargoNo("TEST-2");
        return "good";
    }

    @GetMapping("/test2")
    public String test2() throws Exception {
        cargo.setCargoNo("TEST-1");
        cargo.setCreateTime(new Date());
        exampleService.updateCargo(cargo);
        return "good";
    }

    TCargo getLatestCargo() {
        return cargo;
    }
}
