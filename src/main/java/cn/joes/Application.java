package cn.joes;

import cn.joes.service.sender.RabbitMQSender;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
@EnableRabbit
public class Application {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext run = SpringApplication.run(Application.class, args);
		RabbitMQSender bean = run.getBean(RabbitMQSender.class);
		bean.send();
	}
}