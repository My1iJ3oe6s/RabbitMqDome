package cn.joes.service.receive;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by myijoes on 2018/7/16.
 */
@Component
public class RabbitMQReceive {

    //@RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = "Direct-Queue", group = "group", id = "id")
    //监听器监听指定的Queue
    public void reveive(String str) {
        System.out.println("Direct Receive:  " + str);
    }

    @RabbitListener(queues = "Topic-Queue-3", exclusive = true)
    //监听器监听指定的Queue
    public void topicReceive(String str) {
        System.out.println("Topic Receive:  " + str);
    }

    /**
     * 在未创建队列和exchange的时候可以通过bindings来新建
     *
     * @param order
     * @param headers
     * @param channel
     * @throws Exception
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "order-queue", durable = "true"),
            exchange = @Exchange(value = "order-exchange", durable = "true", type = "topic"),
            key = "order.#"))
    @RabbitHandler
    public void onOrderMessage(@Payload String order, @Headers Map<String, Object> headers, Channel channel) throws Exception {
        System.out.println("--------------收到消息，开始消费------------");
        System.out.println("订单ID是：" + order);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        // ACK
        channel.basicAck(deliveryTag, false);
    }


}
