package org.dromara.portal.kernel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/** 门户内核服务 */
@SpringBootApplication
public class PortalKernelApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PortalKernelApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("门户内核服务启动成功");
    }
}
