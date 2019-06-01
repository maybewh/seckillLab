package com.admin.controller;

import com.admin.bean.Goods;
import com.admin.bean.OrderInfo;
import com.admin.bean.SeckillOrder;
import com.admin.bean.User;
import com.admin.rabbitmq.MQSender;
import com.admin.rabbitmq.SeckillMessage;
import com.admin.redis.GoodsKey;
import com.admin.redis.RedisService;
import com.admin.result.CodeMsg;
import com.admin.result.Result;
import com.admin.service.GoodsService;
import com.admin.service.OrderService;
import com.admin.service.SeckillService;
import com.admin.vo.GoodsVo;
import com.google.common.util.concurrent.RateLimiter;
import com.sun.org.apache.bcel.internal.classfile.Code;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {
    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender sender;

    //基于令牌桶算法的限流实现类
    RateLimiter rateLimiter = RateLimiter.create(10);

    //做标记，判断该商品是否被处理过了
    private HashMap<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();

    /**'
     *  GET POST
     *  1. GET幂等，服务端获取数据，无论调用多少次结果都一样
     *  2. POST，向服务端提交数据，不是幂等
     *  将同步下单改为异步下单
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/do_seckill" ,method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> list(Model model, User user,
                                @RequestParam("goodsId")long goodsId) throws Exception {

        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
            return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
        }

        if (user == null)
            return Result.error(CodeMsg.SESSION_ERROR);
        model.addAttribute("user", user);

        //内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over){
            return Result.error(CodeMsg.SECKILL_OVER);
        }

        //预减库存
        long stock = redisService.decr(GoodsKey.getGoodsStock,""+goodsId);//10
        if (stock < 0){
            afterPropertiesSet();
            long stock2 = redisService.decr(GoodsKey.getGoodsStock, "" + goodsId);
            if (stock2 < 0){
                localOverMap.put(goodsId,true);
                return Result.error(CodeMsg.SECKILL_OVER);
            }
        }

        //判断重复秒杀
        SeckillOrder order = orderService.getSeckillOrderByUserIdGoodsId(user.getId(),goodsId);
        if (order != null){
            return Result.error(CodeMsg.REPEATE_SECKILL);
        }
        //入队
        SeckillMessage message = new SeckillMessage();
        message.setUser(user);
        message.setGoodsId(goodsId);
        sender.send(message);
        return Result.success(0); //排队中
    }

    /**
     * 系统初始化，将商品信息加载到redis和本地内存
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsVosList = goodsService.listGoodsVo();
        if (goodsVosList == null){
            return;
        }
        for (GoodsVo goods: goodsVosList
             ) {
            redisService.set(GoodsKey.getGoodsStock, "" + goods.getId(), goods.getStockCount());
            //初始化商品都是没有处理过的
            localOverMap.put(goods.getId(), false);
        }
    }

    /**
     * orderId:成功
     * -1 : 秒杀失败
     * 0 ： 排队中
     * @param model
     * @param user
     * @param goods
     * @return
     */
    @RequestMapping(value = "/result",method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> seckillResult(Model model,User user,
                                      @RequestParam("goodsId")long goods){
        model.addAttribute("user",user);
        if (user == null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long orderId = seckillService.getSeckillResult(user.getId(),goodsId);
        return Result.success(orderId);
    }
}
