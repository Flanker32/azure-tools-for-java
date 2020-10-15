package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ComboBoxDeployTarget extends AzureComboBox<AzureArtifact> {
    private Project project;

    public ComboBoxDeployTarget(Project project) {
        super(false);
        this.project = project;
    }

    @NotNull
    @Override
    protected List<? extends AzureArtifact> loadItems() throws Exception {
        return AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.OpenDisk, "Open file", this::onSelectFile);
    }

    private void onSelectFile() {
        // Todo: enable customize file descriptor
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        fileDescriptor.setTitle("Select artifact to deploy");
        final VirtualFile file = FileChooser.chooseFile(fileDescriptor, null, null);
        if (file.exists()) {
            addOrSelectExistingVirtualFile(file);
        }
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        for (int i = 0; i < DeployTargetComboBox.this.getItemCount(); i++) {
            Object object = DeployTargetComboBox.this.getItemAt(i);
            if (object instanceof VirtualFile && StringUtils.equals(virtualFile.getPath(), ((VirtualFile) object).getPath())) {
                DeployTargetComboBox.this.setSelectedItem(virtualFile);
                return;
            }
        }
        DeployTargetComboBox.this.addItem(virtualFile);
        DeployTargetComboBox.this.setSelectedItem(virtualFile);
    }
}
