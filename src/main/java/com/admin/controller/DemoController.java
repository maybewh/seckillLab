package com.admin.controller;

import com.admin.bean.User;
import com.admin.rabbitmq.MQSender;
import com.admin.redis.RedisService;
import com.admin.result.Result;
import com.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    @RequestMapping("/hello")
    @ResponseBody
    public String hello(){
        return "hello world";
    }

    @RequestMapping("/db/get")
    @ResponseBody
    public Result<User> get() {
        User user = userService.getById(18181);
        return Result.success(user);
    }

    @RequestMapping("/mq")
    @ResponseBody
    public Result<String> mq() {
       mqSender.send("hello yeah");
        return Result.success("Hello world");
    }

/*    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<Long> redisGet() {
       Long v1 = redisService.get("key1", Long.class);
       return Result.success(v1);
    }

    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<String> redisSet() {
        redisService.set("key2","hello1");
        String v1 = redisService.get("key2",String.class);
        return Result.success(v1);
    }*/
}
