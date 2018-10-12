package cn.joes.config;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.ImmediateAcknowledgeAmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.ConsumerTagStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import java.util.HashMap;
import java.util.Map;

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

        //设置忽略申明异常(避免创建重复队列以及创建异常的时候启动异常)
        admin.setIgnoreDeclarationExceptions(true);

        //创建队列和交换机以及绑定

        /**
         * DIRECT
         *
         * direct : 通过路由键 消息将被投送到对应的队列(一对一)
         */
        //设置死信队列 Dead Letter Exchange(消息过期或者失败都会发送到那边)
        Map<String, Object> queueProperties1 = new HashMap<String, Object>();
        queueProperties1.put("x-dead-letter-exchange","TTL-Direct1");
        queueProperties1.put("x-dead-letter-routing-key","TTL-Direct-failure");
        admin.declareQueue(new Queue("Direct-Queue", true, false, false, queueProperties1));
        //该交换机里面的三个参数分别为: 名字,持久化,是否自动删除
        //在声明交换机的时候若创建的name已经存在会导致创建RabbitAdmin失败
        admin.declareExchange(new DirectExchange("Joe-Direct", true, false));

        Binding direct = BindingBuilder.bind(new Queue("Direct-Queue"))
                .to(new DirectExchange("Joe-Direct", true, false)).with("Direct-RoutingKey");
        admin.declareBinding(direct);

        //设置包含过期时间的队列
        Map<String, Object> queueProperties = new HashMap<String, Object>();
        queueProperties.put("x-message-ttl", 10000);
        //设置队列最大的消息数量
        queueProperties.put("x-max-length", 5);
        // 设置队列可存放消息内容的最大字节长度为20
        queueProperties.put("x-max-length-bytes", 1000);
        //设置死信队列 Dead Letter Exchange(消息过期或者失败都会发送到那边)
        queueProperties.put("x-dead-letter-exchange","TTL-Direct1");
        queueProperties.put("x-dead-letter-routing-key","TTL-Direct-failure");

        Queue addFailureQueue = new Queue("TTL-Queue", true, false, false, queueProperties);
        admin.declareQueue(addFailureQueue);
        admin.declareExchange(new DirectExchange("TTL-Direct", true, false));
        Binding directTTL = BindingBuilder.bind(addFailureQueue)
                .to(new DirectExchange("TTL-Direct", true, false)).with("Direct-TTL");
        admin.declareBinding(directTTL);

        //死信队列
        Queue failureQueue = new Queue("TTL-Failure-Queue");
        admin.declareQueue(failureQueue);
        admin.declareExchange(new DirectExchange("TTL-Direct1", true, false));
        Binding directTTL1 = BindingBuilder.bind(failureQueue)
                .to(new DirectExchange("TTL-Direct1", true, false)).with("TTL-Direct-failure");
        admin.declareBinding(directTTL1);

        /**
         * FANOUT
         *
         * 发布订阅模式(不存在路由键 将被投放到exchange对应的队列中)
         */
        admin.declareQueue(new Queue("Fanout-Queue-1"));
        admin.declareQueue(new Queue("Fanout-Queue-2"));
        admin.declareQueue(new Queue("Fanout-Queue-3"));

        admin.declareExchange(new FanoutExchange("Joe-Fanout", true, false));

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

        //需设置mandatory=true,否则不回回调,消息就丢了(针对的是return这个环节(即交换机到队列))
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

    int count=0;

    /**
     * 消费方式一:使用容器的方式进行消费(相当于线程池,有消息的时候创建对应的消费者对象来处理)
     * 认识一个接口org.springframework.amqp.rabbit.listener.MessageListenerContainer，
     * 其默认实现类org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer。
     *
     * @return
     */
    @Bean(value = "myRabbitListenerContainer")
    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        //设置并发消费者的数量(MessageConsumer)
        container.setConcurrentConsumers(5);
        container.setMaxConcurrentConsumers(10);

        //设置消费的队列 , 队列可以是多个(参数是String的数组)
        container.setQueueNames("Direct-Queue", "TTL-Failure-Queue");

        /**
         * SimpleMessageListenerContainer的生命周期随着spring容器的启动而启动,关闭而关闭
         * 这里设置spring容器初始化的时候设置SimpleMessageListenerContainer不启动
         */
        //container.setAutoStartup(false);

        //设置消费者的consumerTag_tag
        ConsumerTagStrategy consumerTagStrategy = new ConsumerTagStrategy() {
            @Override
            public String createConsumerTag(String s) {
                return "newTag-" + s + "-" + (++count);
            }
        };
        container.setConsumerTagStrategy(consumerTagStrategy);


        //设置消费者的Arguments
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("module","订单模块");
        args.put("fun","发送消息");
        container.setConsumerArguments(args);

        //自动确认
        //container.setAcknowledgeMode(AcknowledgeMode.AUTO);

        //手动确认
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        container.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            //得到channel,可以对消息进行操作
            public void onMessage(Message message, Channel channel) throws Exception {
                System.out.println("消费方式一: 接收到的消息 message : " + message);

                //消费确认(拒绝)
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                //channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);

                //公平分发
                channel.basicQos(1);

                /**
                 * 当AcknowledgeMode=AUTO时出现异常通过这种方式抛出(不同的异常对应不同的情况(是否重新加入队列))
                 *
                 * 抛出NullPointerException异常则重新入队列
                 * throw new NullPointerException("消息消费失败");
                 * 当抛出的异常是AmqpRejectAndDontRequeueException异常的时候，则消息会被拒绝，且requeue=false
                 * throw new AmqpRejectAndDontRequeueException("消息消费失败");
                 * 当抛出ImmediateAcknowledgeAmqpException异常，则消费者会被确认
                 * throw new ImmediateAcknowledgeAmqpException("消息消费失败");
                 */
            }
        });

        //后置处理器，接收到的消息都添加了Header请求头
        container.setAfterReceivePostProcessors(new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().getHeaders().put("desc",10);
                return message;
            }
        });
        return container;
    }

}
