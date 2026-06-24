package org.dromara.appcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 应用中心模块
 */
@SpringBootApplication
public class RuoYiAppCenterApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RuoYiAppCenterApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  应用中心模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
