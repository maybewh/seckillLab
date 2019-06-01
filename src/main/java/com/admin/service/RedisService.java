package com.admin.service;

import com.admin.redis.KeyPrefix;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;

    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return String.valueOf(value);
        } else if (clazz == long.class || clazz == Long.class) {
            return String.valueOf(value);
        } else if (clazz == String.class) {
            return (String) value;
        } else {
            return JSON.toJSONString(value);
        }

    }

    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            String str = jedis.get(realKey);
            T t = strToBean(str,clazz);
            return t;
        }finally {
            if(jedis != null){
                jedis.close();
            }
        }
    }

    public <T> boolean set(KeyPrefix prefix,String key,T value){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String str = beanToStr(value);
            if (str == null || str.length() <= 0)
                return false;

            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds(); //获取过期时间
            if(seconds <= 0){
                jedis.set(realKey,str);
                System.out.println(realKey + " " + str);
            }else {
                jedis.setex(realKey,seconds,str);
                System.out.println(realKey + " " + seconds + " "  + str);
            }
            return true;
        }finally {
            if(jedis != null){
                jedis.close();
            }
        }
    }

    /**
     * 删除
     * @param prefix
     * @param key
     * @return
     */
   public boolean delete(KeyPrefix prefix,String key){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            long ret = jedis.del(realKey);
            return ret > 0;
        }finally {
            if (jedis != null)
                jedis.close();
        }
   }

    /**
     * 判断Key是否存在
     * @param prefix
     * @param key
     * @param <T>
     * @return
     */
   public <T> boolean exists(KeyPrefix prefix,String key){
       Jedis jedis = null;
       try{
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
       }finally {
           if (jedis != null)
               jedis.close();
       }
   }

    private <T> String beanToStr(T value) {
        if (value == null)
            return null;
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class){
            return "" + value;
        }else if(clazz == String.class){
            System.out.println((String)value);
            return (String)value;
        }else if(clazz == long.class || clazz == Long.class){
            return "" + value;
        }else {
            return JSON.toJSONString(value);
        }

    }

    public static  <T> T strToBean(String str,Class<T> clazz) {
        if (str == null || str.length() <= 0 || clazz == null) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(str);
        } else if (clazz == long.class || clazz == Long.class) {
            return (T) Long.valueOf(str);
        } else if (clazz == String.class) {
            return (T) str;
        } else {
            return JSON.toJavaObject(JSON.parseObject(str), clazz);
        }
    }

    //减少值
    public <T> long decr(KeyPrefix keyPrefix, String key) {
       Jedis jedis = null;
       try{
           jedis = jedisPool.getResource();
           //生成真正的key
           String realKey = keyPrefix.getPrefix() + key;
           return jedis.decr(realKey);
       }finally {
           if (jedis != null) {
               jedis.close();
           }
       }
    }


}
