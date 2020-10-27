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

package com.microsoft.intellij.runner.functions.deploy;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.intellij.runner.functions.IntelliJFunctionContext;
import com.microsoft.intellij.runner.functions.IntelliJFunctionRuntimeConfiguration;

public class FunctionDeployModel extends IntelliJFunctionContext {

    private boolean isNewResource;
    private String functionId;

    public FunctionDeployModel() {

    }

    public FunctionDeployModel(final FunctionAppConfig functionAppConfig) {
        super();
        setSubscription(functionAppConfig.getSubscription().subscriptionId());
        setAppName(functionAppConfig.getName());
        setAppServicePlanName(functionAppConfig.getServicePlan().name());
        setAppServicePlanResourceGroup(functionAppConfig.getServicePlan().resourceGroupName());
        setPricingTier(functionAppConfig.getServicePlan().pricingTier().toSkuDescription().size());
        setRegion(functionAppConfig.getServicePlan().regionName());
        final IntelliJFunctionRuntimeConfiguration runtimeConfiguration = new IntelliJFunctionRuntimeConfiguration();
        runtimeConfiguration.setOs(
                functionAppConfig.getPlatform().getOs() == OperatingSystem.WINDOWS ? "windows" : "linux");
        runtimeConfiguration.setJavaVersion(functionAppConfig.getPlatform().getStackVersionOrJavaVersion());
        setRuntime(runtimeConfiguration);
    }

    public String getFunctionId() {
        return functionId;
    }

    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }

    public boolean isNewResource() {
        return isNewResource;
    }

    public void setNewResource(final boolean newResource) {
        isNewResource = newResource;
    }
}
