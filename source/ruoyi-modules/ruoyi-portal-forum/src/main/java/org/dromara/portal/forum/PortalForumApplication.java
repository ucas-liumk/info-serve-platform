package org.dromara.portal.forum;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 服务论坛服务 */
@EnableDubbo
@SpringBootApplication
public class PortalForumApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalForumApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("服务论坛服务启动成功");
    }
}
