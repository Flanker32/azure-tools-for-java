/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class LinkComposite<S extends BaseLinkConfig, T extends BaseLinkConfig> {

    private S service;
    private T module;
    private String envPrefix;

    public LinkComposite(S source, T target) {
        this.service = source;
        this.module = target;
    }

}
