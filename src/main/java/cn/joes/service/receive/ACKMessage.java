package cn.joes.service.receive;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

/**
 * Created by myijoes on 2018/7/18.
 */
//@Component
public class ACKMessage implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        System.out.println("接收到的消息 message : " + message);
        channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        System.out.println("拒绝消息");
    }
}
