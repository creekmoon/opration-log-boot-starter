package cn.creekmoon.operationLog.example.model;


import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TCargo {

    private Long id;
    private String cargoNo;
    private String cargoName;
    private BigDecimal weightTotle;
    private BigDecimal volumeTotle;
    private BigDecimal packageTotle;
    private String packingUnit;
    private BigDecimal goodsValueTotle;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
