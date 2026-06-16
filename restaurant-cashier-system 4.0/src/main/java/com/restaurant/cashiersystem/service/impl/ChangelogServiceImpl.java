package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Changelog;
import com.restaurant.cashiersystem.mapper.ChangelogMapper;
import com.restaurant.cashiersystem.service.ChangelogService;
import org.springframework.stereotype.Service;

@Service
public class ChangelogServiceImpl extends ServiceImpl<ChangelogMapper, Changelog> implements ChangelogService {
}
