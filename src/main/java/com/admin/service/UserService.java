package com.admin.service;

import com.admin.bean.User;
import com.admin.exception.GlobalException;
import com.admin.mapper.UserMapper;
import com.admin.redis.RedisService;
import com.admin.redis.UserKey;
import com.admin.result.CodeMsg;
import com.admin.utils.MD5Util;
import com.admin.utils.UUIDUtil;
import com.admin.vo.LoginVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisService redisService;

    public static final String COOKIE_NAME_TOKEN = "token";

    public User getById(long id) {
        //对象缓存
        User user = redisService.get(UserKey.getById,""+id,User.class);
        if (user != null){
            return user;
        }
        user = userMapper.getById(id);
        //再存入缓存
        if (user != null){
            redisService.set(UserKey.getById,""+id,user);
        }
        return user;
    }

    public boolean updatePassword(String token,long id, String formPass){

        //取User
        User user = getById(id);
        if (user == null)
            throw  new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        //更新数据库
        User toBeUpdate = new User();
        toBeUpdate.setId(id);
        toBeUpdate.setPassword(formPass);
        userMapper.update(toBeUpdate);
        //更新缓存，先删除再插入
        redisService.delete(UserKey.getById,""+id);
        user.setPassword(toBeUpdate.getPassword());
        redisService.set(UserKey.token, token, user);
        return true;
    }


    public String login(HttpServletResponse response, LoginVo loginVo) {
        if(loginVo == null){
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }

        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();

        //判断手机号码是否存在
        User user = getById(Long.parseLong(mobile));

        if (user == null)
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);

        //验证密码
        String dbPass = user.getPassword();
        String dbSalt = user.getSalt();
        String calcPass = MD5Util.formPassToDBPass(formPass,dbSalt);
        if(!calcPass.equals(dbPass)){
            throw  new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        //生成唯一的id作为token
        String token = UUIDUtil.uuid();
        addCokie(response,token,user);
        return token;
    }

    /**
     * 将token作为key，用户信息作为value，存入redis模拟session，
     * 同时将token存入cookie保存登录状态
     * @param response
     * @param token
     * @param user
     */
    public void addCokie(HttpServletResponse response, String token, User user) {
        redisService.set(UserKey.token,token,user);
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN,token);
        cookie.setMaxAge(UserKey.token.expireSeconds());
        cookie.setPath("/"); //设置为网站的根目录
        response.addCookie(cookie);
    }

    public User getByToken(HttpServletResponse response,String token){
        if (StringUtils.isEmpty(token)){
            return null;
        }

        User user = redisService.get(UserKey.token,token,User.class);

        //延长有效期，有效期等于最后一次操作
        if(user != null){
            addCokie(response,token,user);
        }
        return user;
    }
}
