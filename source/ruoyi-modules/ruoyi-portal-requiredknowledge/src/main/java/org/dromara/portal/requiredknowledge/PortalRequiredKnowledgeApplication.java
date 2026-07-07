package org.dromara.portal.requiredknowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 应知应会服务 */
@SpringBootApplication
public class PortalRequiredKnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalRequiredKnowledgeApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("应知应会服务启动成功");
    }
}
