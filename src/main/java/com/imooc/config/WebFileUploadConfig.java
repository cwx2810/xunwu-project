package com.imooc.config;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.MultipartProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.gson.Gson;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

/**
 * 文件上传配置
 *
 */
@Configuration
@ConditionalOnClass({Servlet.class, StandardServletMultipartResolver.class, MultipartConfigElement.class})
@ConditionalOnProperty(prefix = "spring.http.multipart", name = "enabled", matchIfMissing = true)
//让spring帮我们自动配置
@EnableConfigurationProperties(MultipartProperties.class)
public class WebFileUploadConfig {
    //设置自动配置接收
    private final MultipartProperties multipartProperties;
    //构造器，让springboot帮我们注入配置
    public WebFileUploadConfig(MultipartProperties multipartProperties) {
        this.multipartProperties = multipartProperties;
    }

    /**
     * 上传配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MultipartConfigElement multipartConfigElement() {
        return this.multipartProperties.createMultipartConfig();
    }

    /**
     * 注册解析器
     */
    @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(MultipartResolver.class)
    public StandardServletMultipartResolver multipartResolver() {
        //初始化一下
        StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
        //设置 一些属性
        multipartResolver.setResolveLazily(this.multipartProperties.isResolveLazily());
        return multipartResolver;
    }

//    /**
//     * 华东机房
//     */
//    @Bean
//    public com.qiniu.storage.Configuration qiniuConfig() {
//        return new com.qiniu.storage.Configuration(Zone.zone0());
//    }
//
//    /**
//     * 构建一个七牛上传工具实例
//     */
//    @Bean
//    public UploadManager uploadManager() {
//        return new UploadManager(qiniuConfig());
//    }
//
//    @Value("${qiniu.AccessKey}")
//    private String accessKey;
//    @Value("${qiniu.SecretKey}")
//    private String secretKey;
//
//    /**
//     * 认证信息实例
//     * @return
//     */
//    @Bean
//    public Auth auth() {
//        return Auth.create(accessKey, secretKey);
//    }
//
//    /**
//     * 构建七牛空间管理实例
//     */
//    @Bean
//    public BucketManager bucketManager() {
//        return new BucketManager(auth(), qiniuConfig());
//    }
//
//    @Bean
//    public Gson gson() {
//        return new Gson();
//    }
}
