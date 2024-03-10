package usage;
// Copyright 2000-2023 fuzy s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        //TODO nahradit za cz.winstrom.config.WSBundle#getTitle(int, java.lang.String, java.lang.String, boolean, java.lang.Object...)
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

        PluginManager.getLogger().error(infoBuilder);
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile != null);
    }

    // viceradkovy literal: https://github.com/JetBrains/intellij-community/blob/3032b48e705b94daa88473964e9eb02e632e07a5/java/java-analysis-impl/src/com/siyeh/ig/psiutils/ExpressionUtils.java#L38
    // pouziti: https://github.com/JetBrains/intellij-community/blob/3032b48e705b94daa88473964e9eb02e632e07a5/java/java-impl/src/com/siyeh/ipp/concatenation/CopyConcatenatedStringToClipboardIntention.java#L20

    //TODO iteracni rekurze:
    // dokud nemam vsechny pouziti s kompletnimi parametry
    // seznam nezpracovanych PsiReference, relevantni L10nUsage a pracovni objekt: itemName schovani za promennou

    public List<L10nUsage> findMethodUsages(PsiMethod method) {
        Project project = method.getProject();
        // psiMethod is the PsiMethod for which I wanted to find usages.
        final Query<PsiReference> search = ReferencesSearch.search(method);
        Collection<PsiReference> psiReferences = search.findAll();

        StringBuilder sb = new StringBuilder();
        sb.append("references: ").append(psiReferences.size()).append("\n");

        List<L10nUsage> usages = new ArrayList<>();

        for (PsiReference psiReference : psiReferences) {
            // usages of method
            PsiElement reference = psiReference.getElement();

            if (reference instanceof PsiReferenceExpression) {

                L10nUsage usage = new L10nUsage();
                usages.add(usage);

                PsiReferenceExpression expression = ((PsiReferenceExpression) reference);

                PsiMethodCallExpression psiMethodCallExpression = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression.class);

                PsiMethod containingMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                PsiClass containingClass = containingMethod.getContainingClass();
                if (containingClass == null || containingMethod == null) {
                    logger().error("Class: " + containingClass + ", method: " + containingMethod);
                    continue;
                }
                usage.setMethod(containingMethod.getName());
                usage.setClazz(containingClass.getName());

                PsiExpressionList psiExpressionList = PsiTreeUtil.getChildOfType(psiMethodCallExpression, PsiExpressionList.class);
                PsiExpression[] expressions = psiExpressionList.getExpressions();
                for (int i = 0; i < expressions.length; i++) {
                    PsiExpression psiExpression = expressions[i];

                    // TODO preskocit scitani s promenoou: cz.winstrom.config.WSBundle#getMonths
                    // TODO viceradkovy
                    if (psiExpression instanceof PsiPolyadicExpression) {
                        sb.append("[PsiPolyadicExpression] Class: " + containingClass + ", method: " + containingMethod + "\n");
                        continue;
                    }

                    PsiJavaToken javaToken = PsiTreeUtil.getChildOfType(psiExpression, PsiJavaToken.class);
                    if (javaToken != null) {
                        if (JavaTokenType.STRING_LITERAL == javaToken.getTokenType()) {
                            if (i == 0) {
                                usage.setItemName(javaToken.getText());
                            } else if (i == 1) {
                                usage.setDefaultMessage(javaToken.getText());
                            }
                        } else {
                            sb.append("Class: " + containingClass + ", method: " + containingMethod + ", text: " + javaToken.getText());
                        }

                    }
                }
                
            }


            //break; // debug
        }


        PluginManager.getLogger().warn(sb.toString());
        Messages.showInfoMessage(project, "Count: " + usages.size(), "Usages");

        print(usages);
        return usages;
    }

    private String findStringLiteralRecursively(PsiExpression expression) {

        PsiJavaToken javaToken = PsiTreeUtil.getChildOfType(expression, PsiJavaToken.class);
        if (javaToken != null) {
            if (JavaTokenType.STRING_LITERAL == javaToken.getTokenType()) {
                return javaToken.getText();
            } else if (JavaTokenType.IDENTIFIER == javaToken.getTokenType()) {
                //TODO o uroven vys, pozor na pozici parametru - podle nazvu
                PsiMethod containingMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                PsiMethodCallExpression psiMethodCallExpression = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression.class);

            } else {
                logger().error("Unexpected type of token: " + javaToken.getTokenType());
                return null;
            }
        }

        return null;
    }

    private void print(List<L10nUsage> usages) {
        StringBuilder sb = new StringBuilder();
        for (L10nUsage usage : usages) {
            sb.append(usage);
            sb.append("\n");
        }

        logger().warn(sb.toString());
    }

    private Logger logger() {
        return PluginManager.getLogger();
    }

}