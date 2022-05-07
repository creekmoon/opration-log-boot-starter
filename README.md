# operation-log-starter

简易的业务操作日志AOP实现类, 用于记录业务中的Controller的操作日志,能记录用户什么时候修改了哪些字段

#### maven引用方式

```xml

<dependency>
    <groupId>io.github.yinjiangyue</groupId>
    <artifactId>operation-log-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 踩雷警告! 使用前注意!!

个人水平非常非常有限,这个工具性能比较差,而且应该有很多BUG  
这个工具需要额外依赖

* fastjson
* hutool-all
* lombok

### 开始使用

首先在SpringBoot启动类加上注解@EnableOperationLog

```java

@EnableOperationLog
@ComponentScan(basePackages = {"com.vdp", "com.wtx.mgt"})
public class VdpWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(VdpWebApplication.class, args);
    }

} 
```

在需要记录操作的controller方法上加入注解 @OperationLog

```java
@RequestMapping("web/{version}/tTransport")
public class TTransportController {

    @PostMapping(value = "/update")
    @OperationLog
    public ReturnValue update(TTransport tTransport) {

    }
}

```

### 查看效果

如果能够成功启动.那么会将操作的情况打印到控制台.

### 需要进行自定义

实现OperationLogHandler接口
这里需要做两件事情:

* 如何定义请求成功状态(操作失败时可以不进行记录)
* 如何处理数据 (自己存储到Elastic或者Mysql)

```java

@Component
public class MyOperationLogHandler implements OperationLogHandler {
    @Override
    public boolean requestIsSuccess(Object methodResult) {
        return methodResult instanceof ReturnValue && ((ReturnValue) methodResult).isSuccess();
    }

    /*这个save方法是异步的*/
    @Override
    public void save(LogRecord logRecord) {

        System.out.println(logRecord.toString());
    }
}

```

## 결제창 띄우는 예제 코드

```java 
String easyUserToken = userToken.user_token; //example. 621ef840ec81b404d7c6fe83
BootUser user = new BootUser().setPhone("010-1234-5678");
BootExtra extra = new BootExtra().setCardQuota("6");

BioPayload bioPayload = new BioPayload(); 

bioPayload.setPg("nicepay")
        .setApplicationId(applicationId)
        .setOrderName("bootpay test")
        .setUserToken(easyUserToken)
        .setPrice(50000.0) //최종 결제 금액
        .setOrderId(String.valueOf(System.currentTimeMillis())) //개발사에서 관리하는 주문번호
        .setUser(user)
        .setExtra(extra)
        .setOrderName("플리츠레이어 카라숏원피스")
        .setNames(Arrays.asList("블랙 (COLOR)", "55 (SIZE)")) //결제창에 나타날 상품목록
        .setPrices(Arrays.asList(new BioPrice("상품가격", 89000.0),  //결제창에 나타날 가격목록
                new BioPrice("쿠폰적용", -25000.0),
                new BioPrice("배송비", 2500.0)));
BootpayBio.init(this)
                .setBioPayload(bioPayload)
                .setEventListener(new BootpayEventListener() {
                    @Override
                    public void onCancel(String data) {
                        Log.d("bootpay cancel", data);
                    }

                    @Override
                    public void onError(String data) {
                        Log.d("bootpay error", data);
//                        BootpayBio.removePaymentWindow();
                    }

                    @Override
                    public void onClose(String data) {
                        Log.d("bootpay close", data);
                        BootpayBio.removePaymentWindow();
                    }

                    @Override
                    public void onIssued(String data) {
                        Log.d("bootpay issued", data);

                    }


                    @Override
                    public boolean onConfirm(String data) {
                        Log.d("bootpay confirm", data);
//                        return false; //재고 없으면 return false
//                        BootpayBio.transactionConfirm(data);
                        return true; // 재고 있으면 return true
                    }

                    @Override
                    public void onDone(String data) {
                        Log.d("bootpay done", data);
//                        BootpayBio.removePaymentWindow();
                    }
                })
                .requestPayment();
