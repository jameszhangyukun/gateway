package com.zyk.gateway.common.config;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 规则模型
 */
@Data
public class Rule implements Comparable<Rule>, Serializable {
    /**
     * 规则id
     */
    private String id;
    /**
     * 规则名称
     */
    private String name;
    /**
     * 规则协议
     */
    private String protocol;
    /**
     * 规则排序
     */
    private Integer order;

    private Set<FilterConfig> filterConfigs = new HashSet<>();

    public boolean addFilterConfig(FilterConfig filterConfig) {
        return filterConfigs.add(filterConfig);
    }

    public FilterConfig getFilterConfig(String id) {
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equals(id)) return filterConfig;
        }
        return null;
    }

    public boolean hasId(String id) {
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equals(id)) return true;
        }
        return false;
    }

    @Override
    public int compareTo(Rule o) {
        int orderCompare = Integer.compare(getOrder(), o.getOrder());
        if (orderCompare == 0) {
            return getId().compareTo(o.getId());
        }
        return orderCompare;
    }

    /**
     * 过滤器配置类
     */
    public static class FilterConfig {
        /**
         * 过滤器的唯一id
         */
        private String id;
        /**
         * json字符串
         */
        private String config;

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterConfig that = (FilterConfig) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
