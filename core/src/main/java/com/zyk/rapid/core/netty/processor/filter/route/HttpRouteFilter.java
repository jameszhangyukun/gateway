package com.zyk.rapid.core.netty.processor.filter.route;

import com.zyk.gateway.common.constants.ProcessFilterConstants;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.RapidConnectException;
import com.zyk.gateway.common.exception.RapidResponseException;
import com.zyk.gateway.common.util.TimeUtil;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.GatewayConfigLoader;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.context.RapidContext;
import com.zyk.rapid.core.context.RapidResponse;
import com.zyk.rapid.core.helper.AsyncHttpHelper;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.Filter;
import com.zyk.rapid.core.netty.processor.filter.FilterConfig;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterType;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Http请求路由的中置过滤器
 */
@Filter(id = ProcessFilterConstants.HTTP_ROUTER_FILTER_ID,
        value = ProcessorFilterType.ROUTE,
        order = ProcessFilterConstants.HTTP_ROUTER_FILTER_ORDER,
        name = ProcessFilterConstants.HTTP_ROUTER_FILTER_NAME
)
@Slf4j
public class HttpRouteFilter extends AbstractEntryProcessorFilter<FilterConfig> {
    public HttpRouteFilter() {
        super(FilterConfig.class);
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        RapidContext rapidContext = (RapidContext) context;
        //	设置RS:
        rapidContext.setRSTime(TimeUtil.currentTimeMillis());

        Request request = rapidContext.getRequestMutable().build();
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
        // 双异步和单异步
        boolean whenComplete = GatewayConfigLoader.getGatewayConfig().isWhenComplete();
        // 单异步
        if (whenComplete) {
            future.whenComplete((response, throwable) -> {
                complete(request, response, throwable, rapidContext, args);
            });
        } else {
            // 双异步
            future.whenCompleteAsync((response, throwable) -> {
                complete(request, response, throwable, rapidContext, args);
            });
        }
    }


    /**
     * 执行请求响应返回的方法
     *
     * @param request
     * @param response
     * @param throwable
     * @param rapidContext
     */
    private void complete(Request request, Response response, Throwable throwable, RapidContext rapidContext, Object... args) {
        try {
            rapidContext.setRRTime(TimeUtil.currentTimeMillis());
            // 1. 释放请求资源
            rapidContext.releaseRequest();
            // 2. 判断是否产生异常
            if (Objects.nonNull(throwable)) {
                String url = request.getUrl();
                if (throwable instanceof TimeoutException) {
                    log.warn("#HttpRouteFilter# complete返回响应执行，请求路径：{}，耗时多少：{} 超时多少：{}",
                            url,
                            request.getRequestTimeout() == 0 ? GatewayConfigLoader.getGatewayConfig().getHttpRequestTimeout()
                                    : request.getRequestTimeout(),
                            request.getRequestTimeout());

                    // 网关设置异常
                    rapidContext.setThrowable(new RapidResponseException(ResponseCode.REQUEST_TIMEOUT));
                } else {
                    rapidContext.setThrowable(new RapidConnectException(throwable, rapidContext.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                // 设置会写标记
                rapidContext.writtened();
                // 设置响应信息
                rapidContext.setResponse(RapidResponse.buildRapidResponse(response));
            }
        } catch (Throwable t) {
            rapidContext.setThrowable(new RapidResponseException(ResponseCode.INTERNAL_ERROR));
            log.error("#HttpRouteFilter# complete catch 未知异常", t);
        } finally {
            try {
                // 让线程自己内部触发下一个节点执行
                fireNext(rapidContext, args);
            } catch (Throwable t) {
                // 兜底处理，把异常信息放入上下文
                log.error("#HttpRouteFilter# fireNext 出现异常", t);
                rapidContext.setThrowable(new RapidResponseException(ResponseCode.INTERNAL_ERROR));
            }
        }
    }
}
