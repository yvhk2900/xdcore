package com.hzf.core.common;

import com.hzf.core.base.*;
import com.hzf.core.toolkit.ScanUtil;
import com.hzf.core.toolkit.TypeConvert;
import org.hibernate.dialect.Dialect;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.HashMap;
import java.util.Set;

/**
 *
 */
@Configuration
@EnableWebSocket //websocket
@EnableTransactionManagement
public class SystemSpringConfig extends WebMvcConfigurationSupport  {
    private static ApplicationContext applicationContext = null;
    private static final String FAVICON_URL = "/favicon.ico";

    //websocket start
//    @Autowired
//    private WebSocketHandler webSocketHandler;
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(webSocketHandler, "/wsapi")
//                .setAllowedOrigins("*");
//    }
    //websocket end

    /**
     * 发现如果继承了WebMvcConfigurationSupport，则在yml中配置的相关内容会失效。
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // !!!!!!!!如果一个通配符要映射多个路径不能重复add
        registry.addResourceHandler("/**").addResourceLocations("file:./static/", "classpath:/static/");
    }

    @Override
    protected void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/index.html");
    }

    /**
     * 配置servlet处理
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        HashMap<XInterceptor, Class<?>> inters = new HashMap<>();
        if(GlobalValues.baseAppliction==null){

        }
        String[] basePackages = GlobalValues.baseAppliction.GetScanPackages();
        for (String basePackage : basePackages) {
            Set<Class<?>> classes = ScanUtil.getClasses(basePackage);
            for (Class<?> aClass : classes) {
                GlobalValues.baseAppliction.ScanClass(aClass);
                if(BaseModel.class.isAssignableFrom(aClass)) {
                    SqlTable.CheckTable((Class<BaseModel>)aClass);
                }
                if(BaseModelController.class.isAssignableFrom(aClass)){
                    SqlCache.AddController(aClass);
                }
                if(BaseStatistic.class.isAssignableFrom(aClass)){
                    SqlCache.AddStatistic(aClass);
                }
                if(BaseTask.class.isAssignableFrom(aClass)){
                    SqlCache.AddTask(aClass);
                }
                if (HandlerInterceptor.class.isAssignableFrom(aClass)) {
                    XInterceptor systemInterceptor = aClass.getAnnotation(XInterceptor.class);
                    if (systemInterceptor != null) {
                        inters.put(systemInterceptor, aClass);
                    }
                }
            }
        }
        int i = 0;
        while (i < 99 && inters.size() > 0) {
            for (XInterceptor systemInterceptor : inters.keySet()) {
                if (systemInterceptor.priority() == i) {
                    Class<?> aClass = inters.remove(systemInterceptor);
                    try {
                        registry.addInterceptor((HandlerInterceptor) aClass.newInstance()).addPathPatterns(systemInterceptor.pathPatterns()).excludePathPatterns(FAVICON_URL);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            i++;
        }
    }


    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        if (SystemSpringConfig.applicationContext == null) {
            SystemSpringConfig.applicationContext = arg0;
        }
        super.setApplicationContext(arg0);
    }

    @Bean
    public Dialect getDialect() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        HibernateJpaVendorAdapter bean = SystemSpringConfig.getBean(HibernateJpaVendorAdapter.class);
        String dialect = TypeConvert.ToString(bean.getJpaPropertyMap().getOrDefault("hibernate.dialect", ""));
        return (Dialect) Class.forName(dialect).newInstance();
    }

    // 获取applicationContext
    public static ApplicationContext getStaticApplicationContext() {
        return applicationContext;
    }

    // 通过name获取 Bean.
    public static <T> T getBean(String name) {
        return (T) getStaticApplicationContext().getBean(name);
    }

    // 通过class获取Bean.
    public static <T> T getBean(Class<T> clazz) {
        return getStaticApplicationContext().getBean(clazz);
    }

    // 通过name,以及Clazz返回指定的Bean
    public static <T> T getBean(String name, Class<T> clazz) {
        return getStaticApplicationContext().getBean(name, clazz);
    }
}
