package org.dromara.portal.resources;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 资料共享服务 */
@EnableDubbo
@SpringBootApplication
public class PortalResourcesApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalResourcesApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("资料共享服务启动成功");
    }
}
