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

package com.microsoft.azure.toolkit.lib.common.streaminglog;

import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicStreamingLogView extends ConsoleViewImpl {
    protected static final String SEPARATOR = System.getProperty("line.separator");
    protected static final Pattern STACK_TRACE_PATTERN =
            Pattern.compile("(\\s+at\\s+([\\w$\\.]+\\/)?(([\\w$]+\\.)+[<\\w$>]+)\\()([\\w-$]+\\.java:\\d+)(\\).*)");

    public BasicStreamingLogView(@NotNull final Project project, final boolean viewer) {
        super(project, viewer);
    }

    protected void printlnToConsole(String message, ConsoleViewContentType consoleViewContentType) {
        Matcher matcher = STACK_TRACE_PATTERN.matcher(message);
        if (matcher.find()) {
            // print error stack
            String prefix = matcher.group(1);
            String suffix = matcher.group(6);
            String methodField = matcher.group(3);
            String locationField = matcher.group(5);
            String fullyQualifiedName = StringUtils.contains(methodField, "$") ?
                                        methodField.substring(0, methodField.lastIndexOf("$")) :
                                        methodField.substring(0, methodField.lastIndexOf("."));
            String[] locations = locationField.split(":");
            int lineNumber = Integer.parseInt(locations[1]);
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                print(prefix, consoleViewContentType);
                printHyperlink(locationField,
                               new OpenFileHyperlinkInfo(getProject(), getVirtualFile(fullyQualifiedName), lineNumber - 1));
                print(suffix + SEPARATOR, consoleViewContentType);
            });
        } else {
            print(message + SEPARATOR, consoleViewContentType);
        }
    }

    protected VirtualFile getVirtualFile(String fullyQualifiedName) {
        final Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        for (Module module : modules) {
            final GlobalSearchScope scope = GlobalSearchScope.moduleWithLibrariesScope(module);
            final PsiClass ecClass = JavaPsiFacade.getInstance(getProject()).findClass(fullyQualifiedName, scope);
            if (ecClass != null) {
                return PsiUtil.getVirtualFile(ecClass);
            }
        }
        return null;
    }
}
