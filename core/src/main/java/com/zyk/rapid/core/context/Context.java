package com.zyk.rapid.core.context;

import com.zyk.gateway.common.config.Rule;
import io.netty.channel.ChannelHandlerContext;

import java.util.function.Consumer;

/**
 * 网关上下文接口定义
 */
public interface Context {
    /**
     * 一个请求正在执行过程中
     */
    int RUNNING = -1;

    /**
     * 写回响应标记, 标记当前Context/请求需要写回
     */
    int WRITTEN = 0;

    /**
     * 当写回成功后, 设置该标记：ctx.writeAndFlush(response);
     */
    int COMPLETED = 1;

    /**
     * 表示整个网关请求完毕, 彻底结束
     * 1. 正常结束，有响应结果或程序正常执行 pre->route-> post
     * 2. 异常结束，抛出异常
     */
    int TERMINATED = 2;

    /**
     * 设置上下文状态为正常运行状态
     */
    void runned();

    /**
     * 设置上下文状态为标记写回
     */
    void writtened();

    /**
     * 设置上下文状态为写回结束<BR>
     */
    void completed();

    /**
     * 设置上下文状态为最终结束
     */
    void terminated();

    /*************** -- 判断网关的状态系 -- ********************/

    boolean isRunning();

    boolean isWrittened();

    boolean isCompleted();

    boolean isTerminated();

    /**
     * 获取请求转换协议
     */
    String getProtocol();

    /**
     * 获取规则
     */
    Rule getRule();

    /**
     * 获取请求对象
     */
    Object getRequest();

    /**
     * 获取响应对象
     */
    Object getResponse();

    /**
     * 设置响应对象
     */
    void setResponse(Object response);

    /**
     * 设置异常信息
     */
    void setThrowable(Throwable throwable);

    /**
     * 获取异常
     */
    Throwable getThrowable();

    /**
     * 获取上下文参数
     */
    <T> T getAttribute(AttributeKey<T> key);

    /**
     * 保存上下文属性信息
     */
    <T> T putAttribute(AttributeKey<T> key, T value);

    /**
     * 获取Netty的上下文对象
     */
    ChannelHandlerContext getNettyCtx();

    /**
     * 是否保持连接
     */
    boolean isKeepAlive();

    /**
     * 释放请求资源的方法
     */
    void releaseRequest();

    /**
     * 写回接收回调函数设置
     */
    void completedCallback(Consumer<Context> consumer);

    /**
     * 回调函数执行
     */
    void invokeCompletedCallback();

    /**
     * 	SR(Server[Rapid-Core] Received):	网关服务器接收到网络请求
     * 	SS(Server[Rapid-Core] Send):		网关服务器写回请求
     * 	RS(Route Send):						网关客户端发送请求
     * 	RR(Route Received): 				网关客户端收到请求
     */

    long getSRTime();

    void setSRTime(long sRTime);

    long getSSTime();

    void setSSTime(long sSTime);

    long getRSTime();

    void setRSTime(long rSTime);

    long getRRTime();

    void setRRTime(long rRTime);


}
