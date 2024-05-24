package cn.jackding.xiaoyasync;

import lombok.extern.log4j.Log4j2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @Author Jack
 * @Date 2024/5/24 13:41
 * @Version 1.0.0
 */
@Log4j2
public class Util {


    public static String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return str;
    }

    public static String decode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return str;
    }

}
