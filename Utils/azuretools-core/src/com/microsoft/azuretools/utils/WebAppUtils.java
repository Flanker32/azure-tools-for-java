/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;


import com.microsoft.applicationinsights.web.dependencies.apachecommons.io.FileUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebAppUtils {

    public static final String TYPE_WAR = "war";
    public static final String TYPE_JAR = "jar";

    private static final String TEMP_FILE_PREFIX = "azuretoolkit";
    private static final String TEMP_FOLDER_PREFIX = "azuretoolkitstagingfolder";
    private static final String FTP_ROOT_PATH = "/site/wwwroot/";
    private static final String FTP_WEB_APPS_PATH = FTP_ROOT_PATH + "webapps/";
    private static final String WEB_CONFIG_FILENAME = "web.config";
    private static final String NO_TARGET_FILE = "Cannot find target file: %s.";
    private static final String ROOT = "ROOT";
    private static final String JAVASE_ROOT = "app";
    private static final String JAVASE_ARTIFACT_NAME = "app.jar";
    private static final int FTP_MAX_TRY = 3;
    private static final int DEPLOY_MAX_TRY = 3;
    private static final int SLEEP_TIME = 5000; // milliseconds
    private static final String DEFAULT_VALUE_WHEN_VERSION_INVALID = "";

    public static final String STOP_WEB_APP = "Stopping web app...";
    public static final String STOP_DEPLOYMENT_SLOT = "Stopping deployment slot...";
    public static final String DEPLOY_SUCCESS_WEB_APP = "Deploy succeed, restarting web app...";
    public static final String DEPLOY_SUCCESS_DEPLOYMENT_SLOT = "Deploy succeed, restarting deployment slot...";
    public static final String RETRY_MESSAGE = "Exception occurred while deploying to app service:" +
            " %s, retrying immediately (%d/%d)";
    public static final String RETRY_FAIL_MESSAGE = "Failed to deploy after %d times of retry.";
    public static final String COPYING_RESOURCES = "Copying resources to staging folder...";

    @NotNull
    @AzureOperation(
        name = "webapp|artifact.prepare_ftp",
        type = AzureOperation.Type.TASK
    )
    public static FTPClient getFtpConnection(PublishingProfile pp) throws IOException {
        System.out.println("\t\t" + pp.ftpUrl());
        System.out.println("\t\t" + pp.ftpUsername());
        System.out.println("\t\t" + pp.ftpPassword());

        FTPClient ftp = new FTPClient();
        URI uri = URI.create("ftp://" + pp.ftpUrl());
        ftp.connect(uri.getHost(), 21);
        final int replyCode = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            ftp.disconnect();
            throw new ConnectException("Unable to connect to FTP server");
        }

        if (!ftp.login(pp.ftpUsername(), pp.ftpPassword())) {
            throw new ConnectException("Unable to login to FTP server");
        }

        ftp.setControlKeepAliveTimeout(Constants.connection_read_timeout_ms);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode(); //Switch to passive mode

        return ftp;
    }

    public static int deployArtifact(String artifactName, String artifactPath, PublishingProfile pp,
                                     boolean toRoot, IProgressIndicator indicator) throws IOException {
        File file = new File(artifactPath);
        if (!file.exists()) {
            throw new FileNotFoundException(String.format(NO_TARGET_FILE, artifactPath));
        }
        FTPClient ftp = null;
        InputStream input = null;
        int uploadingTryCount = 0;
        try {
            if (indicator != null) {
                indicator.setText("Connecting to FTP server...");
            }

            ftp = getFtpConnection(pp);
            ensureWebAppsFolderExist(ftp);
            if (indicator != null) {
                indicator.setText("Uploading the application...");
            }
            input = new FileInputStream(artifactPath);
            int indexOfDot = artifactPath.lastIndexOf(".");
            String fileType = artifactPath.substring(indexOfDot + 1);

            switch (fileType) {
                case TYPE_WAR:
                    if (toRoot) {
                        WebAppUtils.removeFtpDirectory(ftp, FTP_WEB_APPS_PATH + ROOT, indicator);
                        ftp.deleteFile(FTP_WEB_APPS_PATH + ROOT + "." + TYPE_WAR);
                        uploadingTryCount = uploadFileToFtp(ftp, FTP_WEB_APPS_PATH + ROOT + "." + TYPE_WAR, input, indicator);
                    } else {
                        WebAppUtils.removeFtpDirectory(ftp, FTP_WEB_APPS_PATH + artifactName, indicator);
                        ftp.deleteFile(artifactName + "." + TYPE_WAR);
                        uploadingTryCount = uploadFileToFtp(ftp, FTP_WEB_APPS_PATH + artifactName + "." + TYPE_WAR, input, indicator);
                    }
                    break;
                case TYPE_JAR:
                    uploadingTryCount = uploadFileToFtp(ftp, FTP_ROOT_PATH + ROOT + "." + TYPE_JAR, input, indicator);
                    break;
                default:
                    break;
            }
            if (indicator != null) {
                indicator.setText("Logging out of FTP server...");
            }
            ftp.logout();
        } finally {
            if (input != null) {
                input.close();
            }
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
        return uploadingTryCount;
    }

    public static int deployArtifactForJavaSE(String artifactPath, PublishingProfile pp, IProgressIndicator indicator) throws IOException {
        File file = new File(artifactPath);
        if (!file.exists()) {
            throw new FileNotFoundException(String.format(NO_TARGET_FILE, artifactPath));
        }
        FTPClient ftp = null;
        int uploadingTryCount;
        try (InputStream input = new FileInputStream(artifactPath)) {
            if (indicator != null) {
                indicator.setText("Connecting to FTP server...");
            }
            ftp = getFtpConnection(pp);
            if (indicator != null) {
                indicator.setText("Uploading the application...");
            }
            uploadingTryCount = uploadFileToFtp(ftp, FTP_ROOT_PATH + JAVASE_ROOT + "." + TYPE_JAR, input, indicator);
            if (indicator != null) {
                indicator.setText("Logging out of FTP server...");
            }
            ftp.logout();
        } finally {
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
        return uploadingTryCount;
    }

    private static void ensureWebAppsFolderExist(FTPClient ftp) {
        int count = 0;
        while (count++ < FTP_MAX_TRY) {
            try {
                ftp.getStatus(FTP_WEB_APPS_PATH);
                if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                    return;
                }
                if (ftp.makeDirectory(FTP_WEB_APPS_PATH)) {
                    return;
                }
                Thread.sleep(SLEEP_TIME);
            } catch (final Exception e) {
            }
        }
        final String error = "failed to make directory at server with error code: " + ftp.getReplyCode();
        throw new AzureToolkitRuntimeException(error);
    }

    public static void removeFtpDirectory(FTPClient ftpClient, String path, IProgressIndicator pi) throws IOException {
        String prefix = "Removing from FTP server: ";
        FTPFile[] subFiles = ftpClient.listFiles(path);
        if (subFiles.length > 0) {
            for (FTPFile ftpFile : subFiles) {
                if (pi != null && pi.isCanceled()) {
                    break;
                }
                String currentFileName = ftpFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    continue; // skip
                }

                String path1 = path + "/" + currentFileName;
                if (ftpFile.isDirectory()) {
                    // remove the sub directory
                    removeFtpDirectory(ftpClient, path1, pi);
                } else {
                    // delete the file
                    if (pi != null) {
                        pi.setText2(prefix + path1);
                    }
                    ftpClient.deleteFile(path1);
                }
            }
        }

        if (pi != null) {
            pi.setText2(prefix + path);
        }
        ftpClient.removeDirectory(path);
        if (pi != null) {
            pi.setText2("");
        }
    }

    public static boolean doesRemoteFileExist(FTPClient ftp, String path, String fileName) throws IOException {
        FTPFile[] files = ftp.listFiles(path);
        for (FTPFile file : files) {
            if (file.isFile() && file.getName().equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doesRemoteFolderExist(FTPClient ftp, String path, String folderName) throws IOException {
        FTPFile[] files = ftp.listFiles(path);
        for (FTPFile file : files) {
            if (file.isDirectory() && file.getName().equalsIgnoreCase(folderName)) {
                return true;
            }
        }
        return false;
    }

    public static int sendGet(String sitePath) throws IOException {
        URL url = new URL(sitePath);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setReadTimeout(Constants.connection_read_timeout_ms);
        return con.getResponseCode();
        //con.setRequestProperty("User-Agent", "AzureTools for Intellij");
    }

    public static boolean isUrlAccessible(String url) throws IOException {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("HEAD");
        con.setReadTimeout(Constants.connection_read_timeout_ms);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        } finally {
            con.disconnect();
        }
        return true;
    }

    /**
     * Deploys artifact to Azure App Service
     *
     * @param deployTarget      the web app or deployment slot
     * @param artifact          artifact to deploy
     * @param isDeployToRoot
     * @param progressIndicator
     */
    @AzureOperation(
        name = "webapp|artifact.upload",
        params = {"$artifact.getName()", "$deployTarget.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public static void deployArtifactsToAppService(WebAppBase deployTarget
            , File artifact, boolean isDeployToRoot, IProgressIndicator progressIndicator) {
        if (!(deployTarget instanceof WebApp || deployTarget instanceof DeploymentSlot)) {
            final String error = "the deployment target is not a valid (deployment slot of) Web App";
            final String action = "select a valid Web App or deployment slot to deploy the artifact";
            throw new AzureToolkitRuntimeException(error, action);
        }
        // stop target app service
        String stopMessage = deployTarget instanceof WebApp ? STOP_WEB_APP : STOP_DEPLOYMENT_SLOT;
        progressIndicator.setText(stopMessage);
        deployTarget.stop();
        // deploy with zip/war deploy according to file type
        boolean deployResult = isJarBaseOnFileName(artifact.getPath()) ?
                deployWebAppToJavaSERuntime(deployTarget, artifact, progressIndicator) :
                deployWebAppToWebContainer(deployTarget, artifact, isDeployToRoot, progressIndicator);
        if (deployResult) {
            String successMessage = deployTarget instanceof WebApp ?
                    DEPLOY_SUCCESS_WEB_APP : DEPLOY_SUCCESS_DEPLOYMENT_SLOT;
            progressIndicator.setText(successMessage);
            deployTarget.start();
        }
    }

    private static boolean isJarBaseOnFileName(String filePath) {
        int index = filePath.lastIndexOf(".");
        if (index < 0) {
            return false;
        }
        return filePath.substring(index + 1).equals(TYPE_JAR);
    }

    public static boolean deployWebAppToJavaSERuntime(WebAppBase deployTarget
            , File artifact, IProgressIndicator progressIndicator) {
        try {
            File zipPackage = prepareZipPackage(deployTarget, artifact, progressIndicator);
            int retryCount = 0;
            while (retryCount++ < DEPLOY_MAX_TRY) {
                try {
                    deployTarget.zipDeploy(zipPackage);
                    return true;
                } catch (Exception e) {
                    progressIndicator.setText(String.format(RETRY_MESSAGE, e.getMessage(), retryCount, DEPLOY_MAX_TRY));
                }
            }
            final String error = String.format(RETRY_FAIL_MESSAGE, DEPLOY_MAX_TRY);
            final String action = "try later to deploy";
            throw new AzureToolkitRuntimeException(error, action);
        } catch (final Exception e) {
            progressIndicator.setText(String.format("Deploy failed, %s", e.getMessage()));
            throw e;
        }
    }

    @AzureOperation(
        name = "webapp|artifact.zip",
        params = {"$artifact.getName()"},
        type = AzureOperation.Type.TASK
    )
    private static File prepareZipPackage(WebAppBase deployTarget, File artifact, IProgressIndicator progressIndicator) {
        try {
            final File tempFolder = Files.createTempDirectory(TEMP_FOLDER_PREFIX).toFile();
            // copying artifacts to staging folder and rename it to app.jar
            progressIndicator.setText(COPYING_RESOURCES);
            FileUtils.copyFile(artifact, new File(tempFolder, JAVASE_ARTIFACT_NAME));
            // package the artifacts
            final File result = Files.createTempFile(TEMP_FILE_PREFIX, ".zip").toFile();
            FileUtil.zipFiles(tempFolder.listFiles(), result);
            return result;
        } catch (Exception e) {
            final String error = String.format("failed to archive the artifact[%s] into temp zip file", artifact.getName());
            throw new AzureToolkitRuntimeException(error, e);
        }
    }

    public static boolean deployWebAppToWebContainer(WebAppBase deployTarget
            , File artifact, boolean isDeployToRoot, IProgressIndicator progressIndicator) {
        int retryCount = 0;
        String webappPath = isDeployToRoot ? null : FilenameUtils.getBaseName(artifact.getName()).replaceAll("#", StringUtils.EMPTY);
        while (retryCount++ < DEPLOY_MAX_TRY) {
            try {
                if (deployTarget instanceof WebApp) {
                    ((WebApp) deployTarget).warDeploy(artifact, webappPath);
                } else {
                    ((DeploymentSlot) deployTarget).warDeploy(artifact, webappPath);
                }
                return true;
            } catch (Exception e) {
                progressIndicator.setText(String.format(RETRY_MESSAGE, e.getMessage(), retryCount, DEPLOY_MAX_TRY));
            }
        }
        final String error = String.format(RETRY_FAIL_MESSAGE, DEPLOY_MAX_TRY);
        final String action = "try later to deploy";
        throw new AzureToolkitRuntimeException(error, action);
    }

    public static class WebAppException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = 1352713295336034845L;

        WebAppException(String message) {
            super(message);
        }
    }

    public enum WebContainerMod {
        Newest_Tomcat_90("Newest Tomcat 9.0", "tomcat 9.0"),
        Newest_Tomcat_85("Newest Tomcat 8.5", "tomcat 8.5"),
        Newest_Tomcat_80("Newest Tomcat 8.0", "tomcat 8.0"),
        Newest_Tomcat_70("Newest Tomcat 7.0", "tomcat 7.0"),
        Newest_Jetty_93("Newest Jetty 9.3", "jetty 9.3"),
        Newest_Jetty_91("Newest Jetty 9.1", "jetty 9.1"),
        Java_SE_8("Java SE 8", "java 8"),
        Java_SE_11("Java SE 11", "java 11");

        private String displayName;
        private String value;

        WebContainerMod(String displayName, String value) {
            this.displayName = displayName;
            this.value = value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getValue() {
            return value;
        }

        public WebContainer toWebContainer() {
            return WebContainer.fromString(getValue());
        }

        @Override
        public String toString() {
            return getDisplayName();
        }

    }

    public static String generateWebContainerPath(WebContainer webContainer) throws IOException {
        if (webContainer.toString().equals(WebContainerMod.Newest_Tomcat_70.getValue())) {
            return "%AZURE_TOMCAT7_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Tomcat_80.getValue())) {
            return "%AZURE_TOMCAT8_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Tomcat_85.getValue())) {
            return "%AZURE_TOMCAT85_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Jetty_91.getValue())) {
            return "%AZURE_JETTY9_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Jetty_93.getValue())) {
            return "%AZURE_JETTY93_HOME%";
        }

        throw new IOException("Unknown web container: " + webContainer.toString());
    }

    @AzureOperation(
        name = "webapp.delete_detail",
        params = {"$webAppDetails.webApp.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public static void deleteAppService(WebAppDetails webAppDetails) {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(webAppDetails.subscriptionDetail.getSubscriptionId());
        azure.webApps().deleteById(webAppDetails.webApp.id());
        // check asp still exists
        AppServicePlan asp = azure.appServices().appServicePlans().getById(webAppDetails.appServicePlan.id());
        System.out.println("asp is " + (asp == null ? "null -> removing form cache" : asp.name()));
        // update cache
        AzureModelController.removeWebAppFromResourceGroup(webAppDetails.resourceGroup, webAppDetails.webApp);
        if (asp == null) {
            AzureModelController.removeAppServicePlanFromResourceGroup(webAppDetails.appServicePlanResourceGroup, webAppDetails.appServicePlan);
        }
    }

    @AzureOperation(
        name = "webapp|artifact.update",
        params = {"$webApp.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public static void uploadWebConfig(WebApp webApp, InputStream fileStream, IProgressIndicator indicator) throws IOException {
        FTPClient ftp = null;
        try {
            if (indicator != null) {
                indicator.setText("Stopping the service...");
            }
            webApp.stop();

            PublishingProfile pp = webApp.getPublishingProfile();
            ftp = getFtpConnection(pp);

            if (indicator != null) {
                indicator.setText("Uploading " + WEB_CONFIG_FILENAME + "...");
            }
            uploadFileToFtp(ftp, FTP_ROOT_PATH + WEB_CONFIG_FILENAME, fileStream, indicator);

            if (indicator != null) {
                indicator.setText("Starting the service...");
            }
            webApp.start();
        } finally {
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
    }

    @AzureOperation(
        name = "webapp|artifact.upload",
        params = {"$fileName", "$webApp.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public static int uploadToRemoteServer(WebAppBase webApp, String fileName, InputStream ins,
                                           IProgressIndicator indicator, String targetPath) throws IOException {
        FTPClient ftp = null;
        try {
            PublishingProfile pp = webApp.getPublishingProfile();
            ftp = getFtpConnection(pp);
            if (indicator != null) {
                indicator.setText(String.format("Uploading %s ...", fileName));
            }
            return uploadFileToFtp(ftp, targetPath, ins, indicator);
        } finally {
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
    }

    @AzureOperation(
        name = "webapp|artifact.upload_ftp",
        type = AzureOperation.Type.TASK
    )
    private static int uploadFileToFtp(FTPClient ftp, String path, InputStream stream, IProgressIndicator indicator) {
        boolean success;
        int count = 0;
        int rc = 0;
        final String error = "failed to upload the artifact to FTP server, reply code: " + rc;
        while (count++ < FTP_MAX_TRY) {
            try {
                success = ftp.storeFile(path, stream);
                if (success) {
                    if (indicator != null) {
                        indicator.setText("Uploading successfully...");
                    }
                    return count;
                }
                rc = ftp.getReplyCode();
                Thread.sleep(SLEEP_TIME);
            } catch (final Exception e) {
            }
        }
        throw new AzureToolkitRuntimeException(error);
    }

    public static class WebAppDetails {
        public SubscriptionDetail subscriptionDetail;
        public ResourceGroup resourceGroup;
        public AppServicePlan appServicePlan;
        public ResourceGroup appServicePlanResourceGroup;
        public WebApp webApp;

        public WebAppDetails() {
        }

        public WebAppDetails(ResourceGroup resourceGroup, WebApp webApp,
                             AppServicePlan appServicePlan, ResourceGroup appServicePlanResourceGroup,
                             SubscriptionDetail subscriptionDetail) {
            this.resourceGroup = resourceGroup;
            this.webApp = webApp;
            this.appServicePlan = appServicePlan;
            this.appServicePlanResourceGroup = appServicePlanResourceGroup;
            this.subscriptionDetail = subscriptionDetail;
        }
    }

    public static class AspDetails {
        private AppServicePlan asp;
        private ResourceGroup rg;

        public AspDetails(AppServicePlan asp, ResourceGroup rg) {
            this.asp = asp;
            this.rg = rg;
        }

        public AppServicePlan getAsp() {
            return asp;
        }

        public ResourceGroup getRg() {
            return rg;
        }
    }

    /**
     * Check if the web app is a Windows or Linux Java configured web app.
     * Docker web apps are not included.
     */
    public static boolean isJavaWebApp(@NotNull WebAppBase webApp) {
        return (webApp.operatingSystem() == OperatingSystem.WINDOWS && webApp.javaVersion() != JavaVersion.OFF)
                || (webApp.operatingSystem() == OperatingSystem.LINUX && (StringUtils.containsIgnoreCase(webApp.linuxFxVersion(), "jre")
                || StringUtils.containsIgnoreCase(webApp.linuxFxVersion(), "java")));
    }

    /**
     * For a Windows web app, APIs are separated to get jdk information and web container information.
     * For a Linux web app, API app.linuxFxVersion() returns a combined information, like
     * "Tomcat|8.5-jre8" if it is a Linux web app with the web container Tomcat.
     * We return a combined and refactored information as Java Runtime.
     */
    public static String getJavaRuntime(@NotNull final WebApp webApp) {
        String webContainer;
        switch (webApp.operatingSystem()) {
            case WINDOWS:
                webContainer = webApp.javaContainer() == null ? null : webApp.javaContainer().toLowerCase();
                return String.format("%s %s (Java%s)",
                        StringUtils.capitalize(webContainer), webApp.javaContainerVersion(), webApp.javaVersion().toString());
            case LINUX:
                final String linuxVersion = webApp.linuxFxVersion();
                if (linuxVersion == null) {
                    return DEFAULT_VALUE_WHEN_VERSION_INVALID;
                }

                final String[] versions = linuxVersion.split("\\||-");
                if (versions == null && versions.length != 3) {
                    return linuxVersion;
                }

                webContainer = versions[0].toLowerCase();
                final String webContainerVersion = versions[1];
                final String jreVersion = versions[2];
                final boolean isJavaLinuxRuntimeWithWebContainer = getAllJavaLinuxRuntimeStacks()
                        .stream()
                        .map(r -> r.stack())
                        .filter(w -> !w.equalsIgnoreCase("java"))
                        .anyMatch(w -> w.equalsIgnoreCase(webContainer));
                if (isJavaLinuxRuntimeWithWebContainer) {
                    // TOMCAT|8.5-jre8 -> Tomcat 8.5 (JRE8)
                    return String.format("%s %s (%s)", StringUtils.capitalize(webContainer), webContainerVersion, jreVersion.toUpperCase());
                } else {
                    // JAVA|8-jre8 -> JRE8
                    return jreVersion.toUpperCase();
                }
            default:
                return DEFAULT_VALUE_WHEN_VERSION_INVALID;
        }
    }

    public static List<DeploymentSlot> getDeployments(WebApp webApp) {
        if (webApp == null || webApp.deploymentSlots() == null || webApp.deploymentSlots().list() == null) {
            return new ArrayList<>();
        }
        List<DeploymentSlot> result = new ArrayList<>();
        for (DeploymentSlot deploymentSlot : webApp.deploymentSlots().list()) {
            result.add(deploymentSlot);
        }
        return result;
    }

    public static List<RuntimeStack> getAllJavaLinuxRuntimeStacks() {
        return Arrays.asList(new RuntimeStack[]{
            RuntimeStack.TOMCAT_8_5_JRE8,
            RuntimeStack.TOMCAT_9_0_JRE8,
            RuntimeStack.JAVA_8_JRE8,
            RuntimeStack.JAVA_11_JAVA11,
            RuntimeStack.TOMCAT_8_5_JAVA11,
            RuntimeStack.TOMCAT_9_0_JAVA11});
    }

    public static String getFileType(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        String fileType = "";
        int index = fileName.lastIndexOf(".");
        if (index >= 0 && (index + 1) < fileName.length()) {
            fileType = fileName.substring(index + 1);
        }
        return fileType;
    }

    public static String encodeURL(String fileName) {
        try {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return StringUtils.EMPTY;
        }
    }
}
