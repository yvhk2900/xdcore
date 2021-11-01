package com.hzf.core.base;

import com.hzf.core.common.GlobalValues;
import com.hzf.core.common.SqlInfo;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class BaseApplication implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    public String[] GetScanPackages() {
        return new String[]{"com.hzf.core"};
    }

    @Bean //设置任务调度线程池,各自任务用各自的线程,一般池子的大小与任务数相同
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(20);
        GlobalValues.taskScheduler = taskScheduler;
        return taskScheduler;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        GlobalValues.baseAppliction = this;
        GlobalValues.checkDebug();
        factory.setPort(GlobalValues.CurrentPort);
    }

    public void ResetCache(SqlInfo su) throws Exception {

    }

    public void ScanClass(Class<?> aClass) {
    }

    //上传文件路径
    public String GetUploadFilePath() {
        return "uploadFiles";
    }

    //临时文件路径
    public String GetTempFilePath() {
        return "tempFiles";
    }

}
