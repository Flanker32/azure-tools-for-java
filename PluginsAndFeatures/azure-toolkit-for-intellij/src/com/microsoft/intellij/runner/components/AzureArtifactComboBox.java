package com.microsoft.intellij.runner.components;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.intellij.ui.components.AzureArtifact;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.util.Collections;
import java.util.List;

public class AzureArtifactComboBox extends ComboBox<AzureArtifact> {

    public AzureArtifactComboBox() {
        this(Collections.emptyList());
    }

    public AzureArtifactComboBox(List<T> items){
        this.setEditable(true);
        this.setEditor(new DeployTargetComboBoxEditor());
        this.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(@NotNull final JList jList,
                                  final Object o,
                                  final int i,
                                  final boolean b,
                                  final boolean b1) {
                setText(getItemText(o));
                setIcon(getItemIcon(o));
            }
        });
        setItems(items);
    }

    public void setItems(List<T> items) {
        this.removeAllItems();
        items.stream().forEach(item-> this.addItem(item));
    }

    public String getComboBoxItemDescription(AzureArtifact selectedItem){
        selectedItem.getName();
    }

    public Icon getComboBoxItemIcon(AzureArtifact selectedItem){
        return selectedItem.getIcon();
    }

    public boolean isFile() {
        return this.getSelectedItem() instanceof VirtualFile;
    }

    protected String getItemText(Object o) {
        if (o == null) {
            return StringUtils.EMPTY;
        } else if (o instanceof VirtualFile) {
            return ((VirtualFile) o).getPath();
        } else {
            return getComboBoxItemDescription((T) o);
        }
    }

    protected Icon getItemIcon(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof VirtualFile) {
            return AllIcons.FileTypes.Archive;
        } else {
            return getComboBoxItemIcon((T) o);
        }
    }

    class DeployTargetComboBoxEditor extends BasicComboBoxEditor {
        private Object item;
        private ExtendableTextField extendableTextField;

        @Override
        public void setItem(Object anObject) {
            item = anObject;
            extendableTextField.setText(getItemText(anObject));
            extendableTextField.getAccessibleContext().setAccessibleName(extendableTextField.getText());
            extendableTextField.getAccessibleContext().setAccessibleDescription(extendableTextField.getText());
        }

        @Override
        public Object getItem(){
            return item;
        }

        @Override
        protected JTextField createEditorComponent() {
            if (extendableTextField == null) {
                synchronized (this) {
                    // init extendableTextField here as intellij may call `createEditorComponent` before constructor done
                    if (extendableTextField == null) {
                        extendableTextField = new ExtendableTextField();
                        extendableTextField.addExtension(ExtendableTextComponent.Extension.create(
                                AllIcons.General.OpenDisk, "Open file", this::onSelectFile));
                        extendableTextField.setBorder(null);
                    }
                }
            }
            return extendableTextField;
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
            for (int i = 0; i < AzureArtifactComboBox.this.getItemCount(); i++) {
                Object object = AzureArtifactComboBox.this.getItemAt(i);
                if (object instanceof VirtualFile && StringUtils.equals(virtualFile.getPath(), ((VirtualFile) object).getPath())) {
                    AzureArtifactComboBox.this.setSelectedItem(virtualFile);
                    return;
                }
            }
            AzureArtifactComboBox.this.addItem(virtualFile);
            AzureArtifactComboBox.this.setSelectedItem(virtualFile);
        }
    }
}
