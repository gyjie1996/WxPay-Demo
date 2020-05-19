# WxPay 统一下单，退款demo

[]: 



* 凑齐必填参数
  * appid
  * mch_id
  * nonce_str                WXPayUtil中的generateNonceStr()生成即可
  * body 
  * out_trade_no 
  * total_fee 
  * spbill_create_ip 
  * notify_url 
  * trade_type 
  * openid 
  * sign                            其他10个参数存放到map中，调用WXPayUtil中的generateSignature()生成

调用统一下单接口，拿到返回结果

~~~xml
<xml>
  <return_code><![CDATA[SUCCESS]]></return_code>
  <return_msg><![CDATA[OK]]></return_msg>
  <appid><![CDATA[wx2421b1c4370ec43b]]></appid>
  <mch_id><![CDATA[10000100]]></mch_id>
  <nonce_str><![CDATA[IITRi8Iabbblz1Jc]]></nonce_str>
  <openid><![CDATA[oUpF8uMuAJO_M2pxb1Q9zNjWeS6o]]></openid>
  <sign><![CDATA[7921E432F65EB8ED0CE9755F0E86D72F]]></sign>
  <result_code><![CDATA[SUCCESS]]></result_code>
  <prepay_id><![CDATA[wx201411101639507cbf6ffd8b0779950874]]></prepay_id>
  <trade_type><![CDATA[JSAPI]]></trade_type>
</xml>
~~~

凑齐前端所需参数

* appid
* timeStamp
* nonce_str 
* package           格式为： "prepay_id=wx201411101639507cbf6ffd8b0779950874"
* signType
* sign                   通过其他5个参数生成

配置myConfig

~~~java
package com.github.wxpay.utils;

import java.io.*;

public class MyConfig extends WXPayConfig {
    private byte[] certData;

