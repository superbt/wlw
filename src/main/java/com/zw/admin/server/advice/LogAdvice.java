package com.zw.admin.server.advice;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.zw.admin.server.annotation.LogAnnotation;
import com.zw.admin.server.model.SysLogs;
import com.zw.admin.server.service.SysLogService;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一日志处理
 *
 * @author
 *
 *         2017年8月19日
 */
@Aspect
@Component
public class LogAdvice {

	@Autowired
	private SysLogService logService;

	@Around(value = "@annotation(com.zw.admin.server.annotation.LogAnnotation)")
	public Object logSave(ProceedingJoinPoint joinPoint) throws Throwable {
		SysLogs sysLogs = new SysLogs();
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
		String ip = request.getRemoteAddr();
		String module = null;
		LogAnnotation logAnnotation = methodSignature.getMethod().getDeclaredAnnotation(LogAnnotation.class);
		module = logAnnotation.module();
		if (StringUtils.isEmpty(module)) {
			ApiOperation apiOperation = methodSignature.getMethod().getDeclaredAnnotation(ApiOperation.class);
			if (apiOperation != null) {
				module = apiOperation.value();
			}
		}

		if (StringUtils.isEmpty(module)) {
			throw new RuntimeException("没有指定日志module....");
		}
		sysLogs.setModule(module);

		try {
			Object object = joinPoint.proceed();

			sysLogs.setFlag(true);
			sysLogs.setRemark((sysLogs.getRemark()==null?"":sysLogs.getRemark())+"请求ip:"+ip);
			logService.save(sysLogs);

			return object;
		} catch (Exception e) {
			sysLogs.setFlag(false);
			sysLogs.setRemark(e.getMessage());
			logService.save(sysLogs);
			throw e;
		}

	}
}
