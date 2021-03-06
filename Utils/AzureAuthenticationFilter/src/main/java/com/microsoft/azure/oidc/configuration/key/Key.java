/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.key;

import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.configuration.key.exponent.Exponent;
import com.microsoft.azure.oidc.configuration.key.modulus.Modulus;

public interface Key {

    TimeStamp getNotBefore();

    Modulus getSecret();

    Exponent getExponent();

    boolean equals(Object object);

    int hashCode();

}
