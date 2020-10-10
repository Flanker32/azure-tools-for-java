package com.microsoft.intellij.runner.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import org.apache.commons.collections4.CollectionUtils;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.util.Collections;
import java.util.List;

public abstract class AzureResourceComboBox<T> extends ComboBox {

    private Subscription refreshSubscription;

    public AzureResourceComboBox() {
        this(Collections.emptyList());
    }

    public AzureResourceComboBox(List<T> items){
        this.setEditable(true);
        this.setEditor(new AzureResourceComboBoxEditor());

        setItems(items);
    }

    public boolean isRefreshing() {
        return refreshSubscription != null && !refreshSubscription.isUnsubscribed();
    }

    // WIP
    public void refreshResource(Observable<List<T>> observable, T defaultValue) {
        this.removeAllItems();
        this.addItem(defaultValue);
        this.setEnabled(false);
        refreshSubscription = observable.subscribeOn(Schedulers.io())
                                        .subscribe(resources -> {
                                            resources.stream().forEach(resource -> AzureResourceComboBox.this.addItem(resource));
                                            if (CollectionUtils.containsAny(resources, defaultValue)) {
                                                AzureResourceComboBox.this.setSelectedItem(defaultValue);
                                            }
                                        });
    }

    public void setItems(List<T> items) {
        this.removeAllItems();
        items.stream().forEach(item-> this.addItem(item));
    }

    public abstract String getComboBoxItemDescription(T selectedItem);

    public abstract void onAdd();

    public boolean isFile() {
        return this.getSelectedItem() instanceof VirtualFile;
    }

    class AzureResourceComboBoxEditor extends BasicComboBoxEditor {
        private Object item;
        private ExtendableTextField resourceTextField;
        private ExtendableTextField progressIndicatorTextField;

        @Override
        public void setItem(Object anObject) {
            item = anObject;
            final ExtendableTextField target = getResourceTextField();
            if (anObject == null) {
                target.setText(isRefreshing() ? "Refreshing..." : "");
            } else {
                target.setText(getComboBoxItemDescription((T) anObject));
                target.getAccessibleContext().setAccessibleName(target.getText());
                target.getAccessibleContext().setAccessibleDescription(target.getText());
            }
        }

        @Override
        public Object getItem(){
            return item;
        }

        @Override
        protected ExtendableTextField createEditorComponent() {
            return isRefreshing() ? getProgressIndicatorTextField() : getResourceTextField();
        }

        private ExtendableTextField getResourceTextField(){
            if (resourceTextField == null) {
                synchronized (this) {
                    // init extendableTextField here as intellij may call `createEditorComponent` before constructor done
                    if (resourceTextField == null) {
                        resourceTextField = new ExtendableTextField();
                        resourceTextField.addExtension(ExtendableTextComponent.Extension.create(
                                AllIcons.General.Add, "Create Resource", AzureResourceComboBox.this::onAdd));
                        resourceTextField.setBorder(null);
                        resourceTextField.setEditable(false);
                    }
                }
            }
            return resourceTextField;
        }

        private ExtendableTextField getProgressIndicatorTextField(){
            if (progressIndicatorTextField == null) {
                synchronized (this) {
                    // init extendableTextField here as intellij may call `createEditorComponent` before constructor done
                    if (progressIndicatorTextField == null) {
                        progressIndicatorTextField = new ExtendableTextField();
                        progressIndicatorTextField.addExtension(ExtendableTextComponent.Extension.create(
                                new AnimatedIcon.Default(), null, null));
                        progressIndicatorTextField.setBorder(null);
                        progressIndicatorTextField.setEditable(false);
                    }
                }
            }
            return progressIndicatorTextField;
        }
    }
}

