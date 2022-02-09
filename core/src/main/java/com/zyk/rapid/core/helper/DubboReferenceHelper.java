package com.zyk.rapid.core.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.zyk.gateway.common.config.DubboServiceInvoker;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.DubboConnectException;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.GatewayConfigLoader;
import com.zyk.rapid.core.balance.DubboLoadBalance;
import com.zyk.rapid.core.context.AttributeKey;
import com.zyk.rapid.core.context.DubboRequest;
import com.zyk.rapid.core.context.RapidContext;
import com.zyk.rapid.core.netty.processor.cache.DefaultCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.remoting.Constants.DISPATCHER_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.SHARE_CONNECTIONS_KEY;

public class DubboReferenceHelper {
    public static final String DUBBO_TRANSFER_CONTEXT = "DUBBO_TRANSFER_CONTEXT";

    public static final String APPLICATION_CONFIG_NAME = "rapid-consumer";

    public static final String APPLICATION_OWNER = "rapid";

    public static final String APPLICATION_ORGANIZATION = "rapid";
    public static final int DEFAULT_TIMEOUT = 5000;
    private ApplicationConfig applicationConfig;

    private volatile ReferenceConfigCache referenceConfigCache = ReferenceConfigCache.getCache();

    private final Cache<String, GenericService> cache = DefaultCacheManager.getInstance().createCacheForDubboGenericService();

    private DubboReferenceHelper() {
        this.applicationConfig = new ApplicationConfig(APPLICATION_CONFIG_NAME);
        this.applicationConfig.setOwner(APPLICATION_OWNER);
        this.applicationConfig.setOrganization(APPLICATION_ORGANIZATION);
    }

    private enum Singleton {

        INSTANCE;
        private DubboReferenceHelper dubboReferenceHelper;

        Singleton() {
            dubboReferenceHelper = new DubboReferenceHelper();
        }

        public DubboReferenceHelper getInstance() {
            return dubboReferenceHelper;
        }
    }

    public static DubboReferenceHelper getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    public static DubboRequest buildDubboRequest(DubboServiceInvoker dubboServiceInvoker, Object[] parameters) {
        DubboRequest dubboRequest = new DubboRequest();

        dubboRequest.setRegistriesStr(dubboServiceInvoker.getRegisterAddress());
        dubboRequest.setTimeout(dubboServiceInvoker.getTimeout());
        dubboRequest.setArgs(parameters);
        dubboRequest.setInterfaceClass(dubboServiceInvoker.getInterfaceClass());
        dubboRequest.setVersion(dubboServiceInvoker.getVersion());
        dubboRequest.setMethodName(dubboServiceInvoker.getMethodName());
        dubboRequest.setParameterTypes(dubboServiceInvoker.getParameterTypes());
        return dubboRequest;
    }

    public CompletableFuture<Object> $invokerAsync(RapidContext rapidContext, DubboRequest dubboRequest) {
        fillRpcContext(rapidContext);
        GenericService genericService = newGenericServiceForReg(dubboRequest.getRegistriesStr(), dubboRequest.getInterfaceClass(), dubboRequest.getTimeout(), dubboRequest.getVersion());
        try {
            return genericService.$invokeAsync(dubboRequest.getMethodName(), dubboRequest.getParameterTypes(), dubboRequest.getArgs());
        } catch (Exception e) {
            throw new DubboConnectException(e, rapidContext.getUniqueId(),
                    rapidContext.getOriginRequest().getPath(),
                    dubboRequest.getInterfaceClass(),
                    dubboRequest.getMethodName(),
                    ResponseCode.DUBBO_REQUEST_ERROR);
        }
    }


    private void fillRpcContext(RapidContext rapidContext) {
        // dubbo 调用负载均衡所需的参数
        RpcContext.getContext().set(DUBBO_TRANSFER_CONTEXT, rapidContext);

        // dubbo附加信息传递
        if (rapidContext.getAttribute(AttributeKey.DUBBO_ATTACHMENT) != null) {
            RpcContext.getContext().getAttachments().putAll(rapidContext.getAttribute(AttributeKey.DUBBO_ATTACHMENT));
        }
    }

    private GenericService newGenericServiceForReg(String registriesStr,
                                                   String interfaceClass,
                                                   int timeout,
                                                   String version) {
        String key = registriesStr + ":" + interfaceClass + ":" + version;
        GenericService genericService = cache.get(key, s -> {
            RegistryConfig registryConfig = new RegistryConfig();
            registryConfig.setAddress(registriesStr);
            registryConfig.setCheck(false);
            registryConfig.setTimeout(20000);
            if (registriesStr.indexOf("://") <= 0) {
                registryConfig.setProtocol("zookeeper");
            }
            return newGenericService(Arrays.asList(registryConfig), interfaceClass, timeout, version);
        });
        return genericService;

    }

    private GenericService newGenericService(List<RegistryConfig> registryConfigs,
                                             String interfaceClass,
                                             int timeout,
                                             String version) {
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        GatewayConfig gatewayConfig = GatewayConfigLoader.getGatewayConfig();
        int dubboConnections = gatewayConfig.getDubboConnections();

        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();

        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setRegistries(registryConfigs);
        referenceConfig.setInterface(interfaceClass);
        referenceConfig.setTimeout(timeout);
        referenceConfig.setGeneric("true");
        referenceConfig.setAsync(true);
        referenceConfig.setCheck(false);
        referenceConfig.setLoadbalance(DubboLoadBalance.NAME);

        referenceConfig.setParameters(new HashMap<>());
        referenceConfig.getParameters().put(DISPATCHER_KEY, "direct");
        referenceConfig.getParameters().put(SHARE_CONNECTIONS_KEY, String.valueOf(dubboConnections));
        if (StringUtils.isNoneBlank(version)) {
            referenceConfig.setVersion(version);
        }
        return referenceConfigCache.getCache().get(referenceConfig);

    }
}
