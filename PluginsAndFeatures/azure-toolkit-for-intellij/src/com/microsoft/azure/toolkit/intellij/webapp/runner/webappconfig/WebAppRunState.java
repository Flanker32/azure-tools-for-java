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
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureRunProfileState;
import com.microsoft.intellij.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.webapp.runner.Constants;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebAppRunState extends AzureRunProfileState<WebAppBase> {
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
    public WebAppBase executeSteps(@NotNull RunProcessHandler processHandler
        , @NotNull Map<String, String> telemetryMap) throws Exception {
        File file = new File(getTargetPath());
        if (!file.exists()) {
            throw new FileNotFoundException(message("webapp.deploy.error.noTargetFile", file.getAbsolutePath()));
        }
        webAppConfiguration.setTargetName(file.getName());
        WebAppBase deployTarget = getDeployTargetByConfiguration(processHandler);
        // update settings
        if (MapUtils.isNotEmpty(webAppConfiguration.getApplicationSettings())) {
            updateApplicationSettings(deployTarget, processHandler);
        }
        WebAppUtils.deployArtifactsToAppService(deployTarget, file,
                webAppConfiguration.isDeployToRoot(), processHandler);
        return deployTarget;
    }

    private void updateApplicationSettings(WebAppBase deployTarget, RunProcessHandler processHandler) {
        if (deployTarget instanceof WebApp) {
            processHandler.setText("Updating Application Settings...");
            WebApp webApp = (WebApp) deployTarget;
            AzureWebAppMvpModel.getInstance().updateWebAppSettings(webAppSettingModel.getSubscriptionId(),
                    webApp.id(), webAppConfiguration.getApplicationSettings(), new HashSet<>());
            processHandler.setText("Updated Application Settings successfully.");
        } else if (deployTarget instanceof DeploymentSlot) {
            processHandler.setText("Updating Application Settings...");
            DeploymentSlot slot = (DeploymentSlot) deployTarget;
            AzureWebAppMvpModel.getInstance().updateDeploymentSlotAppSettings(webAppSettingModel.getSubscriptionId(),
                    slot.id(), slot.name(), webAppConfiguration.getApplicationSettings(), new HashSet<>());
            processHandler.setText("Updated Application Settings successfully.");
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
    protected void onSuccess(WebAppBase result, @NotNull RunProcessHandler processHandler) {
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
    private WebAppBase getDeployTargetByConfiguration(@NotNull RunProcessHandler processHandler) throws Exception {
        if (webAppSettingModel.isCreatingNew()) {
            final WebApp webapp = AzureWebAppMvpModel.getInstance().getWebAppByName(webAppSettingModel.getSubscriptionId(),
                                                                                    webAppSettingModel.getResourceGroup(),
                                                                                    webAppSettingModel.getWebAppName());
            if (webapp == null) {
                return createWebApp(processHandler);
            }
        }

        final WebApp webApp = AzureWebAppMvpModel.getInstance()
            .getWebAppById(webAppSettingModel.getSubscriptionId(), webAppSettingModel.getWebAppId());
        if (webApp == null) {
            processHandler.setText(message("appService.deploy.hint.failed"));
            throw new Exception(message("webapp.deploy.error.noWebApp"));
        }

        if (isDeployToSlot()) {
            if (webAppSettingModel.getSlotName() == Constants.CREATE_NEW_SLOT) {
                return createDeploymentSlot(processHandler);
            } else {
                return webApp.deploymentSlots().getByName(webAppSettingModel.getSlotName());
            }
        } else {
            return webApp;
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
        name = "webapp.create_detail",
        params = {"@webAppConfiguration.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private WebApp createWebApp(@NotNull RunProcessHandler processHandler) {
        processHandler.setText(message("webapp.deploy.hint.creatingWebApp"));
        try {
            return AzureWebAppMvpModel.getInstance().createWebApp(webAppSettingModel);
        } catch (final RuntimeException e) {
            processHandler.setText(message("webapp.deploy.error.noWebApp"));
            throw e;
        }
    }

    @AzureOperation(
        name = "webapp|deployment.create.state",
        params = {"@webAppConfiguration.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private DeploymentSlot createDeploymentSlot(@NotNull RunProcessHandler processHandler) {
        processHandler.setText(message("webapp.deploy.hint.creatingDeploymentSlot"));
        try {
            return AzureWebAppMvpModel.getInstance().createDeploymentSlot(webAppSettingModel);
        } catch (final RuntimeException e) {
            processHandler.setText(message("webapp.deploy.error.noWebApp"));
            throw e;
        }
    }

    @NotNull
    private String getUrl(@NotNull WebAppBase webApp, @NotNull String fileName, @NotNull String fileType) {
        String url = "https://" + webApp.defaultHostName();
        if (Comparing.equal(fileType, MavenConstants.TYPE_WAR) && !webAppSettingModel.isDeployToRoot()) {
            url += "/" + WebAppUtils.encodeURL(fileName.replaceAll("#", StringUtils.EMPTY)).replaceAll("\\+", "%20");
        }
        return url;
    }

    private void updateConfigurationDataModel(@NotNull WebAppBase app) {
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
