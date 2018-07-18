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
//@Component
public class RabbitMQReceive {

    @RabbitListener(queues = "firstQueue")    //监听器监听指定的Queue
    public void reveive(String str) {
        System.out.println("Receive:" + str);
    }

    private String queueName = "firstQueue";

    private String routingkey = "first";

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private RabbitTemplate template;

    public void send() throws IOException, TimeoutException {

        System.out.println("开始发送消息....");

        AMQP.BasicProperties persistentTextPlain = com.rabbitmq.client.MessageProperties.PERSISTENT_TEXT_PLAIN;
        System.out.println("success:");
        template.convertAndSend("direct", routingkey, "send message by exchange and routingkey..");

        System.out.println("error:rountingkey");
        template.convertAndSend("direct", routingkey + "1", "send message by exchange and routingkey..");

        System.out.println("error:交换机");
        template.convertAndSend("direct1", routingkey, "send message by exchange and routingkey..");
    }

}
