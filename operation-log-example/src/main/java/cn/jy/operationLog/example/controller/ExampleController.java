package cn.jy.operationLog.example.controller;

import cn.hutool.core.util.RandomUtil;
import cn.jy.operationLog.core.OperationLog;
import cn.jy.operationLog.example.mapper.TCargoMapper;
import cn.jy.operationLog.example.model.TCargo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


@RestController("/example")
public class ExampleController {


    @Resource
    private TCargoMapper cargoMapper;

    @GetMapping("/test1")
    public void testSelect() {
        System.out.println(("----- selectAll method test ------"));
        List<TCargo> userList = cargoMapper.selectList(Wrappers.lambdaQuery());
        userList.forEach(System.out::println);
    }

    @GetMapping("/test2")
    @OperationLog
    @Transactional(rollbackFor = Exception.class)
    public String testInsert() throws Exception {
        for (int i = 0; i < 10; i++) {
            TCargo tCargo = new TCargo();
            tCargo.setCargoNo("TEST" + RandomUtil.randomString(5));
            tCargo.setCreateTime(new Date());
            cargoMapper.insert(tCargo);
            if (i == 5) {
                try {
                    throw new Exception("抛出一个异常");
                } catch (Exception e) {

                    return "error but success";
                }
            }
        }
        return "good succes";
    }
}
