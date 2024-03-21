package usage;
// Copyright 2000-2023 fuzy s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PsiNavigationDemoAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(PsiNavigationDemoAction.class);

    final String debugClass = "KonVykDphWizard";

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

                //TODO pouzije metodu kde je kurzor, nahradit za cz.winstrom.config.WSBundle#getTitle(int, java.lang.String, java.lang.String, boolean, java.lang.Object...)

                Project project = anActionEvent.getProject();
                Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.allScope(project));
                LOG.warn("Searching: " + files.size() + " files.");

                List<L10nUsage> usages = collectUsages(containingMethod, files);
                // cz.winstrom.config.WSConfig#getMessageTitle(java.lang.String, java.lang.String): 714
                Messages.showInfoMessage(project, "Count: " + usages.size(), "Usages");
                print(usages);
            }
        }

        LOG.warn(infoBuilder.toString());
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

    public List<L10nUsage> collectUsages(PsiMethod method, Collection<VirtualFile> files) {

        List<L10nUsage> usages = new ArrayList<>();
        List<L10nUsage> tmp = new ArrayList<>();

        // výchozí metoda
        L10nUsage start = new L10nUsage();
        start.setClazz(method.getContainingClass().getName());
        start.setMethod(method.getName());
        start.setPsiMethod(method);
        tmp.add(start);
        //TODO method parameters
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiParameter parameter = parameters[i];

            String canonicalText = parameter.getType().getCanonicalText();
            if ("int".equals(canonicalText)) {
                start.setGidParaIdx(i);
            } else if ("java.lang.String".equals(canonicalText)) {
                if (parameter.getName().contains("item")) {
                    start.setItemParaIdx(i);
                } else if (parameter.getName().contains("default")) {
                    start.setDefaultParaIdx(i);
                }
            }

            // logger().error("Type: " + canonicalText + " name: " + parameter.getName() + " [" + i + "]" + "\n ");
        }

        LOG.warn("start: " + start + "\n");

        int debugLimit = 200;
        int x = 0;

        do {
            x++;
            if (x > debugLimit) {
                break;
            }

            // konec rekurze
            if (tmp.isEmpty()) {
                break;
            }

            L10nUsage current = tmp.remove(0);

            // neni to metoda (WSHibernateExceptionWrapper#chybaPraceSDatabazi)
            if (current.getPsiMethod() == null) {
                continue;
            }

            Project project = method.getProject();

            // psiMethod is the PsiMethod for which I wanted to find usages.
            final Query<PsiReference> search = ReferencesSearch.search(current.getPsiMethod(), GlobalSearchScope.filesScope(project, files));
            Collection<PsiReference> psiReferences = search.findAll();


            //sb.append("references of ").append(current).append(": ").append(psiReferences.size()).append("\n");

            for (PsiReference psiReference : psiReferences) {
                // usages of method
                PsiElement reference = psiReference.getElement();

                StringBuilder sb = new StringBuilder();

                if (reference instanceof PsiReferenceExpression) {

                    L10nUsage usage = new L10nUsage();
                    usage.setOrigin(current);

                    PsiReferenceExpression expression = ((PsiReferenceExpression) reference);

                    PsiMethodCallExpression psiMethodCallExpression = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression.class);

                    PsiClass psiClass = PsiTreeUtil.getParentOfType(expression, PsiClass.class);
                    if (psiClass == null) {
                        LOG.warn("Class: " + psiClass + " is null.");
                        continue;
                    }

                    PsiMethod containingMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                    if (containingMethod != null) {
                        usage.setMethod(containingMethod.getName());
                        usage.setPsiMethod(containingMethod);
                    }
                    usage.setClazz(psiClass.getName());


                    PsiExpressionList psiExpressionList = PsiTreeUtil.getChildOfType(psiMethodCallExpression, PsiExpressionList.class);
                    PsiExpression[] expressions = psiExpressionList.getExpressions();
                    for (int i = 0; i < expressions.length; i++) {
                        PsiExpression psiExpression = expressions[i];
                        sb.append("Class: " + psiClass + ", method: " + containingMethod + ", psiExpression: " + psiExpression + "\n");

                        // TODO preskocit scitani s promenoou: cz.winstrom.config.WSBundle#getMonths
                        // TODO viceradkovy
                        if (psiExpression instanceof PsiPolyadicExpression) {
                            sb.append("[PsiPolyadicExpression] Class: " + psiClass + ", method: " + containingMethod + "\n");
                            continue;
                        }

                        //TODO pouzit pro konstantu pro nazev item - poradi parametru ve zdrojove metode
                        // konstanta s hodnotou
                        if (psiExpression instanceof PsiLiteralExpression) {
                            PsiJavaToken javaToken = PsiTreeUtil.getChildOfType(psiExpression, PsiJavaToken.class);
                            if (JavaTokenType.STRING_LITERAL == javaToken.getTokenType()) {
                                String text = normalizeName(javaToken.getText());
                                usage.setItemName(text);

                                sb.append("Class: " + psiClass + ", method: " + containingMethod + ", psiMethodCallExpression: " + psiExpression + ", setItemName: " + usage.getItemName() + "\n");
                            }
                        }

                        //TODO najit promennou v seznamu parametru volajici metody a ulozit si jeji index a predat ho dalsi usage
                        if (psiExpression instanceof PsiReferenceExpression) {
                            PsiJavaToken javaToken = PsiTreeUtil.getChildOfType(psiExpression, PsiJavaToken.class);

                            if (JavaTokenType.IDENTIFIER == javaToken.getTokenType()) {
                                usage.setItemNameParam(javaToken.getText());
                                //TODO jmeno promenne
                                sb.append("Class: " + psiClass + ", method: " + containingMethod + ", variable: " + usage.getItemNameParam() + "\n");
                            }
                        }


                    }

                    //usage.setDebug(sb.toString());
                    if (usage.isComplete()) {
                        usage.setPsiMethod(null);
                        usages.add(usage);
                    } else {
                        tmp.add(usage);
                    }

                }

            }

        } while (!tmp.isEmpty());

        return usages;
    }

    // Dokazu resit:
    // promenna jako parametr metody

    // Nedokazu resit:
    // pri dereferencovani prommenne ziskavam vyraz

    private void print(List<L10nUsage> usages) {

        List<L10nUsage> collect = usages.stream().filter(u -> debugClass.equals(u.clazz)).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("Found [" + collect.size() + "]: ");

        for (L10nUsage usage : collect) {
            sb.append(usage);
            sb.append("\n");
        }

        LOG.warn(sb.toString());
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        if (name.charAt(0) == '"') {
            name = name.substring(1);
        }

        if (name.charAt(name.length() - 1) == '"') {
            name = name.substring(0, name.length() - 1);
        }

        return name;
    }

}