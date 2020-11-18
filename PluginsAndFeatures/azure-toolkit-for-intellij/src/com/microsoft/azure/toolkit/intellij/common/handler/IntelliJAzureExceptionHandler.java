/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.common.handler;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.util.HashMap;
import java.util.Map;

public class IntelliJAzureExceptionHandler extends AzureExceptionHandler {

    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    private static final Map<String, AzureExceptionAction> exceptionActionMap = new HashMap<>();

    @Override
    protected void onShowErrorDialog(final String title, final String message) {
        AzureTaskManager.getInstance().runLater(() -> {
            Messages.showErrorDialog(message, title);
        });
    }

    @Override
    protected void onShowErrorDialog(final Throwable throwable,
                                     final @Nullable AzureExceptionAction[] extraActions) {
        AzureTaskManager.getInstance().runLater(() -> {

        });
    }

    @Override
    protected void onShowErrorNotification(final String title, final String message) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.ERROR);
        Notifications.Bus.notify(notification);
        RxJavaHooks.setOnObservableCreate();
        Observable.fromCallable().subscribeOn(Schedulers.newThread()).subscribe(o->{}, Throwable::printStackTrace)
    }

    @Override
    protected void onShowErrorNotification(final Throwable throwable,
                                           final @Nullable AzureExceptionAction[] extraActions) {

    }

    private String getErrorTitle(Throwable throwable) {
        final String operation;
        if(throwable instanceof AzureToolkitException){
            operation = ((AzureToolkitException)throwable).getAction();
        }
    }
}
