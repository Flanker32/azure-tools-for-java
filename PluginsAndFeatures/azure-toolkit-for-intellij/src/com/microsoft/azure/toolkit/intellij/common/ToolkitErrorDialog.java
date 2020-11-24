package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.HideableDecorator;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import static com.intellij.openapi.ui.Messages.wrapToScrollPaneIfNeeded;

public class ToolkitErrorDialog extends AzureDialogWrapper {
    private JPanel pnlMain;
    private JPanel pnlHolder;
    private JPanel pnlDetails;
    private JPanel pnlRoot;
    private JLabel lblIcon;
    private JPanel pnlMessage;

    private HideableDecorator slotDecorator;

    private String title;
    private String message;
    private String details;
    private Throwable throwable;
    private Action[] actions;
    private Project project;

    public ToolkitErrorDialog(final Project project, String title, String message, String details,
                              AzureExceptionHandler.AzureExceptionAction[] actions, Throwable throwable) {
        super(project);
        this.project = project;
        this.title = title;
        this.message = message;
        this.details = details;
        this.actions = getExceptionAction(actions);
        this.throwable = throwable;
        setTitle(title);
        slotDecorator = new HideableDecorator(pnlHolder, "Details", true);
        slotDecorator.setContentComponent(pnlDetails);
        slotDecorator.setOn(false);
        if (StringUtils.isEmpty(details)) {
            slotDecorator.setEnabled(false);
            pnlHolder.setVisible(false);
        }
        init();
    }

    @Override
    protected void init() {
        super.init();
        final JTextPane messageTemp = new JTextPane();
        final JTextPane messageComponent = Messages.configureMessagePaneUi(new JTextPane(), message);
        final JComponent component = wrapToScrollPaneIfNeeded(messageComponent, 60, 10);
        pnlMessage.add(component, BorderLayout.CENTER);

        final JTextPane detailsComponent = Messages.configureMessagePaneUi(new JTextPane(), details);
        pnlDetails.add( wrapToScrollPaneIfNeeded(detailsComponent, 60, 10), BorderLayout.CENTER);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    protected Action[] createActions() {
        return ArrayUtils.addAll(new Action[]{getOKAction()}, actions);
    }

    private Action[] getExceptionAction(AzureExceptionHandler.AzureExceptionAction[] actions) {
        return Arrays.stream(actions).map(exceptionAction -> new DialogWrapper.DialogWrapperAction(exceptionAction.name()) {
            @Override
            protected void doAction(final ActionEvent actionEvent) {
                exceptionAction.actionPerformed(throwable);
            }
        }).toArray(Action[]::new);
    }
}
