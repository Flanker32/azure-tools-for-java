package com.microsoft.intellij.runner.springcloud.ui;

import com.intellij.execution.util.EnvVariablesTable;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.table.TableView;

import java.awt.*;
import java.util.List;

public class SpringCloudEnvVariablesTable extends EnvVariablesTable {

    public SpringCloudEnvVariablesTable(List<EnvironmentVariable> list) {
        TableView<EnvironmentVariable> tableView = getTableView();
        tableView.setPreferredScrollableViewportSize(
                new Dimension(tableView.getPreferredScrollableViewportSize().width,
                              tableView.getRowHeight() * JBTable.PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS));
        setValues(list);
        setPasteActionEnabled(true);
    }

    @Override
    protected AnActionButton[] createExtraActions() {
        return super.createExtraActions();
    }
}