    public MyConfig() throws IOException {
        String certPath = "证书所在地址";
        File file = new File(certPath);
        if (!file.exists()) {
            return;
        }
        try {
            InputStream certStream = new FileInputStream(file);
            this.certData = new byte[(int) file.length()];
            certStream.read(this.certData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAppID() {
        return "wx8888888888888888";
    }

    @Override
    public String getMchID() {
        return "12888888";
    }

    @Override
    public String getKey() {
        return "88888888888888888888888888888888";
    }

    @Override
    public InputStream getCertStream() {
        ByteArrayInputStream certBis = new ByteArrayInputStream(this.certData);
        return certBis;
    }

    @Override
    IWXPayDomain getWXPayDomain() {
        return IWXPayDomainImpl.instance();
    }

    @Override
    public int getHttpConnectTimeoutMs() {
        return 8000;
    }

    @Override
    public int getHttpReadTimeoutMs() {
        return 10000;
    }
}

~~~

实现domain接口

~~~java
package com.github.wxpay.utils;

import org.apache.http.conn.ConnectTimeoutException;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class IWXPayDomainImpl implements IWXPayDomain {
    private IWXPayDomainImpl() {
    }

    static class DomainStatics {
        final String domain;
        int succCount = 0;
        int connectTimeoutCount = 0;
        int dnsErrorCount = 0;
        int otherErrorCount = 0;

        DomainStatics(String domain) {
            this.domain = domain;
        }

        void resetCount() {
            succCount = connectTimeoutCount = dnsErrorCount = otherErrorCount = 0;
        }

        boolean isGood() {
            return connectTimeoutCount <= 2 && dnsErrorCount <= 2;
        }

        int badCount() {
            return connectTimeoutCount + dnsErrorCount * 5 + otherErrorCount / 4;
        }
    }

    //3 minutes
    private final int MIN_SWITCH_PRIMARY_MSEC = 3 * 60 * 1000;
    private long switchToAlternateDomainTime = 0;
    private Map<String, DomainStatics> domainData = new HashMap<String, DomainStatics>();

    private static class WxpayDomainHolder {
        private static IWXPayDomain holder = new IWXPayDomainImpl();
    }

    public static IWXPayDomain instance() {
        return WxpayDomainHolder.holder;
    }

    public synchronized void report(final String domain, long elapsedTimeMillis, final Exception ex) {
        DomainStatics info = domainData.get(domain);
        if (info == null) {
            info = new DomainStatics(domain);
            domainData.put(domain, info);
        }

        //success
        if (ex == null) {
            //continue succ, clear error count
            if (info.succCount >= 2) {
                info.connectTimeoutCount = info.dnsErrorCount = info.otherErrorCount = 0;
            } else {
                ++info.succCount;
            }
        } else if (ex instanceof ConnectTimeoutException) {
            info.succCount = info.dnsErrorCount = 0;
            ++info.connectTimeoutCount;
        } else if (ex instanceof UnknownHostException) {
            info.succCount = 0;
            ++info.dnsErrorCount;
        } else {
            info.succCount = 0;
            ++info.otherErrorCount;
        }
    }

    public synchronized DomainInfo getDomain(final WXPayConfig config) {
        DomainStatics primaryDomain = domainData.get(WXPayConstants.DOMAIN_API);
        if (primaryDomain == null ||
                primaryDomain.isGood()) {
            return new DomainInfo(WXPayConstants.DOMAIN_API, true);
        }

        long now = System.currentTimeMillis();
        //first switch
        if (switchToAlternateDomainTime == 0) {
            switchToAlternateDomainTime = now;
            return new DomainInfo(WXPayConstants.DOMAIN_API2, false);
        } else if (now - switchToAlternateDomainTime < MIN_SWITCH_PRIMARY_MSEC) {
            DomainStatics alternateDomain = domainData.get(WXPayConstants.DOMAIN_API2);
            if (alternateDomain == null ||
                    alternateDomain.isGood() ||
                    alternateDomain.badCount() < primaryDomain.badCount()) {
                return new DomainInfo(WXPayConstants.DOMAIN_API2, false);
            } else {
                return new DomainInfo(WXPayConstants.DOMAIN_API, true);
            }
        } else {  //force switch back
            switchToAlternateDomainTime = 0;
            primaryDomain.resetCount();
            DomainStatics alternateDomain = domainData.get(WXPayConstants.DOMAIN_API2);
            if (alternateDomain != null) {
                alternateDomain.resetCount();
            }
            return new DomainInfo(WXPayConstants.DOMAIN_API, true);
        }
    }

}

~~~

统一下单demo

~~~java
package com.github.wxpay.sdk;

import com.github.wxpay.utils.MyConfig;
import com.github.wxpay.utils.WXPay;
import com.github.wxpay.utils.WXPayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author gongyujie
 * @date 2020/5/15
 */
public class UnifiedOrder {
    public static void main(String[] args) {
        try {
            MyConfig config = new MyConfig();
            WXPay wxPay = new WXPay(config);
            Map<String, String> map = new HashMap<String, String>(16);
            map.put("body", "测试");
            map.put("out_trade_no", genOrderNo("g"));
            map.put("total_fee", "100");
            map.put("spbill_create_ip", "127.0.0.1");
            map.put("notify_url", "");
            map.put("trade_type", "JSAPI");
            map.put("openid", "");
            map.put("signType", "MD5");
            map.put("nonce_str", WXPayUtil.generateNonceStr());
            Map<String, String> result = wxPay.unifiedOrder(map);
            String resultCode = result.get("result_code");
            String returnMsg = result.get("return_msg");
            /*
                统一下单返回参数：
                <xml>
                      <return_code><![CDATA[SUCCESS]]></return_code>
                      <return_msg><![CDATA[OK]]></return_msg>
                      <appid><![CDATA[wx2421b1c4370ec43b]]></appid>
                      <mch_id><![CDATA[10000100]]></mch_id>
                      <nonce_str><![CDATA[IITRi8Iabbblz1Jc]]></nonce_str>
                      <openid><![CDATA[oUpF8uMuAJO_M2pxb1Q9zNjWeS6o]]></openid>
                      <sign><![CDATA[7921E432F65EB8ED0CE9755F0E86D72F]]></sign>
                      <result_code><![CDATA[SUCCESS]]></result_code>
                      <prepay_id><![CDATA[wx201411101639507cbf6ffd8b0779950874]]></prepay_id>
                      <trade_type><![CDATA[JSAPI]]></trade_type>
                </xml>
             */
            //前台参数
            Map<String, String> m = new HashMap<String, String>(10);
            m.put("appid", config.getAppID());
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            m.put("timestamp", timestamp);
            m.put("nonceStr", WXPayUtil.generateNonceStr());
            String prepayId = result.get("prepay_id");
            String packages = "prepay_id = " + prepayId;
            m.put("package", packages);
            m.put("signType", "MD5");
            m.put("paySign", WXPayUtil.generateSignature(m, config.getKey()));

            System.out.println(m);
            System.out.println(resultCode);
            System.out.println(returnMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static synchronized String genOrderNo(String tag) {
        return tag + System.currentTimeMillis() + "" + (new Random(4));

    }
}

~~~

退款demo

~~~java
package com.github.wxpay.sdk;

import com.github.wxpay.utils.MyConfig;
import com.github.wxpay.utils.WXPay;
import com.github.wxpay.utils.WXPayConstants;
import com.github.wxpay.utils.WXPayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author gongyujie
 * @date 2020/5/15
 */
public class ReFund {
    public static void main(String[] args) {
        try {
            MyConfig config = new MyConfig();
            WXPay wxPay = new WXPay(config);
            Map<String, String> map = new HashMap<String, String>(16);
            map.put("appid", config.getAppID());
            map.put("mch_id", config.getMchID());
            String timestampStr = WXPayUtil.getCurrentTimestamp() + "";
            String noceStr = WXPayUtil.MD5(timestampStr);
            map.put("nonce_str", noceStr);
            map.put("transaction_id", genOrderNo("g"));
            map.put("out_refund_no", genOrderNo("r"));
            map.put("total_fee", "100");
            map.put("refund_fee", "100");
            Map<String, String> signMap = new HashMap<String, String>(8);
            signMap.put("appId", config.getAppID());
            signMap.put("nonceStr", noceStr);
            signMap.put("timeStamp", timestampStr);
            signMap.put("signType", "MD5");
            String sign = WXPayUtil.generateSignature(signMap, config.getKey(), WXPayConstants.SignType.MD5);
            map.put("sign", sign);
            map.put("sign_type", "MD5");
            Map<String, String> refund = wxPay.refund(map);
            System.out.println(refund);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static synchronized String genOrderNo(String tag) {
        return tag + System.currentTimeMillis() + "" + (new Random(4));

    }
}

~~~

