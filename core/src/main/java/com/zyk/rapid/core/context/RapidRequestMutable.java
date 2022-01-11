package com.zyk.rapid.core.context;

import io.netty.handler.codec.http.cookie.Cookie;
import org.asynchttpclient.Request;

public interface RapidRequestMutable {
    /**
     * 设置Host
     *
     * @param host
     */
    void setModifyHost(String host);

    /**
     * 获取修改的Host
     *
     * @return
     */
    String getModifyHost();

    /**
     * 设置请求路径
     *
     * @param path
     */
    void setModifyPath(String path);

    /**
     * 获取修改的地址
     *
     * @return
     */
    String getModifyPath();

    /**
     * 添加请求头信息
     *
     * @param name
     * @param value
     */
    void addHeader(CharSequence name, String value);

    /**
     * 设置请求头信息
     *
     * @param name
     * @param value
     */
    void setHeader(CharSequence name, String value);

    /**
     * 添加请求查询参数
     *
     * @param name
     * @param value
     */
    void addQueryParam(String name, String value);

    /**
     * 添加或替换Cookie
     *
     * @param cookie
     */
    void addOrReplaceCookie(org.asynchttpclient.cookie.Cookie cookie);

    /**
     * 添加form表单参数
     *
     * @param name
     * @param value
     */
    void addFormParam(String name, String value);

    /**
     * 设置请求超时时间
     *
     * @param requestTimeout
     */
    void setRequestTimeout(int requestTimeout);

    /**
     * 构建转发请求的请求对象
     *
     * @return
     */
    Request build();

    /**
     * 获取最终的请求路径
     *
     * @return
     */
    String getFinalUrl();
}
