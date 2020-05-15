package com.github.wxpay.utils;

import java.io.*;

/**
 * @author gongyujie
 * @date 2020/5/15
 */
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
