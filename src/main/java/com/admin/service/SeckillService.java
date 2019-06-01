package com.admin.service;

import com.admin.bean.OrderInfo;
import com.admin.bean.SeckillOrder;
import com.admin.bean.User;
import com.admin.redis.SeckillKey;
import com.admin.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeckillService {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;

    /**
     * 保证减库存，下订单，写入秒杀订单 是一个事务
     * @param user
     * @param goods
     * @return
     */
    @Transactional
    public OrderInfo seckill(User user, GoodsVo goods) {
        //减库存 下订单 写入秒杀订单
        boolean success = goodsService.reduceStock(goods);
        if (success){
            return orderService.createOrder(user,goods);
        }else {
            setGoodsOver(goods.getId());
            return null;
        }
    }

    public long getSeckillResult(long userId, long goodsId) {
        SeckillOrder order = orderService.getSeckillOrderByUserIdGoodsId(userId,goodsId);
        if (order != null){
            return order.getGoodsId();
        }else {
            boolean isOver = getGoodsOver(goodsId);
            if (isOver){
                return -1;
            }else {
                return 0;
            }
        }
    }

    private boolean getGoodsOver(long goodsId) {
        return redisService.exists(SeckillKey.isGoodsOver,""+goodsId);
    }

    private void setGoodsOver(long goodsId){
        redisService.set(SeckillKey.isGoodsOver,""+ goodsId,true);
    }
}
