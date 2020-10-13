package com.microsoft.intellij.runner.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang.StringUtils;
import rx.Subscription;

import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;

public abstract class AzureResourceComboBox<T> extends ComboBox {

    private AtomicBoolean isRefreshing;
    private Subscription refreshSubscription;
    private AzureResourceComboBoxEditor editor;

    public AzureResourceComboBox() {
        this(Collections.emptyList());
    }

    public AzureResourceComboBox(List<T> items){
        this.editor = new AzureResourceComboBoxEditor();
        this.isRefreshing = new AtomicBoolean(false);
        this.setEditor(editor);
        this.setEditable(true);
        setItems(items);
        this.addPopupMenuListener(new PopupMenuListenerAdapter() {
            List<T> itemList;
            ComboFilterListener<T> listener;
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                editor.getTextField().setEditable(true);
                editor.getTextField().setText(StringUtils.EMPTY);
                itemList = new ArrayList<>();
                for (int i = 0; i < AzureResourceComboBox.this.getItemCount(); i++) {
                    itemList.add((T) AzureResourceComboBox.this.getItemAt(i));
                }
                listener = new ComboFilterListener<>(itemList, (item, input) -> StringUtils.contains(getComboBoxItemDescription(item), input));
                editor.getTextField().getDocument().addDocumentListener(listener);
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                editor.getTextField().setEditable(false);
                if (listener != null) {
                    editor.getTextField().getDocument().removeDocumentListener(listener);
                }
                final Object selectedItem = AzureResourceComboBox.this.getSelectedItem();
                AzureResourceComboBox.this.removeAllItems();
                AzureResourceComboBox.this.setItems(itemList);
                AzureResourceComboBox.this.setSelectedItem(selectedItem);
                editor.getTextField().setText(getComboBoxItemDescription((T) selectedItem));
            }
        });
    }

    class ComboFilterListener<T> extends DocumentAdapter{

        private List<T> list;
        private BiPredicate<T, String> filter;

        public ComboFilterListener(List<T> list, BiPredicate<T, String> filter) {
            this.list = list;
            this.filter = filter;
        }

        @Override
        protected void textChanged(@NotNull final DocumentEvent documentEvent) {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                try {
                    if (documentEvent.getType() == DocumentEvent.EventType.CHANGE) {
                        return;
                    }
                    String text = documentEvent.getDocument().getText(0, documentEvent.getDocument().getLength());
                    AzureResourceComboBox.this.removeAllItems();
                    list.stream().filter(item -> filter.test(item, text)).forEach(item -> AzureResourceComboBox.this.addItem(item));
                } catch (BadLocationException e) {
                    // swallow exception and show all items
                }
            });
        }
    }

    public void showRefresh(T defaultValue){
        this.setEnabled(false);
        this.isRefreshing.set(true);
        this.removeAllItems();
        this.addItem(defaultValue);
        this.editor.switchRefreshStatus(isRefreshing.get());
    }

    public void setItems(List<T> items) {
        this.removeAllItems();
        this.setEnabled(true);
        this.isRefreshing.set(false);
        this.editor.switchRefreshStatus(isRefreshing.get());
        items.stream().forEach(item-> this.addItem(item));
    }

    public abstract String getComboBoxItemDescription(T selectedItem);

    public abstract void onAdd();

    public boolean isFile() {
        return this.getSelectedItem() instanceof VirtualFile;
    }

    class AzureResourceComboBoxEditor extends BasicComboBoxEditor {
        private Object item;
        private ExtendableTextField textField;
        private ExtendableTextComponent.Extension addExtension;
        private ExtendableTextComponent.Extension refreshExtension;

        public AzureResourceComboBoxEditor(){
            this.addExtension = ExtendableTextComponent.Extension.create(
                    AllIcons.General.Add, "Create Resource", AzureResourceComboBox.this::onAdd);
            this.refreshExtension = ExtendableTextComponent.Extension.create(new AnimatedIcon.Default(),null, null);
        }

        @Override
        public void setItem(Object anObject) {
            item = anObject;
            final ExtendableTextField target = getTextField();
            if (AzureResourceComboBox.this.isPopupVisible()) {
                return;
            } else if (anObject == null) {
                target.setText(isRefreshing.get() ? "Refreshing..." : "");
            } else {
                target.setText(getComboBoxItemDescription((T) anObject));
                target.getAccessibleContext().setAccessibleName(target.getText());
                target.getAccessibleContext().setAccessibleDescription(target.getText());
            }
        }

        public void switchRefreshStatus(boolean isRefresh) {
            final ExtendableTextField textField = getTextField();
            textField.getExtensions().forEach(extension -> textField.removeExtension(extension));
            textField.addExtension(isRefresh ? refreshExtension : addExtension);
        }

        @Override
        public Object getItem(){
            return item;
        }

        @Override
        protected ExtendableTextField createEditorComponent() {
            return getTextField();
        }

        private ExtendableTextField getTextField(){
            if (textField == null) {
                synchronized (this) {
                    // init extendableTextField here as intellij may call `createEditorComponent` before constructor done
                    if (textField == null) {
                        textField = new ExtendableTextField();
                        textField.addExtension(ExtendableTextComponent.Extension.create(
                                AllIcons.General.Add, "Create Resource", AzureResourceComboBox.this::onAdd));
                        textField.setBorder(null);
                        textField.setEditable(false);
                    }
                }
            }
            return textField;
        }
    }
}

