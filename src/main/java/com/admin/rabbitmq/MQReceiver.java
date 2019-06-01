package com.admin.rabbitmq;

import com.admin.bean.SeckillOrder;
import com.admin.bean.User;
import com.admin.service.GoodsService;
import com.admin.service.OrderService;
import com.admin.service.RedisService;
import com.admin.service.SeckillService;
import com.admin.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQReceiver {

    private static Logger log = LoggerFactory.getLogger(MQReceiver.class);

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    SeckillService seckillService;

		@RabbitListener(queues=MQConfig.SECKILL_QUEUE)
		public void receive(String message) {
			log.info("receive message:"+message);

			SeckillMessage seckillMessage = RedisService.strToBean(message,SeckillMessage.class);
			User user = seckillMessage.getUser();
			long goodsId = seckillMessage.getGoodsId();

            GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
            int stock = goodsVo.getStockCount();
            if (stock <= 0){
                return;
            }

            //判断重复秒杀
            SeckillOrder order = orderService.getSeckillOrderByUserIdGoodsId(user.getId(),goodsId);
            if (order != null){
                return;
            }
            seckillService.seckill(user,goodsVo);
		}

		@RabbitListener(queues=MQConfig.TOPIC_QUEUE1)
		public void receiveTopic1(String message) {
			log.info(" topic  queue1 message:"+message);
		}

		@RabbitListener(queues=MQConfig.TOPIC_QUEUE2)
		public void receiveTopic2(String message) {
			log.info(" topic  queue2 message:"+message);
		}

		@RabbitListener(queues=MQConfig.HEADER_QUEUE)
		public void receiveHeaderQueue(byte[] message) {
			log.info(" header  queue message:"+new String(message));
		}



}
