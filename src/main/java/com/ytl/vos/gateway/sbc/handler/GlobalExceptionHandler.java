package com.ytl.vos.gateway.sbc.handler;

import cn.hutool.core.util.ReUtil;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.vos.gateway.sbc.aspect.ControllerAspect;
import com.ytl.vos.gateway.sbc.dto.RouteRespDTO;
import com.ytl.vos.gateway.sbc.enums.VosErrCodeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.Field;
import java.rmi.UnexpectedException;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public RouteRespDTO error(Exception e){
        if (e.getCause() instanceof DuplicateKeyException) {
            return error((DuplicateKeyException) e.getCause());
        }
        log.error("发生未知异常", e);
        return errBack(e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public RouteRespDTO error(DuplicateKeyException e){
        String message = e.getCause().getMessage();
        if (message.contains("PRIMARY")) {
            message = "重复主键插入["+ReUtil.getGroup1("'(.*?)'", message)+"]";
        } else {
            message = "重复插入["+ReUtil.getGroup1("'(.*?)'", message)+"]";
        }
        log.error(message, e);
        return errBack(message);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public RouteRespDTO error(MethodArgumentNotValidException e) {
        return error((Exception) e, e.getBindingResult());
    }


    @ExceptionHandler(BindException.class)
    @ResponseBody
    public RouteRespDTO error(BindException e) {
        return error((Exception) e, e.getBindingResult());
    }

    @ExceptionHandler(UnexpectedException.class)
    @ResponseBody
    public RouteRespDTO error(UnexpectedException e){
        log.error("类型检查器异常", e);
        return errBack(e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public RouteRespDTO error(BusinessException e){
        if (e.getCode() != null && e.getCode().startsWith("SY")) {
            log.error(e.getMessage(), e);
        } else {
            log.error(e.getMessage());
        }
        return errBack(e);
    }

    private RouteRespDTO error(Exception e, BindingResult bindingResult) {
        StringBuilder errMsg = new StringBuilder();
        List<ObjectError> allErrors = bindingResult.getAllErrors();
        for (int i = 0; i < allErrors.size(); i++) {
            FieldError fieldError = (FieldError) allErrors.get(i);
            String showName = getShowName(fieldError);
            String defaultMessage = fieldError.getDefaultMessage();
            String reFixed = "长度需要在(\\d+)和\\1之间";
            String reMax = "长度需要在0和(\\d+)之间";
            if (ReUtil.isMatch(reFixed, defaultMessage)) {
                String maxLength = ReUtil.getGroup1(reFixed, defaultMessage);
                defaultMessage = "长度必须是" + maxLength + "位";
            } else if (ReUtil.isMatch(reMax, defaultMessage)) {
                String maxLength = ReUtil.getGroup1(reMax, defaultMessage);
                defaultMessage = "长度不能超过" + maxLength + "位";
            }
            if (i > 0) {
                errMsg.append(",");
            }
            errMsg.append(showName).append(defaultMessage);
        }
        log.error(errMsg.toString(), e);
        return errBack(new BusinessException(VosErrCodeEnum.Param_Validate_Error, errMsg.toString(), e));
    }

    private String getShowName(FieldError fieldError) {
        String voName = fieldError.getObjectName();
        String field = fieldError.getField();
        String showName = field;
        Class voClass = ControllerAspect.dtoClassMap.get(voName);
        if (voClass != null) {
            try {
                Field voField = voClass.getDeclaredField(field);
                showName = voField.getDeclaredAnnotation(ApiModelProperty.class).value();
            } catch (NoSuchFieldException e) {
                log.error("获取字段名称失败", e);
            }
        }
        return showName;
    }

    private RouteRespDTO errBack(BusinessException err) {
        RouteRespDTO respDTO = new RouteRespDTO();
        respDTO.setResult(false);
        respDTO.setReason(err.getMessage());
        return respDTO;
    }

    private RouteRespDTO errBack(String errMsg) {
        RouteRespDTO respDTO = new RouteRespDTO();
        respDTO.setResult(false);
        respDTO.setReason(errMsg);
        return respDTO;
    }


}
