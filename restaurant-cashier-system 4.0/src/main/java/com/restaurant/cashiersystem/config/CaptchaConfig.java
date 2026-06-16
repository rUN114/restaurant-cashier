package com.restaurant.cashiersystem.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * tianai-captcha 资源存储器配置
 * ImageCaptchaApplication 由 spring-boot-starter 自动创建，无需手动构建
 */
@Configuration
public class CaptchaConfig {

    @Bean
    public ResourceStore resourceStore() {
        LocalMemoryResourceStore store = new LocalMemoryResourceStore();

        // 背景图（600x360），为每种验证码类型都加上
        Resource bg = new Resource("classpath", "META-INF/cut-image/resource/1.jpg", "default");
        store.addResource(CaptchaTypeConstant.SLIDER, bg);
        store.addResource(CaptchaTypeConstant.ROTATE, bg);
        store.addResource(CaptchaTypeConstant.CONCAT, bg);
        store.addResource(CaptchaTypeConstant.WORD_IMAGE_CLICK, bg);

        return store;
    }
}
