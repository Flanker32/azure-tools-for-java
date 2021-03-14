/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.webapp.runner.Constants;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.RunProcessHandler;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebAppRunState extends AzureRunProfileState<IAppService> {
    private WebAppConfiguration webAppConfiguration;
    private final IntelliJWebAppSettingModel webAppSettingModel;

    /**
     * Place to execute the Web App deployment task.
     */
    public WebAppRunState(Project project, WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.webAppSettingModel = webAppConfiguration.getModel();
    }

    @Nullable
    @Override
    @AzureOperation(name = "webapp.deploy_artifact", params = {"@webAppConfiguration.getWebAppName()"}, type = AzureOperation.Type.ACTION)
    public IAppService executeSteps(@NotNull RunProcessHandler processHandler
        , @NotNull Map<String, String> telemetryMap) throws Exception {
        File file = new File(getTargetPath());
        if (!file.exists()) {
            throw new FileNotFoundException(message("webapp.deploy.error.noTargetFile", file.getAbsolutePath()));
        }
        webAppConfiguration.setTargetName(file.getName());
        final IAppService deployTarget = getDeployTargetByConfiguration(processHandler);
        updateApplicationSettings(deployTarget, processHandler);
        AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(deployTarget, file, webAppSettingModel.isDeployToRoot(), processHandler);
        return deployTarget;
    }

    private void updateApplicationSettings(IAppService deployTarget, RunProcessHandler processHandler) {
        final Map<String, String> applicationSettings = webAppConfiguration.getApplicationSettings();
        if (MapUtils.isEmpty(applicationSettings)) {
            return;
        }
        if (deployTarget instanceof IWebApp) {
            processHandler.setText("Updating application settings...");
            IWebApp webApp = (IWebApp) deployTarget;
            webApp.update().withAppSettings(applicationSettings).commit();
            processHandler.setText("Updated application settings successfully.");
        } else if (deployTarget instanceof IWebAppDeploymentSlot) {
            processHandler.setText("Updating deployment slot application settings...");
            AzureWebAppMvpModel.getInstance().updateDeploymentSlotAppSettings((IWebAppDeploymentSlot) deployTarget, applicationSettings);
            processHandler.setText("Updated deployment slot application settings successfully.");
        }
    }

    private boolean isDeployToSlot() {
        return !webAppSettingModel.isCreatingNew() && webAppSettingModel.isDeployToSlot();
    }

    @AzureOperation(name = "webapp.open_browser.state", type = AzureOperation.Type.ACTION)
    private void openWebAppInBrowser(String url, RunProcessHandler processHandler) {
        try {
            Desktop.getDesktop().browse(new URL(url).toURI());
        } catch (final IOException | URISyntaxException e) {
            processHandler.println(e.getMessage(), ProcessOutputTypes.STDERR);
        }
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP);
    }

    @Override
    @AzureOperation(name = "webapp.complete_starting.state", type = AzureOperation.Type.ACTION)
    protected void onSuccess(IAppService result, @NotNull RunProcessHandler processHandler) {
        if (webAppSettingModel.isCreatingNew() && AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
        }
        updateConfigurationDataModel(result);
        int indexOfDot = webAppSettingModel.getTargetName().lastIndexOf(".");
        final String fileName = webAppSettingModel.getTargetName().substring(0, indexOfDot);
        final String fileType = webAppSettingModel.getTargetName().substring(indexOfDot + 1);
        final String url = getUrl(result, fileName, fileType);
        processHandler.setText(message("appService.deploy.hint.succeed"));
        processHandler.setText("URL: " + url);
        if (webAppSettingModel.isOpenBrowserAfterDeployment()) {
            openWebAppInBrowser(url, processHandler);
        }
        processHandler.notifyComplete();
    }

    @Override
    protected String getDeployTarget() {
        return isDeployToSlot() ? "DeploymentSlot" : "WebApp";
    }

    @Override
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
        telemetryMap.put("SubscriptionId", webAppSettingModel.getSubscriptionId());
        telemetryMap.put("CreateNewApp", String.valueOf(webAppSettingModel.isCreatingNew()));
        telemetryMap.put("CreateNewSP", String.valueOf(webAppSettingModel.isCreatingAppServicePlan()));
        telemetryMap.put("CreateNewRGP", String.valueOf(webAppSettingModel.isCreatingResGrp()));
        telemetryMap.put("FileType", MavenRunTaskUtil.getFileType(webAppSettingModel.getTargetName()));
    }

    @NotNull
    private IAppService getDeployTargetByConfiguration(@NotNull RunProcessHandler processHandler) throws Exception {
        final AzureAppService azureAppService = AzureWebAppMvpModel.getInstance().getAzureAppServiceClient(webAppSettingModel.getSubscriptionId());
        final IWebApp webApp = getOrCreateAzureWebApp(azureAppService, processHandler);
        if (!isDeployToSlot()) {
            return webApp;
        }
        if (webAppSettingModel.getSlotName() == Constants.CREATE_NEW_SLOT) {
            return createDeploymentSlot(webApp, processHandler);
        } else {
            return webApp.deploymentSlot(webAppSettingModel.getSlotName());
        }
    }

    private IWebApp getOrCreateAzureWebApp(AzureAppService azureAppService, RunProcessHandler processHandler) throws Exception {
        final WebAppEntity entity = WebAppEntity.builder().id(webAppSettingModel.getWebAppId())
                                                .resourceGroup(webAppSettingModel.getResourceGroup())
                                                .name(webAppSettingModel.getWebAppName()).build();
        final IWebApp webApp = azureAppService.webapp(entity);
        if (webApp.exists()) {
            return webApp;
        }
        if (webAppSettingModel.isCreatingNew()) {
            processHandler.setText(message("webapp.deploy.hint.creatingWebApp"));
            return AzureWebAppMvpModel.getInstance().createWebAppFromSettingModel(webAppSettingModel);
        } else {
            processHandler.setText(message("appService.deploy.hint.failed"));
            throw new Exception(message("webapp.deploy.error.noWebApp"));
        }
    }

    @AzureOperation(
        name = "webapp|artifact.get.state",
        params = {"@webAppConfiguration.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private String getTargetPath() throws AzureExecutionException {
        final AzureArtifact azureArtifact =
                AzureArtifactManager.getInstance(project).getAzureArtifactById(webAppConfiguration.getAzureArtifactType(),
                                                                               webAppConfiguration.getArtifactIdentifier());
        if (Objects.isNull(azureArtifact)) {
            final String error = String.format("selected artifact[%s] not found", webAppConfiguration.getArtifactIdentifier());
            throw new AzureExecutionException(error);
        }
        return AzureArtifactManager.getInstance(project).getFileForDeployment(azureArtifact);
    }

    @AzureOperation(
        name = "webapp|deployment.create.state",
        params = {"@webAppConfiguration.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private IWebAppDeploymentSlot createDeploymentSlot(final IWebApp webApp,
                                                       @NotNull RunProcessHandler processHandler) {
        processHandler.setText(message("webapp.deploy.hint.creatingDeploymentSlot"));
        try {
            return AzureWebAppMvpModel.getInstance().createDeploymentSlotFromSettingModel(webApp, webAppSettingModel);
        } catch (final RuntimeException e) {
            processHandler.setText(message("webapp.deploy.error.noWebApp"));
            throw e;
        }
    }

    @NotNull
    private String getUrl(@NotNull IAppService webApp, @NotNull String fileName, @NotNull String fileType) {
        String url = "https://" + webApp.hostName();
        if (Comparing.equal(fileType, MavenConstants.TYPE_WAR) && !webAppSettingModel.isDeployToRoot()) {
            url += "/" + WebAppUtils.encodeURL(fileName.replaceAll("#", StringUtils.EMPTY)).replaceAll("\\+", "%20");
        }
        return url;
    }

    private void updateConfigurationDataModel(@NotNull IAppService app) {
        webAppSettingModel.setCreatingNew(false);
        // todo: add flag to indicate create new slot or not
        if (app instanceof DeploymentSlot) {
            webAppSettingModel.setSlotName(app.name());
            webAppSettingModel.setNewSlotConfigurationSource(Constants.DO_NOT_CLONE_SLOT_CONFIGURATION);
            webAppSettingModel.setNewSlotName("");
            webAppSettingModel.setWebAppId(((DeploymentSlot) app).parent().id());
        } else {
            webAppSettingModel.setWebAppId(app.id());
        }
        webAppSettingModel.setWebAppName("");
        webAppSettingModel.setResourceGroup("");
        webAppSettingModel.setAppServicePlanName("");
    }
}
