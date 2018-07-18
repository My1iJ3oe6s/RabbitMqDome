package cn.joes.service.receive;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by myijoes on 2018/7/16.
 */
@Component
public class RabbitMQReceive {

    @RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = "firstQueue", group = "group", id = "id")    //监听器监听指定的Queue
    public void reveive(String str) {
        System.out.println("Receive:" + str);
    }

}
