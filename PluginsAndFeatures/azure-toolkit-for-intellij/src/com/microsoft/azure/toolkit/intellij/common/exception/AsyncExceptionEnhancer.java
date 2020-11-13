package com.microsoft.azure.toolkit.intellij.common.exception;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AsyncExceptionEnhancer {

    @Pointcut("execution(@com.microsoft.azure.toolkit.lib.common.exception.BizService * *..*.*(..))")
    public void bizAction() {
    }

    @Around("execution(public * com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFileService.getFilesInDirectory(..))")
    public Object onGettingFilesInDirectory(ProceedingJoinPoint point) {
        try {
            return point.proceed();
        } catch (final Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

    @Before("bizAction()")
    public void aroundBizAction(JoinPoint point) {
        System.out.println(point.getSignature().toString());
    }
}
