/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.po;


import com.microsoft.azure.toolkit.intellij.link.base.ServiceType;

public class ModulePO extends BaseServicePO {

    public ModulePO(String id) {
        super(id, ServiceType.IDE_MODULE);
    }
}
