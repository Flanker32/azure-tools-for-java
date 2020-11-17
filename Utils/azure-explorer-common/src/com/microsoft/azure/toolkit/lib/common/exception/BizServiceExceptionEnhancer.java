/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@Aspect
public class BizServiceExceptionEnhancer {

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

    @AfterThrowing(pointcut = "bizAction()", throwing = "e")
    public void afterThrowBizAction(JoinPoint point, Throwable e) throws Throwable {
        if (!(e instanceof RuntimeException)) {
            // Do not handle checked exception
            throw e;
        }
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Method method = signature.getMethod();
        final Annotation annotation = method.getAnnotation(BizService.class);
        if (annotation != null) {
            final BizService serviceAnnotation = (BizService) annotation;
            final String messageTemplate = serviceAnnotation.value();
            final String[] parameters = serviceAnnotation.params();
            String[] params = Arrays.stream(parameters).map(parameter -> parseValue(parameter, point)).toArray(String[]::new);
            throw new AzureToolkitRuntimeException(String.format(messageTemplate, (Object[]) params), e);
        }
        throw e;
    }

    public static String parseValue(String expression, JoinPoint point) {
        final String fixedExpression = StringUtils.substring(expression, 1);
        final String parameterName = StringUtils.contains(expression, ".")
                ? StringUtils.substring(fixedExpression, 0, StringUtils.indexOf(fixedExpression, "."))
                : fixedExpression;
        Object object = null;
        if (StringUtils.startsWith(expression, "$")) {
            // process parameter
            final CodeSignature codeSignature = (CodeSignature) point.getSignature();
            final int parameterIndex = ArrayUtils.indexOf(codeSignature.getParameterNames(), parameterName);
            object = parameterIndex >= 0 ? point.getArgs()[parameterIndex] : null;
        } else if (StringUtils.startsWith(expression, "@")) {
            // member variables
            final Field variableField = FieldUtils.getField(point.getThis().getClass(), parameterName, true);
            try {
                if (variableField != null) {
                    object = variableField.get(point.getThis());
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // swallow exception while get variables
            }
        } else {
            return expression;
        }
        return object != null ? evalInline(fixedExpression, Collections.singletonMap(parameterName, object)) : null;
    }

    private static String evalInline(String expr, Map<String, Object> variableMap) {
        final JtwigTemplate template = JtwigTemplate.inlineTemplate(String.format("{{%s}}", expr));
        final JtwigModel model = JtwigModel.newModel();
        variableMap.entrySet().forEach((t) -> model.with(t.getKey(), t.getValue()));
        return template.render(model);
    }
}
