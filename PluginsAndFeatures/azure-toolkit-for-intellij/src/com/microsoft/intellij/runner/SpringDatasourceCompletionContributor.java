/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.link.LinkMySQLToModuleDialog;
import com.microsoft.azure.toolkit.intellij.link.base.LinkType;
import com.microsoft.azure.toolkit.intellij.link.po.LinkPO;
import com.microsoft.intellij.AzureLinkStorage;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SpringDatasourceCompletionContributor extends CompletionContributor {

    public SpringDatasourceCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    public void addCompletions(@NotNull CompletionParameters parameters,
                                               @NotNull ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {
                        resultSet.addElement(LookupElementBuilder
                                .create("spring.datasource.url")
                                .withIcon(AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.BIND_INTO))
                                .withInsertHandler(new MyInsertHandler())
                                .withBoldness(true)
                                .withTypeText("String")
                                .withTailText(" (Link Azure Datasource for MySQL Service)")
                                .withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT)
                        );
                    }
                }
        );

    }

    private class MyInsertHandler implements InsertHandler<LookupElement> {

        @Override
        public void handleInsert(@NotNull InsertionContext insertionContext, @NotNull LookupElement lookupElement) {
            Module module = ModuleUtil.findModuleForFile(insertionContext.getFile().getVirtualFile(), insertionContext.getProject());
            List<LinkPO> moduleLinkList = AzureLinkStorage.getProjectStorage(insertionContext.getProject()).getLinkByModuleId(module.getName())
                    .stream()
                    .filter(e -> LinkType.SERVICE_WITH_MODULE == e.getType())
                    .collect(Collectors.toList());
            boolean insertRequired = true;
            if (CollectionUtils.isEmpty(moduleLinkList)) {
                final LinkMySQLToModuleDialog dialog = new LinkMySQLToModuleDialog(insertionContext.getProject(), null, module);
                insertRequired = dialog.showAndGet();
            }
            if (insertRequired) {
                String envPrefix = moduleLinkList.get(0).getEnvPrefix();
                StringBuilder builder = new StringBuilder();
                builder.append("=${").append(envPrefix).append("URL}").append(StringUtils.LF)
                        .append("spring.datasource.username=${").append(envPrefix).append("USERNAME}").append(StringUtils.LF)
                        .append("spring.datasource.password=${").append(envPrefix).append("PASSWORD}").append(StringUtils.LF);
                EditorModificationUtil.insertStringAtCaret(insertionContext.getEditor(), builder.toString(), true);
            } else {
                EditorModificationUtil.insertStringAtCaret(insertionContext.getEditor(), "=", true);
            }
        }
    }

}
