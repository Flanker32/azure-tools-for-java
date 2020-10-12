package com.microsoft.intellij.runner.webapp.webappconfig;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import org.apache.commons.lang3.StringUtils;

public class WebAppComboBoxModel {
    private boolean isNewCreateResource;
    private String subscriptionId;
    private String appName;
    private String resourceGroup;
    private String os;
    private String runtime;
    private String webappId;
    private WebAppSettingModel webAppSettingModel;
    private WebApp webApp;

    public WebAppComboBoxModel(ResourceEx<WebApp> resourceEx){
        final WebApp resource = resourceEx.getResource();
        this.webappId = resource.id();
        this.appName = resource.name();
        this.resourceGroup = resource.resourceGroupName();
        this.os = StringUtils.capitalize(resource.operatingSystem().toString());
        this.runtime = WebAppUtils.getJavaRuntime(resource);
        this.subscriptionId = resourceEx.getSubscriptionId();
        this.isNewCreateResource = false;
    }

    public WebAppComboBoxModel(WebAppSettingModel webAppSettingModel){
        this.appName = webAppSettingModel.getWebAppName();
        this.resourceGroup = webAppSettingModel.getResourceGroup();
        this.os = webAppSettingModel.getOS().name();
        this.runtime = webAppSettingModel.getOS() == OperatingSystem.LINUX ?
                       webAppSettingModel.getLinuxRuntime().toString() : webAppSettingModel.getWebContainer();
        this.subscriptionId = webAppSettingModel.getSubscriptionId();
        this.isNewCreateResource = true;
    }

    public boolean isNewCreateResource() {
        return isNewCreateResource;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getAppName() {
        return appName;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getOs() {
        return os;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getWebappId() {
        return webappId;
    }

    public WebAppSettingModel getWebAppSettingModel() {
        return webAppSettingModel;
    }

    public WebApp getWebApp() {
        return webApp;
    }
}
