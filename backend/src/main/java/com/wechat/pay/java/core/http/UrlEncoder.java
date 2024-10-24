package com.wechat.pay.java.core.http;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlEncoder {

  /**
   * 对参数进行url编码
   *
   * @param string 待编码的字符串
   * @return 编码后的字符串
   */
  public static String urlEncode(String string) {
      return URLEncoder.encode(string, StandardCharsets.UTF_8);
  }
}
