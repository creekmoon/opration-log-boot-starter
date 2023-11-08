package cn.creekmoon.operationLog.example.service;

import cn.creekmoon.operationLog.core.OperationLog;
import cn.creekmoon.operationLog.core.OperationLogContext;
import cn.creekmoon.operationLog.example.model.TCargo;
import org.springframework.stereotype.Service;

@Service
public class ExampleService {


    @OperationLog("更新货物")
    public TCargo updateCargo(TCargo cargo) {
        OperationLogContext.follow(() -> cargo);

        cargo.setCargoNo("TEST-2");
        return cargo;
    }
}
