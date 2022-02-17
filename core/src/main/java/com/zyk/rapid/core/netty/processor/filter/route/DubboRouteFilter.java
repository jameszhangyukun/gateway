package com.zyk.rapid.core.netty.processor.filter.route;

import com.zyk.gateway.common.config.DubboServiceInvoker;
import com.zyk.gateway.common.config.ServiceInvoker;
import com.zyk.gateway.common.constants.ProcessFilterConstants;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.DubboConnectException;
import com.zyk.gateway.common.exception.RapidResponseException;
import com.zyk.gateway.common.util.FastJsonConvertUtil;
import com.zyk.gateway.common.util.TimeUtil;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.GatewayConfigLoader;
import com.zyk.rapid.core.context.*;
import com.zyk.rapid.core.helper.DubboReferenceHelper;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.Filter;
import com.zyk.rapid.core.netty.processor.filter.FilterConfig;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterType;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
@Filter(id = ProcessFilterConstants.DUBBO_ROUTER_FILTER_ID,
        value = ProcessorFilterType.ROUTE,
        order = ProcessFilterConstants.DUBBO_ROUTER_FILTER_ORDER,
        name = ProcessFilterConstants.DUBBO_ROUTER_FILTER_NAME
)
@Slf4j
public class DubboRouteFilter extends AbstractEntryProcessorFilter<FilterConfig> {
    public DubboRouteFilter() {
        super(FilterConfig.class);
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        RapidContext rapidContext = (RapidContext) context;
        ServiceInvoker serviceInvoker = rapidContext.getRequiredAttribute(AttributeKey.DUBBO_INVOKER);
        DubboServiceInvoker dubboServiceInvoker = (DubboServiceInvoker) serviceInvoker;
        if (!HttpHeaderValues.APPLICATION_JSON.toString().equals(rapidContext.getOriginRequest().getContentType())) {
            rapidContext.terminated();
            throw new RapidResponseException(ResponseCode.DUBBO_PARAMETER_VALUE_ERROR);
        }
        String body = rapidContext.getOriginRequest().getBody();
        rapidContext.releaseRequest();
        List<Object> parameters = null;
        try {
            parameters = FastJsonConvertUtil.convertJSONToArray(body, Object.class);
        } catch (Exception e) {
            rapidContext.terminated();
            throw new RapidResponseException(ResponseCode.DUBBO_PARAMETER_VALUE_ERROR);
        }
        DubboRequest dubboRequest = DubboReferenceHelper.buildDubboRequest(dubboServiceInvoker, parameters.toArray());

        CompletableFuture<Object> future = DubboReferenceHelper.getInstance().$invokerAsync(rapidContext, dubboRequest);
        boolean whenComplete = GatewayConfigLoader.getGatewayConfig().isWhenComplete();
        if (whenComplete) {
            future.whenComplete((response, throwable) -> {
                complete(dubboServiceInvoker, response, throwable, rapidContext, args);
            });
        } else {
            future.whenCompleteAsync((response, throwable) -> {
                complete(dubboServiceInvoker, response, throwable, rapidContext, args);
            });
        }

    }

    public void complete(DubboServiceInvoker dubboServiceInvoker,
                         Object response,
                         Throwable throwable,
                         RapidContext rapidContext,
                         Object[] args) {
        try {
            rapidContext.setRRTime(TimeUtil.currentTimeMillis());
            if (Objects.nonNull(throwable)) {
                DubboConnectException dubboConnectException = new DubboConnectException(throwable, rapidContext.getUniqueId(), rapidContext.getOriginRequest().getPath(), dubboServiceInvoker.getInterfaceClass(), dubboServiceInvoker.getMethodName(),
                        ResponseCode.DUBBO_RESPONSE_ERROR);
                rapidContext.setThrowable(dubboConnectException);
            } else {
                RapidResponse rapidResponse = RapidResponse.buildRapidResponseObj(response);
                rapidContext.setResponse(rapidResponse);
            }
        } catch (Throwable t) {
            rapidContext.setThrowable(new RapidResponseException(ResponseCode.INTERNAL_ERROR));
            log.error("未知异常", t);
        } finally {
            rapidContext.writtened();
            try {
                super.fireNext(rapidContext, args);
            } catch (Throwable t) {
                rapidContext.setThrowable(new RapidResponseException(ResponseCode.INTERNAL_ERROR));
                log.error("fireNext", t);
            }
        }
    }
}
