package usage;
// Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PsiNavigationDemoAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            return;
        }
        int offset = editor.getCaretModel().getOffset();

        final StringBuilder infoBuilder = new StringBuilder();
        PsiElement element = psiFile.findElementAt(offset);
        infoBuilder.append("Element at caret: ").append(element).append("\n");
        if (element != null) {
            PsiMethod containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            infoBuilder
                    .append("Containing method: ")
                    .append(containingMethod != null ? containingMethod.getName() : "none")
                    .append("\n");
            if (containingMethod != null) {
                PsiClass containingClass = containingMethod.getContainingClass();
                infoBuilder
                        .append("Containing class: ")
                        .append(containingClass != null ? containingClass.getName() : "none")
                        .append("\n");

                infoBuilder.append("Local variables:\n");
                containingMethod.accept(new JavaRecursiveElementVisitor() {
                    @Override
                    public void visitLocalVariable(@NotNull PsiLocalVariable variable) {
                        super.visitLocalVariable(variable);
                        infoBuilder.append(variable.getName()).append("\n");
                    }
                });

                findMethodUsages(containingMethod);
            }
        }
        Messages.showInfoMessage(anActionEvent.getProject(), infoBuilder.toString(), "PSI Info");
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile != null);
    }

    public void findMethodUsages(PsiMethod method) {
        Project project = method.getProject();
        final Query<PsiReference> search = ReferencesSearch.search(method); // psiMethod is the PsiMethod for which I wanted to find usages.
        Collection<PsiReference> psiReferences = search.findAll();

        StringBuilder sb = new StringBuilder();
        sb.append("references: ").append(psiReferences.size()).append("\n");

        for (PsiReference reference : psiReferences) {
            PsiElement resolve = reference.resolve();
            if (resolve instanceof PsiMethod) {
                PsiMethod method1 = (PsiMethod) resolve;
                PsiParameterList parameters = method1.getParameterList();

                // Iterate over each parameter
                for (int i = 0; i < parameters.getParametersCount(); i++) {
                    PsiParameter parameter = parameters.getParameter(i);
                    // Access information about the parameter
                    String parameterName = parameter.getName();
                    PsiType parameterType = parameter.getType();

                    // Handle the parameter according to your requirements
                    sb.append("Parameter Name: " + parameterName);
                    sb.append("Parameter Type: " + parameterType);
                }

            }

            break; // debug
        }


        Messages.showInfoMessage(project, sb.toString() , "PSI Info");
    }

}