package cn.jy.operationLog.example.model;

import lombok.Data;

@Data
public class TCargo {


    private String id;
    private String cargoNo;
    private String cargoName;
    private String weightTotle;
    private String volumeTotle;
    private String packageTotle;
    private String packingUnit;
    private String goodsValueTotle;
    private String createTime;
    private String createUser;
    private String updateTime;
    private String updateUser;
}
