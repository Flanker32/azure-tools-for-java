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

package com.microsoft.intellij.runner.webapp.webappconfig.slimui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.components.AzureResourceComboBox;
import com.microsoft.intellij.runner.components.DeployTargetComboBox;
import com.microsoft.intellij.runner.webapp.Constants;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import com.microsoft.intellij.runner.webapp.webappconfig.slimui.creation.WebAppCreationDialog;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import icons.MavenIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WebAppSlimSettingPanel extends AzureSettingPanel<WebAppConfiguration> implements WebAppDeployMvpViewSlim {

    private static final String DEPLOYMENT_SLOT = "Deployment Slot";
    private static final String DEFAULT_SLOT_NAME = "slot-%s";
    private static final String CREATE_NEW_WEBAPP = "Create New WebApp";
    private static final String REFRESHING_WEBAPP = "Refreshing...";
    private static final String DEPLOYMENT_SLOT_HOVER = "Deployment slots are live apps with their own hostnames. App" +
            " content and configurations elements can be swapped between two deployment slots, including the production " +
            "slot.";

    private WebAppDeployViewPresenterSlim presenter = null;

    private JPanel pnlSlotCheckBox;
    private JTextField txtNewSlotName;
    private JComboBox cbxSlotConfigurationSource;
    private JCheckBox chkDeployToSlot;
    private JCheckBox chkToRoot;
    private JLabel lblArtifact;
    private DeployTargetComboBox cbArtifact;
    private JLabel lblMavenProject;
    private DeployTargetComboBox cbMaven;
    private JPanel pnlRoot;
    private JPanel pnlSlotDetails;
    private JRadioButton rbtNewSlot;
    private JRadioButton rbtExistingSlot;
    private JComboBox cbxSlotName;
    private JPanel pnlSlot;
    private JPanel pnlSlotHolder;
    private JPanel pnlCheckBox;
    private JPanel pnlSlotRadio;
    private JLabel lblSlotName;
    private JLabel lblSlotConfiguration;
    private HyperlinkLabel lblCreateWebApp;
    private JCheckBox chkOpenBrowser;
    private HyperlinkLabel lblNewSlot;
    private JPanel pnlExistingSlot;
    private JButton btnSlotHover;
    private AzureResourceComboBox<ResourceEx<WebApp>> azureResourceComboBox;
    private HideableDecorator slotDecorator;

    // presenter
    private WebAppConfiguration webAppConfiguration;

    public WebAppSlimSettingPanel(@NotNull Project project, @NotNull WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.presenter = new WebAppDeployViewPresenterSlim();
        this.presenter.onAttachView(this);

        final ButtonGroup slotButtonGroup = new ButtonGroup();
        slotButtonGroup.add(rbtNewSlot);
        slotButtonGroup.add(rbtExistingSlot);
        rbtExistingSlot.addActionListener(e -> toggleSlotType(true));
        rbtNewSlot.addActionListener(e -> toggleSlotType(false));

        chkDeployToSlot.addActionListener(e -> toggleSlotPanel(chkDeployToSlot.isSelected()));

        Icon informationIcon = AllIcons.General.Information;
        btnSlotHover.setIcon(informationIcon);
        btnSlotHover.setHorizontalAlignment(SwingConstants.CENTER);
        btnSlotHover.setPreferredSize(new Dimension(informationIcon.getIconWidth(), informationIcon.getIconHeight()));
        btnSlotHover.setToolTipText(DEPLOYMENT_SLOT_HOVER);
        btnSlotHover.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                btnSlotHover.setBorderPainted(true);
                final MouseEvent phantom = new MouseEvent(btnSlotHover, MouseEvent.MOUSE_ENTERED,
                                                          System.currentTimeMillis(), 0, 10, 10, 0, false);
                DefaultLoader.getIdeHelper().invokeLater(() -> IdeTooltipManager.getInstance().eventDispatched(phantom));
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                btnSlotHover.setBorderPainted(false);
                IdeTooltipManager.getInstance().dispose();
            }
        });

