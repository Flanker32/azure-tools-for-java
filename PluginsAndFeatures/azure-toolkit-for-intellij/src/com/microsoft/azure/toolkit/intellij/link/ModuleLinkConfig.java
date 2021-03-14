/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link;

import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.intellij.link.base.ServiceType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ModuleLinkConfig extends BaseLinkConfig {

    private Module module;

    public static ModuleLinkConfig getDefaultConfig(Module module) {
        ModuleLinkConfig config = new ModuleLinkConfig();
        config.setModule(module);
        return config;
    }

    @Override
    public ServiceType getType() {
        return ServiceType.IDE_MODULE;
    }

}
