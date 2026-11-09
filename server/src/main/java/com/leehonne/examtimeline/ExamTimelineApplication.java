package com.leehonne.examtimeline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类（根包 com.leehonne.examtimeline）。
 * MyBatis-Plus：@MapperScan 扫描 mapper 包下的 BaseMapper 接口。
 */
@SpringBootApplication
@MapperScan("com.leehonne.examtimeline.mapper")
public class ExamTimelineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamTimelineApplication.class, args);
	}

}
