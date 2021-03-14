/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.po;


import com.microsoft.azure.toolkit.intellij.link.base.ServiceType;
import com.microsoft.azure.toolkit.intellij.link.mysql.PasswordSaveType;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MySQLServicePO extends BaseServicePO {

    private String url;
    private String username;
    @Setter
    private PasswordSaveType passwordSave;

    private MySQLServicePO(String id, String url, String username, PasswordSaveType passwordSave) {
        super(id, ServiceType.AZURE_DATABASE_FOR_MYSQL);
        this.url = url;
        this.username = username;
        this.passwordSave = passwordSave;
    }

    public static class Builder {
        private String id;
        private String url;
        private String username;
        private PasswordSaveType passwordSave;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder passwordSave(PasswordSaveType passwordSave) {
            this.passwordSave = passwordSave;
            return this;
        }

        public MySQLServicePO build() {
            return new MySQLServicePO(id, url, username, passwordSave);
        }

    }
}
