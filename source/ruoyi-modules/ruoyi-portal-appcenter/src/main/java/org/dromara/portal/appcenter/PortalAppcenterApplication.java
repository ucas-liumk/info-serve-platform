package org.dromara.portal.appcenter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 应用中心服务 */
@EnableDubbo
@SpringBootApplication
public class PortalAppcenterApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalAppcenterApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("应用中心服务启动成功");
    }
}
