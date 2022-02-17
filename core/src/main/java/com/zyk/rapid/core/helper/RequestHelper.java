package com.zyk.rapid.core.helper;

import com.zyk.gateway.common.config.DynamicConfigManager;
import com.zyk.gateway.common.config.Rule;
import com.zyk.gateway.common.config.ServiceDefinition;
import com.zyk.gateway.common.config.ServiceInvoker;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.constants.RapidConstants;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.RapidNotFoundException;
import com.zyk.gateway.common.exception.RapidPathNoMatchedException;
import com.zyk.gateway.common.exception.RapidResponseException;
import com.zyk.gateway.common.util.AntPathMatcher;
import com.zyk.rapid.core.context.AttributeKey;
import com.zyk.rapid.core.context.RapidContext;
import com.zyk.rapid.core.context.RapidRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.zyk.gateway.common.constants.RapidProtocol.DUBBO;
import static com.zyk.gateway.common.constants.RapidProtocol.HTTP;

public class RequestHelper {

    public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static RapidContext doContext(FullHttpRequest request, ChannelHandlerContext ctx) {
        // 1. 构建请求对象RapidRequest
        RapidRequest rapidRequest = doRequest(request, ctx);

        // 2. 根据请求对象的uniqueId 获取资源信息
        ServiceDefinition serviceDefinition = getServiceDefinition(rapidRequest);

        // 3. 匹配路径
        if (!ANT_PATH_MATCHER.match(serviceDefinition.getPatternPath(), rapidRequest.getPath())) {
            throw new RapidPathNoMatchedException();
        }
        // 4. 根据请求对象获取服务定义对应的方法调用
        ServiceInvoker serviceInvoker = getServiceInvoker(rapidRequest, serviceDefinition);
        String ruleId = serviceInvoker.getRuleId();
        Rule rule = DynamicConfigManager.getInstance().getRule(ruleId);

        // 构建RapidContext对象
        RapidContext rapidContext = new RapidContext.Builder()
                .setProtocol(serviceDefinition.getProtocol())
                .setRapidRequest(rapidRequest)
                .setNettyCtx(ctx)
                .setRule(rule)
                .build();

        //	6. 设置SR:
        rapidContext.setSRTime(rapidRequest.getBeginTime());
        // 设置必要的上下文参数
        putContext(rapidContext, serviceInvoker);

        return rapidContext;
    }

    private static void putContext(RapidContext rapidContext, ServiceInvoker serviceInvoker) {
        switch (rapidContext.getProtocol()) {
            case HTTP:
                rapidContext.putAttribute(AttributeKey.HTTP_INVOKER, serviceInvoker);
            case DUBBO:
                rapidContext.putAttribute(AttributeKey.DUBBO_INVOKER, serviceInvoker);
        }
    }

    /**
     * 根据请求和服务定义对象获取ServiceInvoker
     *
     * @param rapidRequest
     * @param serviceDefinition
     */
    private static ServiceInvoker getServiceInvoker(RapidRequest rapidRequest, ServiceDefinition serviceDefinition) {
        Map<String, ServiceInvoker> serviceInvokerMap = serviceDefinition.getInvokerMap();
        ServiceInvoker serviceInvoker = serviceInvokerMap.get(rapidRequest.getPath());
        if (serviceInvoker == null) {
            throw new RapidNotFoundException(ResponseCode.SERVICE_INVOKER_NOT_FOUND);
        }
        return serviceInvoker;

    }

    private static ServiceDefinition getServiceDefinition(RapidRequest rapidRequest) {
        // ServiceDefinition从哪里获取，就是在网关服务加载的时候？
        ServiceDefinition serviceDefinition = DynamicConfigManager.getInstance().getServiceDefinition(rapidRequest.getUniqueId());
        if (serviceDefinition == null) {
            throw new RapidNotFoundException(ResponseCode.SERVICE_DEFINITION_NOT_FOUND);
        }
        return serviceDefinition;
    }


    public static RapidRequest doRequest(FullHttpRequest request, ChannelHandlerContext ctx) {
        // 1. 构建请求对象
        HttpHeaders headers = request.headers();
        String uniqueId = headers.get(RapidConstants.UNIQUE_ID);
        if (StringUtils.isBlank(uniqueId)) {
            throw new RapidResponseException(ResponseCode.REQUEST_PARSE_ERROR_NO_UNIQUEID);
        }

        String host = headers.get(HttpHeaderNames.HOST);
        HttpMethod method = request.getMethod();
        String uri = request.uri();
        String clientIp = getClientIp(ctx, request);
        String contentType = HttpUtil.getMimeType(request) == null ? null : HttpUtil.getMimeType(request).toString();
        Charset charset = HttpUtil.getCharset(request, StandardCharsets.UTF_8);

        RapidRequest rapidRequest = new RapidRequest(uniqueId,
                charset,
                clientIp,
                host,
                uri,
                method,
                contentType,
                headers,
                request);
        // 2. 根据请求对象的uniqueId获取资源服务信息

        return rapidRequest;
    }

    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);
        String clientIp = null;
        if (StringUtils.isNoneEmpty(xForwardedValue)) {
            List<String> values = Arrays.asList(xForwardedValue.split(","));
            if (values.size() >= 1 && StringUtils.isNoneBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }
        if (clientIp == null) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            clientIp = inetSocketAddress.getAddress().getHostAddress();
        }
        return clientIp;
    }
}
