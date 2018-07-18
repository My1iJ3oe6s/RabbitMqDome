package cn.joes.controller;

import cn.joes.service.sender.RabbitMQSender;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by myijoes on 2018/7/16.
 */

public class RabbitMQController {

    @Autowired
    RabbitMQSender rabbitMQSender;


}
