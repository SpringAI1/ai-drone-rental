package com.drone.rental;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DroneRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroneRentalApplication.class, args);
        log.info("====================================");
        log.info("  无人机租赁系统启动成功！");
        log.info("  Swagger文档: http://localhost:8080/api/swagger-ui/index.html");
        log.info("====================================");
    }
}
