package cn.joes.service.sender;


import com.rabbitmq.client.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by myijoes on 2018/7/16.
 */

@Component
public class RabbitMQSender {

    private String queueName = "firstQueue";

    private String routingkey = "first";

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private RabbitTemplate template;

    public void send() throws IOException, TimeoutException {

        System.out.println("##### 开始发送消息....");

        System.out.println("##### send message to Direct : ");
        template.convertAndSend
                ("Joe-Direct", "Direct-RoutingKey", "send message to Joe-Direct(Direct) with Direct-RoutingKey(routingkey)..");

        System.out.println("##### send message to Fanout : ");
        template.convertAndSend
                ("Joe-Fanout", null, "send message to Joe-Fanout(Fanout) with null(routingkey)..");

        System.out.println("##### send message to topic : ");
        template.convertAndSend
                ("Joe-Topic", "log1.to", "send topic message to Joe-Topic(Topic) with log1.to(routing-key)..");


        //如何发送延迟消息

        //如何接收消息的时候时自动的

        //消费者的确认

        //
    }
}
