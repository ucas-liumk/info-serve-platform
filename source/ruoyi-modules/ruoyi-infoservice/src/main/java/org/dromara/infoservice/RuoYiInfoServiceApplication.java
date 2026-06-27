package org.dromara.infoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 信息中心数智服务模块
 */
@SpringBootApplication
public class RuoYiInfoServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RuoYiInfoServiceApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("信息中心数智服务模块启动成功");
    }
}
