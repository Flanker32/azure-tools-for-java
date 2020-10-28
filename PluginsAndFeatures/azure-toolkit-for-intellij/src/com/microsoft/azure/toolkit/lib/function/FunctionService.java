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
package com.microsoft.azure.toolkit.lib.function;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import com.microsoft.azure.toolkit.lib.webapp.WebAppService;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.telemetrywrapper.*;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployModel;

import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

public class FunctionService {
    private static final FunctionService instance = new FunctionService();

    public static FunctionService getInstance() {
        return FunctionService.instance;
    }

//    public WebApp createWebApp(final WebAppConfig config) throws Exception {
//        final FunctionDeployModel functionDeployModel = new FunctionDeployModel();
//
//        settings.setCreatingNew(true);
//        final Map<String, String> properties = settings.getTelemetryProperties(null);
//        final Operation operation = TelemetryManager.createOperation(WEBAPP, CREATE_WEBAPP);
//        try {
//            operation.start();
//            EventUtil.logEvent(EventType.info, operation, properties);
//            return AzureWebAppMvpModel.getInstance().createWebApp(settings);
//        } catch (final Exception e) {
//            EventUtil.logError(operation, ErrorType.userError, e, properties, null);
//            throw e;
//        } finally {
//            operation.complete();
//        }
//    }

//    public static FunctionDeployModel convertFunctionConfig2Model(){
//
//    }
}
