package com.github.wxpay.utils;

import org.apache.http.conn.ConnectTimeoutException;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gongyujie
 * @date 2020/5/15
 */
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
