package com.imooc.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration//让这个类成为配置类
@EnableJpaRepositories(basePackages = "com.imooc.repository")//让它可以扫描到仓库类
@EnableTransactionManagement//允许事物管理
public class JPAConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")//设置前缀，在配置文件里配置数据源
    //建立数据源
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    //建立实体类的管理工厂
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        HibernateJpaVendorAdapter japVendor = new HibernateJpaVendorAdapter();//实例化hibernate
        japVendor.setGenerateDdl(false);//不产生ddl，就是不自动生成sql，我们手写
        //实例化实体映射管理工厂类
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        //设置一些属性，数据源，hibernate适配，扫描的实体类包名
        entityManagerFactory.setDataSource(dataSource());
        entityManagerFactory.setJpaVendorAdapter(japVendor);
        entityManagerFactory.setPackagesToScan("com.imooc.entity");
        return entityManagerFactory;
    }

    @Bean
    //事物管理
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
