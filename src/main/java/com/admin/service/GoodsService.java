package com.admin.service;

import com.admin.bean.Goods;
import com.admin.bean.SeckillGoods;
import com.admin.mapper.GoodsMapper;
import com.admin.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {

    //乐观锁冲突最大重试次数
    private static final int DEFAULT_MAX_RETRIES = 5;

    @Autowired
    GoodsMapper goodsMapper;

    /**
     * 查询商品列表
     * @return
     */
    public List<GoodsVo> listGoodsVo() {
        return goodsMapper.listGoodsVo();
    }


    public GoodsVo getGoodsVoByGoodsId(long goodsId) {
        return goodsMapper.getGoodsVoByGoodsId(goodsId);
    }

    /**
     * 较少库存，每次减一
     * 通过版本号还实现乐观锁，还可以使用CAS
     * 自旋，重试次数不超过4，总共5次
     * @param goods
     * @return
     */
    public boolean reduceStock(GoodsVo goods){

        int numAttempts = 0;
        int ret = 0;
        SeckillGoods sg =  new SeckillGoods();
        sg.setGoodsId(goods.getId());
        sg.setVersion(goods.getVersion());
        do {
            numAttempts++;
            try {
                sg.setVersion(goodsMapper.getVersionByGoodsId(goods.getId()));
                ret = goodsMapper.reduceStockByVersion(sg);
            }catch (Exception e){
                e.printStackTrace();
            }
            if (ret != 0){
                break;
            }
        }while (numAttempts < DEFAULT_MAX_RETRIES);

        return ret > 0;
    }
}
