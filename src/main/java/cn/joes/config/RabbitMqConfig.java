package cn.joes.config;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

/**
 *
 * RabbitProperties config = new RabbitProperties(); rabbitmq的配置参数实体类
 *
 * 发送消息的确认机制原则::
 *  (1)如果消息没有到exchange,则confirm回调,ack=false
 *  (2)如果消息到达exchange,则confirm回调,ack=true
 *  (3)exchange到queue成功,则不回调return
 *  (4)exchange到queue失败,则回调return(需设置mandatory=true,否则不回回调,消息就丢了)
 *
 * <p>
 * 测试的时候,原生的client,exchange错误的话,直接就报错了,是不会到confirmListener和returnListener的
 * <p>
 * <p>
 * Created by myijoes on 2018/7/16.
 */

@Configuration
@PropertySource(value = "classpath:application.properties")
public class RabbitMqConfig {

    @Value("${rabbitmq.host}")
    private String host;

    @Value("${rabbitmq.port}")
    private Integer port;

    @Value("${rabbitmq.username}")
    private String username;

    @Value("${rabbitmq.password}")
    private String password;

    /**
     * 通过 AbstractConnectionFactory 获取到内部 rabbitConnectionFactory
     *
     * @return
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        /**消息确认*/
        connectionFactory.setPublisherConfirms(true);
        /**消息回调*/
        connectionFactory.setPublisherReturns(true);
        /**消费者的ack方式为手动*/

        return connectionFactory;
    }

    /**
     * 定义了 AMQP 基础管理操作，主要是对各种资源（交换机、队列、绑定）的申明和删除操作。
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);

        //创建队列和交换机以及绑定

        /**
         * DIRECT
         *
         * direct : 通过路由键 消息将被投送到对应的队列(一对一)
         */
        admin.declareQueue(new Queue("Direct-Queue"));

        //该交换机里面的三个参数分别为: 名字,持久化,是否自动删除
        admin.declareExchange(new DirectExchange("Joe-Direct", false, false));

        Binding direct = BindingBuilder.bind(new Queue("Direct-Queue"))
                .to(new DirectExchange("Joe-Direct", true, false)).with("Direct-RoutingKey");
        admin.declareBinding(direct);

        /**
         * FANOUT
         *
         * 发布订阅模式(不存在路由键 将被投放到exchange对应的队列中)
         */
        admin.declareQueue(new Queue("Fanout-Queue-1"));
        admin.declareQueue(new Queue("Fanout-Queue-2"));
        admin.declareQueue(new Queue("Fanout-Queue-3"));

        admin.declareExchange(new FanoutExchange("Joe-Fanout", false, false));

        Binding fanout1 = BindingBuilder.bind(new Queue("Fanout-Queue-1"))
                .to(new FanoutExchange("Joe-Fanout", false, false));

        Binding fanout2 = BindingBuilder.bind(new Queue("Fanout-Queue-2"))
                .to(new FanoutExchange("Joe-Fanout", false, false));

        Binding fanout3 = BindingBuilder.bind(new Queue("Fanout-Queue-3"))
                .to(new FanoutExchange("Joe-Fanout", false, false));

        admin.declareBinding(fanout1);
        admin.declareBinding(fanout2);
        admin.declareBinding(fanout3);

        /**
         * Topic
         *
         * 可以使得不同源头的数据投放到一个队列中(order.log , order.id, purchase.log, purchase.id)
         *
         * 通过路由键的命名分类来进行筛选
         */
        admin.declareQueue(new Queue("Topic-Queue-1"));
        admin.declareQueue(new Queue("Topic-Queue-2"));
        admin.declareQueue(new Queue("Topic-Queue-3"));

        admin.declareExchange(new TopicExchange("Joe-Topic", false, false));

        Binding topic1 = BindingBuilder.bind(new Queue("Topic-Queue-1"))
                .to(new TopicExchange("Joe-Topic", false, false)).with("*.to");

        Binding topic2 = BindingBuilder.bind(new Queue("Topic-Queue-2"))
                .to(new TopicExchange("Joe-Topic", false, false)).with("log.*");

        Binding topic3 = BindingBuilder.bind(new Queue("Topic-Queue-3"))
                .to(new TopicExchange("Joe-Topic", false, false)).with("log1.to");

        admin.declareBinding(topic1);
        admin.declareBinding(topic2);
        admin.declareBinding(topic3);

        return admin;
    }

    /**
     * 该对象中配置的ConfirmCallback和ReturnCallback均针对的是消息发送这个阶段
     * ConfirmCallback在到达交换机前这个阶段起作用
     * ReturnCallback在交换机到队列这个阶段起作用
     *
     * 发送消息的确认机制原则::
     *  (1)如果消息没有到exchange,则confirm回调,ack=false
     *  (2)如果消息到达exchange,则confirm回调,ack=true
     *  (3)exchange到queue成功,则不回调return
     *  (4)exchange到queue失败,则回调return(需设置mandatory=true,否则不回回调,消息就丢了)
     *
     * @return RabbitTemplate
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());

        //需设置mandatory=true,否则不回回调,消息就丢了
        template.setMandatory(true);

        template.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("消息确认 ===== ack,是否确认 : " + b + ", 错误原因 : " + s );
                if (correlationData != null) {
                    System.out.println("correlationData : " + correlationData.toString());
                }
            }
        });

        template.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                System.out.println("消息回调机制 ====== i :" + i + " , s(错误信息) : " + s + " , s1(交换机) : " + s1 + ", s2(队列) :" + s2);
                if (message != null) {
                    System.out.println("消息回调机制: message :" + message.toString());
                }
            }
        });

        return template;
    }

    /**
     * 消费者的工厂类
     *
     * @return
     */
    @Bean(value = "myRabbitListenerContainerFactory")
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        /**开启手动 ack */
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        RabbitListenerEndpoint endpoint = new RabbitListenerEndpoint() {
            @Override
            public String getId() {
                return "id";
            }

            @Override
            public String getGroup() {
                return "group";
            }

            @Override
            public void setupListenerContainer(MessageListenerContainer messageListenerContainer) {
                messageListenerContainer.setupMessageListener(new ChannelAwareMessageListener() {
                    @Override
                    public void onMessage(Message message, Channel channel) throws Exception {
                        System.out.println("接收到的消息 message : " + message);
                        channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                        System.out.println("拒绝消息");
                    }
                });
            }
        };
        factory.createListenerContainer(endpoint);
        return factory;
    }

    /*@Bean(value = "myRabbitListenerContainer")
    public SimpleMessageListenerContainer simpleMessageListenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
        container.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                System.out.println("接收到的消息 message : " + message);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                System.out.println("拒绝消息");
            }
        });
        return container;
    }*/



}
