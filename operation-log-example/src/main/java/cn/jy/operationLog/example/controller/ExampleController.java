package cn.jy.operationLog.example.controller;

import cn.jy.operationLog.example.mapper.TCargoMapper;
import cn.jy.operationLog.example.model.TCargo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;


@RestController("/example")
public class ExampleController {


    @Resource
    private TCargoMapper cargoMapper;

    @GetMapping("/test1")
    public void testSelect() {
        System.out.println(("----- selectAll method test ------"));
        List<TCargo> userList = cargoMapper.selectList(null);
        userList.forEach(System.out::println);
    }
}
