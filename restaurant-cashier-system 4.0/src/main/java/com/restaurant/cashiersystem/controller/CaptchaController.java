package com.restaurant.cashiersystem.controller;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.CaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "验证码", description = "行为验证码生成")
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    @Autowired
    private ImageCaptchaApplication application;

    @Value("${captcha.tianai.enabled:true}")
    private boolean enabled;

    @Operation(summary = "生成滑块验证码")
    @PostMapping("/generate")
    public Result<CaptchaVO> generate() {
        if (!enabled) {
            return Result.success(null);
        }

        try {
            ApiResponse<ImageCaptchaVO> response =
                    application.generateCaptcha(CaptchaTypeConstant.SLIDER);

            ImageCaptchaVO vo = response.getData();
            CaptchaVO result = new CaptchaVO();
            result.setCaptchaId(vo.getId());
            result.setBackgroundImage(vo.getBackgroundImage());
            result.setSliderImage(vo.getTemplateImage());
            result.setBgWidth(vo.getBackgroundImageWidth());
            result.setBgHeight(vo.getBackgroundImageHeight());
            result.setSliderWidth(vo.getTemplateImageWidth());
            result.setSliderHeight(vo.getTemplateImageHeight());

            log.info("CAPTCHA 生成成功: id={}", vo.getId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("CAPTCHA 生成失败", e);
            return Result.error("验证码生成失败");
        }
    }
}