//        cbArtifact.addItemListener((itemEvent) -> updateArtifactConfiguration());
//        cbMaven.addItemListener((itemEvent) -> updateArtifactConfiguration());

        JLabel labelForNewSlotName = new JLabel("Slot Name");
        labelForNewSlotName.setLabelFor(txtNewSlotName);
        JLabel labelForExistingSlotName = new JLabel("Slot Name");
        labelForExistingSlotName.setLabelFor(cbxSlotName);

        slotDecorator = new HideableDecorator(pnlSlotHolder, DEPLOYMENT_SLOT, true);
        slotDecorator.setContentComponent(pnlSlot);
        slotDecorator.setOn(webAppConfiguration.isSlotPanelVisible());
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Deploy to Azure";
    }

    @Override
    public void disposeEditor() {
    }

    @Override
    public synchronized void fillWebApps(List<ResourceEx<WebApp>> webAppLists, final String defaultWebAppId) {
        List<ResourceEx<WebApp>> sortedWebAppLists = webAppLists
                .stream()
                .filter(resource -> WebAppUtils.isJavaWebApp(resource.getResource()))
                .sorted((a, b) -> a.getResource().name().compareToIgnoreCase(b.getResource().name()))
                .collect(Collectors.toList());
        azureResourceComboBox.setItems(sortedWebAppLists);
    }

    @Override
    public synchronized void fillDeploymentSlots(List<DeploymentSlot> slotList, @NotNull final ResourceEx<WebApp> selectedWebApp) {
        cbxSlotName.removeAllItems();
        cbxSlotConfigurationSource.removeAllItems();

        cbxSlotConfigurationSource.addItem(Constants.DO_NOT_CLONE_SLOT_CONFIGURATION);
        cbxSlotConfigurationSource.addItem(selectedWebApp.getResource().name());
        slotList.stream().filter(slot -> slot != null).forEach(slot -> {
            cbxSlotName.addItem(slot.name());
            cbxSlotConfigurationSource.addItem(slot.name());
            if (StringUtils.equals(slot.name(), webAppConfiguration.getSlotName())) {
                cbxSlotName.setSelectedItem(slot.name());
            }
            if (StringUtils.equals(slot.name(), webAppConfiguration.getNewSlotConfigurationSource())) {
                cbxSlotConfigurationSource.setSelectedItem(slot.name());
            }
        });
        boolean existDeploymentSlot = slotList.size() > 0;
        lblNewSlot.setVisible(!existDeploymentSlot);
        cbxSlotName.setVisible(existDeploymentSlot);
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return cbArtifact;
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return lblArtifact;
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return cbMaven;
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return lblMavenProject;
    }

    @Override
    protected void resetFromConfig(@NotNull WebAppConfiguration configuration) {
        refreshWebApps(false);
        if (configuration.getWebAppId() != null && webAppConfiguration.isDeployToSlot()) {
            toggleSlotPanel(true);
            chkDeployToSlot.setSelected(true);
            final boolean useNewDeploymentSlot = Comparing.equal(configuration.getSlotName(),
                                                                 Constants.CREATE_NEW_SLOT);
            rbtNewSlot.setSelected(useNewDeploymentSlot);
            rbtExistingSlot.setSelected(!useNewDeploymentSlot);
            toggleSlotType(!useNewDeploymentSlot);
        } else {
            toggleSlotPanel(false);
            chkDeployToSlot.setSelected(false);
        }
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String defaultSlotName = StringUtils.isEmpty(webAppConfiguration.getNewSlotName()) ?
                String.format(DEFAULT_SLOT_NAME, df.format(new Date())) : webAppConfiguration.getNewSlotName();
        txtNewSlotName.setText(defaultSlotName);
        chkToRoot.setSelected(configuration.isDeployToRoot());
        chkOpenBrowser.setSelected(configuration.isOpenBrowserAfterDeployment());
        slotDecorator.setOn(configuration.isSlotPanelVisible());
    }

    @Override
    protected void apply(@NotNull WebAppConfiguration configuration) {
        final ResourceEx<WebApp> selectedWebApp = (ResourceEx<WebApp>) azureResourceComboBox.getSelectedItem();
        configuration.setWebAppId(selectedWebApp == null ? null : selectedWebApp.getResource().id());
        configuration.setSubscriptionId(selectedWebApp == null ? null : selectedWebApp.getSubscriptionId());
        final String targetName = getTargetName();
        configuration.setTargetPath(getTargetPath());
        configuration.setTargetName(targetName);
        configuration.setCreatingNew(false);
        configuration.setDeployToSlot(chkDeployToSlot.isSelected());
        configuration.setSlotPanelVisible(slotDecorator.isExpanded());
        chkToRoot.setVisible(isAbleToDeployToRoot(targetName));
        toggleSlotPanel(configuration.isDeployToSlot() && selectedWebApp != null);
        if (chkDeployToSlot.isSelected()) {
            configuration.setDeployToSlot(true);
            configuration.setSlotName(cbxSlotName.getSelectedItem() == null ? "" :
                                      cbxSlotName.getSelectedItem().toString());
            if (rbtNewSlot.isSelected()) {
                configuration.setSlotName(Constants.CREATE_NEW_SLOT);
                configuration.setNewSlotName(txtNewSlotName.getText());
                configuration.setNewSlotConfigurationSource((String) cbxSlotConfigurationSource.getSelectedItem());
            }
        } else {
            configuration.setDeployToSlot(false);
        }
        configuration.setDeployToRoot(chkToRoot.isVisible() && chkToRoot.isSelected());
        configuration.setOpenBrowserAfterDeployment(chkOpenBrowser.isSelected());
    }

    private boolean isAbleToDeployToRoot(final String targetName) {
        final ResourceEx<WebApp> selectedWebApp = (ResourceEx<WebApp>) azureResourceComboBox.getSelectedItem();
        if (selectedWebApp == null) {
            return false;
        }
        final WebApp app = selectedWebApp.getResource();
        final boolean isDeployingWar =
                MavenRunTaskUtil.getFileType(targetName).equalsIgnoreCase(MavenConstants.TYPE_WAR);
        return isDeployingWar && (app.operatingSystem() == OperatingSystem.WINDOWS ||
                !Constants.LINUX_JAVA_SE_RUNTIME.equalsIgnoreCase(app.linuxFxVersion()));
    }

    private void createNewWebApp() {
        final WebAppCreationDialog dialog = new WebAppCreationDialog(this.project, this.webAppConfiguration);
        if (dialog.showAndGet()) {
            final WebApp webApp = dialog.getCreatedWebApp();
            if (webApp != null) {
                // Set selectedWebApp to null in case user deploy while refreshing web app list
                webAppConfiguration.setWebAppId(webApp.id());
                refreshWebApps(true, webApp.id());
            } else {
                // In case created failed
                refreshWebApps(false);
            }
        } else {
            refreshWebApps(false);
        }
    }

    private void toggleSlotPanel(boolean slot) {
        boolean isDeployToSlot = slot && (azureResourceComboBox.getSelectedItem() != null);
        rbtNewSlot.setEnabled(isDeployToSlot);
        rbtExistingSlot.setEnabled(isDeployToSlot);
        lblSlotName.setEnabled(isDeployToSlot);
        lblSlotConfiguration.setEnabled(isDeployToSlot);
        cbxSlotName.setEnabled(isDeployToSlot);
        txtNewSlotName.setEnabled(isDeployToSlot);
        cbxSlotConfigurationSource.setEnabled(isDeployToSlot);
    }

    private void toggleSlotType(final boolean isExistingSlot) {
        pnlExistingSlot.setVisible(isExistingSlot);
        pnlExistingSlot.setEnabled(isExistingSlot);
        txtNewSlotName.setVisible(!isExistingSlot);
        txtNewSlotName.setEnabled(!isExistingSlot);
        lblSlotConfiguration.setVisible(!isExistingSlot);
        cbxSlotConfigurationSource.setVisible(!isExistingSlot);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        lblCreateWebApp = new HyperlinkLabel("No available webapp, click to create a new one");
        lblCreateWebApp.addHyperlinkListener(e -> createNewWebApp());

        lblNewSlot = new HyperlinkLabel("No available deployment slot, click to create a new one");
        lblNewSlot.addHyperlinkListener(e -> rbtNewSlot.doClick());

        cbMaven = new DeployTargetComboBox<MavenProject>() {
            @Override
            public String getComboBoxItemDescription(final MavenProject selectedItem) {
                return selectedItem.toString();
            }

            @Override
            public Icon getComboBoxItemIcon(final MavenProject selectedItem) {
                return MavenIcons.MavenProject;
            }
        };
        cbMaven.setVisible(false);

        cbArtifact = new DeployTargetComboBox<Artifact>() {
            @Override
            public String getComboBoxItemDescription(final Artifact selectedItem) {
                return selectedItem.getName();
            }

            @Override
            public Icon getComboBoxItemIcon(final Artifact selectedItem) {
                return selectedItem.getArtifactType().getIcon();
            }
        };
        cbArtifact.setVisible(false);

        azureResourceComboBox = new AzureResourceComboBox<ResourceEx<WebApp>>() {
            @Override
            public String getComboBoxItemDescription(final ResourceEx<WebApp> selectedItem) {
                return selectedItem.getResource().name();
            }

            @Override
            public void onAdd() {
                createNewWebApp();
            }
        };
        azureResourceComboBox.setRenderer(new WebAppCombineBoxRender(azureResourceComboBox));
    }

    private void refreshWebApps(boolean force) {
        refreshWebApps(force, null);
    }

    private void refreshWebApps(boolean force, String targetId) {
        azureResourceComboBox.showRefresh(null);
        presenter.loadWebApps(force, targetId);
    }

    private void updateArtifactConfiguration() {
        webAppConfiguration.setTargetName(getTargetName());
        webAppConfiguration.setTargetPath(getTargetPath());
    }

    class WebAppCombineBoxRender extends SimpleListCellRenderer {

        private final JComboBox comboBox;
        private final int cellHeight;
        private static final String TEMPLATE_STRING = "<html><div>TEMPLATE</div><small>TEMPLATE</small></html>";

        public WebAppCombineBoxRender(JComboBox comboBox) {
            this.comboBox = comboBox;
            JLabel template = new JLabel(TEMPLATE_STRING);
            //Create a multi-line jlabel and calculate its preferred size
            this.cellHeight = template.getPreferredSize().height;
        }

        @Override
        public void customize(JList list, Object value, int i, boolean b, boolean b1) {
            if (value == null) {
                return;
            }else if(value instanceof String){
                setText((String) value);
            }
            else {
                final ResourceEx<WebApp> webApp = (ResourceEx<WebApp>) value;
                // For label in combobox textfield, just show webapp name
                if (i >= 0) {
                    setText(getWebAppLabelText(webApp.getResource()));
                    list.setFixedCellHeight(cellHeight);
                } else {
                    setText(webApp.getResource().name());
                }
            }
        }

        private String getWebAppLabelText(WebApp webApp) {
            final String webAppName = webApp.name();
            final String os = StringUtils.capitalize(webApp.operatingSystem().toString());
            final String runtime = WebAppUtils.getJavaRuntime(webApp);
            final String resourceGroup = webApp.resourceGroupName();

            return comboBox.isPopupVisible() ? String.format("<html><div>%s</div></div><small>OS:%s Runtime:%s " +
                    "ResourceGroup:%s</small></html>", webAppName, os, runtime, resourceGroup) : webAppName;
        }
    }
}
