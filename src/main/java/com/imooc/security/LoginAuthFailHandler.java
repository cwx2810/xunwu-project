package com.imooc.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * 登录验证失败处理器
 * Created by 瓦力.
 */
public class LoginAuthFailHandler extends SimpleUrlAuthenticationFailureHandler {
    //引入入口识别类，好用来跳转失败时返回相应的错误信息
    private final LoginUrlEntryPoint urlEntryPoint;
    //构造入口识别类
    public LoginAuthFailHandler(LoginUrlEntryPoint urlEntryPoint) {
        this.urlEntryPoint = urlEntryPoint;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        //获取跳转的URL
        String targetUrl =
                this.urlEntryPoint.determineUrlToUseForThisRequest(request, response, exception);

        //对跳转失败进行处理
        targetUrl += "?" + exception.getMessage();
        super.setDefaultFailureUrl(targetUrl);
        super.onAuthenticationFailure(request, response, exception);
    }
}
