package org.dromara.portal;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 门户业务服务（内核/应用中心/资料共享/服务论坛）
 */
@EnableDubbo
@SpringBootApplication
public class RuoYiPortalApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RuoYiPortalApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("门户业务服务模块启动成功");
    }
}
