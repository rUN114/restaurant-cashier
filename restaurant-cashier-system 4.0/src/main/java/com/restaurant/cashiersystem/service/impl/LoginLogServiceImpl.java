package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.LoginLog;
import com.restaurant.cashiersystem.mapper.LoginLogMapper;
import com.restaurant.cashiersystem.service.LoginLogService;
import org.springframework.stereotype.Service;

/**
 * 登录日志实现
 */
@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

}