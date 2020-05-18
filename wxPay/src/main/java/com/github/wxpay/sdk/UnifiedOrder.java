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
    public static void main(String[] args){
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
            map.put("signType","MD5");
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
            m.put("appid",config.getAppID());
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            m.put("timestamp",timestamp);
            m.put("nonceStr",WXPayUtil.generateNonceStr());
            String prepayId = result.get("prepay_id");
            String packages = "prepay_id = " + prepayId;
            m.put("package",packages);
            m.put("signType","MD5");
            m.put("paySign",WXPayUtil.generateSignature(m,config.getKey()));

            System.out.println(m);
            System.out.println(resultCode);
            System.out.println(returnMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static synchronized String genOrderNo(String tag) {
        return tag+System.currentTimeMillis() + ""  + (new Random(4));

    }
}
