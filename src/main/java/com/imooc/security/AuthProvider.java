package com.imooc.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.imooc.entity.User;
import com.imooc.service.IUserService;

/**
 * 自定义认证实现，验证用户登录信息
 *
 */
public class AuthProvider implements AuthenticationProvider {
    @Autowired
    private IUserService userService;//从user业务逻辑中取得用户

    private final Md5PasswordEncoder passwordEncoder = new Md5PasswordEncoder();//md5密码验证，判断取出的密码和输入的是否相等

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //获取输入的用户名密码
        String userName = authentication.getName();
        String inputPassword = (String) authentication.getCredentials();
        //从业务逻辑从UserRepository获取数据库中的用户名密码
        User user = userService.findUserByName(userName);
        //没有用户返回异常
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("authError");
        }
        //用md5判断密码是否相等
        if (this.passwordEncoder.isPasswordValid(user.getPassword(), inputPassword, user.getId())) {
            //如果验证通过，返回通过信息，三个参数，一个用户名，一个密码，一个该用户拥有的权限
            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        }
        //如果密码验证不通过，返回异常
        throw new BadCredentialsException("authError");

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }
}