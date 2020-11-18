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

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public final class AzureOperationEnhancer {

    @Pointcut("execution(@AzureOperation * *..*.*(..))")
    public void operation() {
    }

    @Around("operation()")
    public Object aroundOperation(ProceedingJoinPoint point) throws Throwable {
        System.out.println("####################### start calling stack ############################");
        AzureOperationsContext.getOperations().forEach(o -> System.out.println(AzureOperationUtils.getOperationTitle(o)));
        final AzureOperationRef operation = toOperationRef(point);
        System.out.println(AzureOperationUtils.getOperationTitle(operation));
        System.out.println("####################### end calling stack ############################");
        return AzureOperationsContext.execute(operation, point::proceed);
    }

    @AfterThrowing(pointcut = "operation()", throwing = "e")
    public void onOperationException(JoinPoint point, Throwable e) throws Throwable {
        if (!(e instanceof RuntimeException)) {
            // Do not handle checked exception
            throw e;
        }
        final AzureOperationRef operation = toOperationRef(point);
        if (operation != null) {
            final String message = AzureOperationUtils.getOperationTitle(operation);
            throw new AzureToolkitRuntimeException(message, e);
        }
        throw e;
    }

    private static AzureOperationRef toOperationRef(JoinPoint point) {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Object[] args = point.getArgs();
        final Object instance = point.getThis();
        return AzureOperationRef.builder()
                                .instance(instance)
                                .method(signature.getMethod())
                                .paramNames(signature.getParameterNames())
                                .paramValues(args)
                                .build();
    }
}
