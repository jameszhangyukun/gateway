package com.zyk.gateway.common.config;

/**
 * 服务调用的接口模型描述
 */
public interface ServiceInvoker {
    /**
     * 获取真实服务调用的全路径
     *
     * @return
     */
    String getInvokerPath();

    void setInvokerPath(String invokerPath);

    String getRuleId();

    void setRuleId(String ruleId);

    int getTimeout();

    void setTimeout(int timeout);
}
