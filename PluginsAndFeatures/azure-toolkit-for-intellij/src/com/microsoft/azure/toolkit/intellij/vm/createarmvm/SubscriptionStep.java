/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.createarmvm;

import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardNavigationState;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.intellij.actions.SelectSubscriptionsAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import com.microsoft.azure.toolkit.intellij.vm.VMWizardModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubscriptionStep extends AzureWizardStep<VMWizardModel> implements TelemetryProperties {
    VMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JButton buttonLogin;
    private JComboBox<SubscriptionDetail> subscriptionComboBox;
    private JLabel userInfoLabel;
    private Project project;

    public SubscriptionStep(final VMWizardModel model, final Project project) {
        super("Choose a Subscription", null, null);

        this.model = model;
        this.project = project;

        model.configStepList(createVmStepsList, 0);

        buttonLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SelectSubscriptionsAction.selectSubscriptions(project).subscribe((subs) -> {
                    loadSubscriptions();
                });
            }
        });

        subscriptionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof SubscriptionDetail) {
                    model.setSubscription((SubscriptionDetail) itemEvent.getItem());
                    model.setRegion(null);
                }
            }
        });
        loadSubscriptions();
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();
        model.getCurrentNavigationState().NEXT.setEnabled(subscriptionComboBox.getSelectedItem() != null);
        return rootPanel;
    }

    private void loadSubscriptions() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                model.getCurrentNavigationState().NEXT.setEnabled(false);
                return;
            }
            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
            List<SubscriptionDetail> selectedSubscriptions = subscriptionDetails.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());

            subscriptionComboBox.setModel(new DefaultComboBoxModel<>(selectedSubscriptions.toArray(new SubscriptionDetail[selectedSubscriptions.size()])));
            if (selectedSubscriptions.size() > 0) {
                model.setSubscription(selectedSubscriptions.get(0));
            }
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Subscriptions\n\n" + ex.getMessage(), ex);
        }
    }

    @Override
    public Map<String, String> toProperties() {
        return model.toProperties();
    }
}
