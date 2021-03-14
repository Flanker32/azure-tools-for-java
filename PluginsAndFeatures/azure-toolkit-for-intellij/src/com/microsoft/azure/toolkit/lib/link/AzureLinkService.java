/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.link;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.link.LinkComposite;
import com.microsoft.azure.toolkit.intellij.link.ModuleLinkConfig;
import com.microsoft.azure.toolkit.intellij.link.base.LinkType;
import com.microsoft.azure.toolkit.intellij.link.base.ServiceType;
import com.microsoft.azure.toolkit.intellij.link.mysql.MySQLConnectionUtils;
import com.microsoft.azure.toolkit.intellij.link.mysql.MySQLLinkConfig;
import com.microsoft.azure.toolkit.intellij.link.mysql.PasswordConfig;
import com.microsoft.azure.toolkit.intellij.link.mysql.PasswordDialog;
import com.microsoft.azure.toolkit.intellij.link.po.BaseServicePO;
import com.microsoft.azure.toolkit.intellij.link.po.LinkPO;
import com.microsoft.azure.toolkit.intellij.link.po.ModulePO;
import com.microsoft.azure.toolkit.intellij.link.po.MySQLServicePO;
import com.microsoft.intellij.AzureLinkStorage;
import com.microsoft.intellij.AzureMySQLStorage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AzureLinkService {
    private static final AzureLinkService instance = new AzureLinkService();

    public static AzureLinkService getInstance() {
        return instance;
    }

    private AzureLinkService() {

    }

    public void link(Project project, LinkComposite<MySQLLinkConfig, ModuleLinkConfig> linkComposite) {
        ModulePO modulePO = createModulePO(linkComposite.getModule());
        MySQLServicePO resource = createResourcePO(linkComposite.getService());
        // create link
        LinkPO linkPO = new LinkPO(resource.getId(), modulePO.getId(), LinkType.SERVICE_WITH_MODULE, linkComposite.getEnvPrefix());
        // storage mysql
        AzureMySQLStorage.getStorage().addService(resource);
        // storage password
        if (ArrayUtils.isNotEmpty(linkComposite.getService().getPasswordConfig().getPassword())) {
            String inputPassword = String.valueOf(linkComposite.getService().getPasswordConfig().getPassword());
            AzureMySQLStorage.getStorage().savePassword(resource, resource.getPasswordSave(), resource.getUsername(), inputPassword);
        }
        // storage link
        AzureLinkStorage.getProjectStorage(project).addLink(linkPO);
    }

    private ModulePO createModulePO(ModuleLinkConfig config) {
        return new ModulePO(config.getModule().getName());
    }

    private MySQLServicePO createResourcePO(MySQLLinkConfig config) {
        MySQLServicePO mysqlPO = new MySQLServicePO.Builder()
                .id(config.getId())
                .url(config.getUrl())
                .username(config.getUsername())
                .passwordSave(config.getPasswordConfig().getPasswordSaveType())
                .build();
        return mysqlPO;
    }

    public Map<String, String> retrieveLinkEnvsByModuleName(Project project, String moduleName) {
        Map<String, String> linkedEnvMap = new LinkedHashMap<>();
        List<LinkPO> moduleRelatedLinkList = AzureLinkStorage.getProjectStorage(project).getLinkByModuleId(moduleName)
                .stream()
                .filter(e -> LinkType.SERVICE_WITH_MODULE.equals(e.getType()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(moduleRelatedLinkList)) {
            return linkedEnvMap;
        }
        // services in application level
        Set<? extends BaseServicePO> serviceSet = AzureMySQLStorage.getStorage().getServices();
        for (BaseServicePO service : serviceSet) {
            for (LinkPO link : moduleRelatedLinkList) {
                if (!StringUtils.equals(link.getServiceId(), service.getId())) {
                    continue;
                }
                String envPrefix = link.getEnvPrefix();
                if (ServiceType.AZURE_DATABASE_FOR_MYSQL.equals(service.getType())) {
                    MySQLServicePO mysql = (MySQLServicePO) service;
                    String password = readPasswordCredentials(project, mysql);
                    linkedEnvMap.put(envPrefix + "URL", mysql.getUrl());
                    linkedEnvMap.put(envPrefix + "USERNAME", mysql.getUsername());
                    linkedEnvMap.put(envPrefix + "PASSWORD", password);
                }
            }
        }
        return linkedEnvMap;
    }

    private String readPasswordCredentials(Project project, MySQLServicePO service) {
        String storagedPassword = AzureMySQLStorage.getStorage().loadPassword(service, service.getPasswordSave(), service.getUsername());
        if (StringUtils.isNotBlank(storagedPassword)) {
            if (MySQLConnectionUtils.connect(service.getUrl(), service.getUsername(), storagedPassword)) {
                return storagedPassword;
            }
        }
        // re-input password
        AtomicReference<PasswordConfig> passwordConfigReference = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            PasswordDialog dialog = new PasswordDialog(project, service.getUsername(), service.getUrl());
            dialog.setOkActionListener(data -> {
                dialog.close();
                String inputPassword = String.valueOf(data.getPassword());
                if (MySQLConnectionUtils.connect(service.getUrl(), service.getUsername(), inputPassword)) {
                    AzureMySQLStorage.getStorage().savePassword(service, data.getPasswordSaveType(), service.getUsername(), inputPassword);
                    if (!Objects.equals(service.getPasswordSave(), data.getPasswordSaveType())) {
                        service.setPasswordSave(data.getPasswordSaveType());
                    }
                }
                passwordConfigReference.set(data);
            });
            dialog.show();
        });
        PasswordConfig passwordConfig = passwordConfigReference.get();
        if (Objects.nonNull(passwordConfig)) {
            return String.valueOf(passwordConfig.getPassword());
        } else {
            return StringUtils.EMPTY;
        }
    }

}
