package com.admin.controller;

import com.admin.bean.OrderInfo;
import com.admin.bean.User;
import com.admin.redis.RedisService;
import com.admin.result.CodeMsg;
import com.admin.result.Result;
import com.admin.service.GoodsService;
import com.admin.service.OrderService;
import com.admin.service.UserService;
import com.admin.vo.GoodsVo;
import com.admin.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    OrderService orderService;

    @Autowired
    GoodsService goodsService;

    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> info(Model model, User user, @RequestParam("orderId") long orderId){
         if (user == null){
             return Result.error(CodeMsg.SESSION_ERROR);
         }

        OrderInfo orderInfo = orderService.getOrderById(orderId);

         if (orderInfo == null){
             return Result.error(CodeMsg.ORDER_NOT_EXIST);
         }
         long goodsId = orderInfo.getGoodsId();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo vo = new OrderDetailVo();
        vo.setOrder(orderInfo);
        vo.setGoods(goods);
        return Result.success(vo);
    }
}
