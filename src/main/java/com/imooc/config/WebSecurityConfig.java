package com.imooc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//import com.imooc.security.AuthFilter;
import com.imooc.security.AuthProvider;
//import com.imooc.security.LoginAuthFailHandler;
//import com.imooc.security.LoginUrlEntryPoint;

/**
 * Created by 瓦力.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * HTTP权限控制
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //http.addFilterBefore(authFilter(), UsernamePasswordAuthenticationFilter.class);

        // 资源访问权限
        http.authorizeRequests()
                //访问配置
                .antMatchers("/admin/login").permitAll() // 管理员登录入口
                .antMatchers("/static/**").permitAll() // 静态资源
                .antMatchers("/user/login").permitAll() // 用户登录入口
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/user/**").hasAnyRole("ADMIN", "USER")
                .antMatchers("/api/user/**").hasAnyRole("ADMIN",
                "USER")
                .and()//结尾
                //登录配置
                .formLogin()
                .loginProcessingUrl("/login") // 配置角色登录处理入口
                //.failureHandler(authFailHandler())
                .and();
//                .logout()
//                .logoutUrl("/logout")
//                .logoutSuccessUrl("/logout/page")
//                .deleteCookies("JSESSIONID")
//                .invalidateHttpSession(true)
//                .and()
//                .exceptionHandling()
//                .authenticationEntryPoint(urlEntryPoint())
//                .accessDeniedPage("/403");
        //关掉防御配置，加载同源策略
        http.csrf().disable();
        http.headers().frameOptions().sameOrigin();
    }

    /**
     * 自定义认证策略，实现登录验证
     */
    @Autowired
    public void configGlobal(AuthenticationManagerBuilder auth) throws Exception {
        //配置内存的用户名密码
//        auth.inMemoryAuthentication().withUser("admin").password("admin")
//                .roles("ADMIN").and();
        //验证用户登录同时擦除密码
        auth.authenticationProvider(authProvider()).eraseCredentials(true);
    }
    //注入验证用户登录
    @Bean
    public AuthProvider authProvider() {
        return new AuthProvider();
    }
//
//    @Bean
//    public LoginUrlEntryPoint urlEntryPoint() {
//        return new LoginUrlEntryPoint("/user/login");
//    }
//
//    @Bean
//    public LoginAuthFailHandler authFailHandler() {
//        return new LoginAuthFailHandler(urlEntryPoint());
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager() {
//        AuthenticationManager authenticationManager = null;
//        try {
//            authenticationManager =  super.authenticationManager();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return authenticationManager;
//    }
//
//    @Bean
//    public AuthFilter authFilter() {
//        AuthFilter authFilter = new AuthFilter();
//        authFilter.setAuthenticationManager(authenticationManager());
//        authFilter.setAuthenticationFailureHandler(authFailHandler());
//        return authFilter;
//    }
}
