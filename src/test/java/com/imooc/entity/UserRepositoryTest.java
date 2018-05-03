package com.imooc.entity;

import com.imooc.ApplicationTests;
import com.imooc.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserRepositoryTest extends ApplicationTests {
    //注入我们要测试的jpa
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindOne() {
        //找到id=1的user
        User user = userRepository.findOne(1L);
        //判断name是否等于我们在数据库已经插入的waliwali
        Assert.assertEquals("waliwali",user.getName());
    }
}