```

결제 진행 상태에 따라 LifeCycle 함수가 실행됩니다. 각 함수에 대한 상세 설명은 아래를 참고하세요.

### onError 함수

결제 진행 중 오류가 발생된 경우 호출되는 함수입니다. 진행중 에러가 발생되는 경우는 다음과 같습니다.

1. **부트페이 관리자에서 활성화 하지 않은 PG, 결제수단을 사용하고자 할 때**
2. **PG에서 보내온 결제 정보를 부트페이 관리자에 잘못 입력하거나 입력하지 않은 경우**
3. **결제 진행 도중 한도초과, 카드정지, 휴대폰소액결제 막힘, 계좌이체 불가 등의 사유로 결제가 안되는 경우**
4. **PG에서 리턴된 값이 다른 Client에 의해 변조된 경우**

에러가 난 경우 해당 함수를 통해 관련 에러 메세지를 사용자에게 보여줄 수 있습니다.

data 포맷은 아래와 같습니다.

```text
{
  action: "BootpayError",
  message: "카드사 거절",
  receipt_id: "5fffab350c20b903e88a2cff"
}
```

### onCancel 함수

결제 진행 중 사용자가 PG 결제창에서 취소 혹은 닫기 버튼을 눌러 나온 경우 입니다. ****

data 포맷은 아래와 같습니다.

```text
{
  action: "BootpayCancel",
  message: "사용자가 결제를 취소하였습니다.",
  receipt_id: "5fffab350c20b903e88a2cff"
}
```

### onIssued 함수

가상계좌 발급이 완료되면 호출되는 함수입니다. 가상계좌는 다른 결제와 다르게 입금할 계좌 번호 발급 이후 입금 후에 Feedback URL을 통해 통지가 됩니다. 발급된 가상계좌 정보를 ready 함수를 통해
확인하실 수 있습니다.

data 포맷은 아래와 같습니다.

```text
{
  account: "T0309260001169"
  accounthodler: "한국사이버결제"
  action: "BootpayBankReady"
  bankcode: "BK03"
  bankname: "기업은행"
  expiredate: "2021-01-17 00:00:00"
  item_name: "테스트 아이템"
  method: "vbank"
  method_name: "가상계좌"
  order_id: "1610591554856"
  params: null
  payment_group: "vbank"
  payment_group_name: "가상계좌"
  payment_name: "가상계좌"
  pg: "kcp"
  pg_name: "KCP"
  price: 3000
  purchased_at: null
  ready_url: "https://dev-app.bootpay.co.kr/bank/7o044QyX7p"
  receipt_id: "5fffad430c20b903e88a2d17"
  requested_at: "2021-01-14 11:32:35"
  status: 2
  tax_free: 0
  url: "https://d-cdn.bootapi.com"
  username: "홍길동"
}
```

### onConfirm 함수

결제 승인이 되기 전 호출되는 함수입니다. 승인 이전 관련 로직을 서버 혹은 클라이언트에서 수행 후 결제를 승인해도 될 경우`BootPay.transactionConfirm(data); 또는 return true;`

코드를 실행해주시면 PG에서 결제 승인이 진행이 됩니다.

**\* 페이앱, 페이레터 PG는 이 함수가 실행되지 않고 바로 결제가 승인되는 PG 입니다. 참고해주시기 바랍니다.**

data 포맷은 아래와 같습니다.

```text
{
  receipt_id: "5fffc0460c20b903e88a2d2c",
  action: "BootpayConfirm"
}
```

### onDone 함수

PG에서 거래 승인 이후에 호출 되는 함수입니다. 결제 완료 후 다음 결제 결과를 호출 할 수 있는 함수 입니다.

이 함수가 호출 된 후 반드시 REST API를 통해 [결제검증](https://docs.bootpay.co.kr/rest/verify)을 수행해야합니다. data 포맷은 아래와 같습니다.

```text
{
  action: "BootpayDone"
  card_code: "CCKM",
  card_name: "KB국민카드",
  card_no: "0000120000000014",
  card_quota: "00",
  item_name: "테스트 아이템",
  method: "card",
  method_name: "카드결제",
  order_id: "1610596422328",
  payment_group: "card",
  payment_group_name: "신용카드",
  payment_name: "카드결제",
  pg: "kcp",
  pg_name: "KCP",
  price: 100,
  purchased_at: "2021-01-14 12:54:53",
  receipt_id: "5fffc0460c20b903e88a2d2c",
  receipt_url: "https://app.bootpay.co.kr/bill/UFMvZzJqSWNDNU9ERWh1YmUycU9hdnBkV29DVlJqdzUxRzZyNXRXbkNVZW81%0AQT09LS1XYlNJN1VoMDI4Q1hRdDh1LS10MEtZVmE4c1dyWHNHTXpZTVVLUk1R%0APT0%3D%0A",
  requested_at: "2021-01-14 12:53:42",
  status: 1,
  tax_free: 0,
  url: "https://d-cdn.bootapi.com"
}
```

# 기타 문의사항이 있으시다면

1. [부트페이 개발연동 문서](https://app.gitbook.com/@bootpay/s/docs/client/pg/android) 참고
2. [부트페이 홈페이지](https://www.bootpay.co.kr) 참고 - 사이트 우측 하단에 채팅으로 기술문의 주시면 됩니다.
