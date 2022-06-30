package cn.jy.operationLog.example.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TCargo {

    @TableId(type = IdType.AUTO)
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
