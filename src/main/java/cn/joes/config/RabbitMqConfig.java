package cn.joes.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

/**
 * 如果消息没有到exchange,则confirm回调,ack=false
 * 如果消息到达exchange,则confirm回调,ack=true
 * exchange到queue成功,则不回调return
 * exchange到queue失败,则回调return(需设置mandatory=true,否则不回回调,消息就丢了)
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

    private String queueName = "firstQueue";

    private String routingkey = "first";

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
        //创建队列和交换机
        admin.declareQueue(new Queue(queueName));
        Exchange exchange;
        /*该交换机里面的三个参数分别为: 名字,持久化,是否自动删除*/
        admin.declareExchange(new DirectExchange("direct", false, false));
        Binding direct = BindingBuilder.bind(new Queue(queueName))
                .to(new DirectExchange("direct", true, false)).with(routingkey);
        admin.declareBinding(direct);
        return admin;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());

        //需设置mandatory=true,否则不回回调,消息就丢了
        template.setMandatory(true);

        template.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("消息确认 ===== ack,是否确认 : " + b + ", 错误原因 : " + s);
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

}
