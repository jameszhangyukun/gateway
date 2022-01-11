package com.zyk.rapid.core.context;

import com.zyk.gateway.common.config.Rule;
import com.zyk.gateway.common.util.AssertUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

public class RapidContext extends BasicContext {

    private final RapidRequest rapidRequest;

    private RapidResponse rapidResponse;

    private final Rule rule;

    private RapidContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepAlive,
                         RapidRequest rapidRequest, Rule rule) {
        super(protocol, nettyCtx, keepAlive);
        this.rapidRequest = rapidRequest;
        this.rule = rule;
    }

    public static class Builder {
        private String protocol;

        private ChannelHandlerContext nettyCtx;

        private Rule rule;
        private boolean keepAlive;
        private RapidRequest rapidRequest;

        public Builder() {

        }

        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setNettyCtx(ChannelHandlerContext nettyCtx) {
            this.protocol = protocol;
            return this;
        }

        public Builder setRule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder setRapidRequest(RapidRequest rapidRequest) {
            this.rapidRequest = rapidRequest;
            return this;
        }

        public RapidContext build() {
            return new RapidContext(protocol, nettyCtx, keepAlive, rapidRequest, rule);
        }
    }

    public <T> T getRequiredAttribute(AttributeKey<T> key) {
        T value = getAttribute(key);
        AssertUtil.notNull(value, "required attribute" + key);
        return value;
    }

    public <T> T getAttributeOrDefault(AttributeKey<T> key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }

    public Rule.FilterConfig getFilterConfig(String filterId) {
        return rule.getFilterConfig(filterId);
    }

    public String getUniqueId() {
        return rapidRequest.getUniqueId();
    }

    @Override
    public RapidRequest getRequest() {
        return rapidRequest;
    }

    @Override
    public RapidResponse getResponse() {
        return rapidResponse;
    }

    public RapidRequest getOriginRequest() {
        return rapidRequest;
    }

    public RapidRequest getRequestMutable() {
        return rapidRequest;
    }

    @Override
    public void setResponse(Object response) {
        this.rapidResponse = (RapidResponse) response;
    }

    @Override
    public Rule getRule() {
        return rule;
    }

    @Override
    public void releaseRequest() {
        if (requestReleased.compareAndSet(false, true)) {
            ReferenceCountUtil.release(rapidRequest.getFullHttpRequest());
        }
    }
}
