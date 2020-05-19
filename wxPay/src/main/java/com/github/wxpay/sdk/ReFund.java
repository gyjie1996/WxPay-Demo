package com.github.wxpay.sdk;

import com.github.wxpay.utils.MyConfig;
import com.github.wxpay.utils.WXPay;
import com.github.wxpay.utils.WXPayConstants;
import com.github.wxpay.utils.WXPayUtil;
import org.apache.commons.lang3.RandomStringUtils;

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
        return tag + System.currentTimeMillis() + "" + (RandomStringUtils.randomNumeric(4));

    }
}
