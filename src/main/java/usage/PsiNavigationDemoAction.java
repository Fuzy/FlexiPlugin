package usage;
// Copyright 2000-2023 fuzy s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PsiNavigationDemoAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(PsiNavigationDemoAction.class);

    final static String[] MODULES = new String[]{"winstrom-core", "winstrom-ucto"};
    // public static final int GID_BUTTONS = 2;
    final static int[] GIDS = new int[]{2};

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

                ModuleManager moduleManager = ModuleManager.getInstance(project);
                Module[] allModules = moduleManager.getModules();
                List<Module> modules = Arrays.stream(allModules).filter(m -> Arrays.asList(MODULES).contains(m.getName())).toList();
                LOG.warn("Modules: " + modules);

                Function<Module, Stream<? extends VirtualFile>> javaFilesForModule = m -> FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.moduleScope(m)).stream();
                Collection<VirtualFile> files = modules.stream().flatMap(javaFilesForModule).toList();
                LOG.warn("Searching: " + files.size() + " files.");

                Map<String, Integer> constants = bundleConstants(project);
                LOG.warn("WSBundleConstants: " + constants);

                List<L10nUsage> usages = collectUsages(containingMethod, files, constants);
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


    // dokud nemam vsechny pouziti s kompletnimi parametry
    // seznam nezpracovanych PsiReference, relevantni L10nUsage a pracovni objekt: itemName schovani za promennou

    public List<L10nUsage> collectUsages(PsiMethod method, Collection<VirtualFile> files, Map<String, Integer> constants) {

        List<L10nUsage> usages = new ArrayList<>();
        List<L10nUsage> tmp = new ArrayList<>();

        // výchozí metoda
        L10nUsage start = new L10nUsage();
        start.setClazz(method.getContainingClass().getName());
        start.setMethod(method.getName());
        start.setPsiMethod(method);
        tmp.add(start);
        //TODO method parameters
        fillPositionsOfParameters(method, start);

        LOG.warn("start: " + start + "\n");

        int debugLimit = 1000;
        int x = 0;

        do {
            x++;
            if (x > debugLimit) {
                //break;
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
            LOG.debug("Searching: " + current.getMethod() + " " + current.getClazz() + "[tmp: " + tmp.size() + "] " + "[usages: " + usages.size() + "] ");
            // psiMethod is the PsiMethod for which I wanted to find usages.
            final Query<PsiReference> search = ReferencesSearch.search(current.getPsiMethod(), GlobalSearchScope.filesScope(project, files));
            Collection<PsiReference> psiReferences = search.findAll();


            //sb.append("references of ").append(current).append(": ").append(psiReferences.size()).append("\n");
            for (PsiReference psiReference : psiReferences) {
                // usages of method
                PsiElement reference = psiReference.getElement();

                //StringBuilder sb = new StringBuilder();

                if (reference instanceof PsiReferenceExpression) {

                    L10nUsage usage = new L10nUsage();
                    //usage.setOrigin(current);

                    PsiReferenceExpression expression = ((PsiReferenceExpression) reference);

                    PsiMethodCallExpression psiMethodCallExpression = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression.class);

                    PsiClass psiClass = PsiTreeUtil.getParentOfType(expression, PsiClass.class);
                    if (psiClass == null) {
                        LOG.warn("Class: " + psiClass + " is null.");
                        continue;
                    }

                    PsiMethod containingMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                    if (containingMethod != null) {
                        // najit promennou v seznamu parametru volajici metody a ulozit si jeji index a predat ho dalsi usage
                        usage.setGid(current.getGid());
                        fillPositionsOfParameters(containingMethod, usage);
                        usage.setGidCallIdx(current.getGidParaIdx());
                        usage.setItemCallIdx(current.getItemParaIdx());
                        usage.setDefaultCallIdx(current.getDefaultParaIdx());

                        //String collect = Stream.of(containingMethod.getParameterList().getParameters()).map(p -> p.getType() + " " + p.getName() + ", ").collect(Collectors.joining());
                        usage.setMethod(containingMethod.getName());
                        usage.setPsiMethod(containingMethod);
                    }
                    usage.setClazz(psiClass.getName());

                    PsiExpressionList psiExpressionList = PsiTreeUtil.getChildOfType(psiMethodCallExpression, PsiExpressionList.class);
                    PsiExpression[] expressions = psiExpressionList.getExpressions();
                    for (int i = 0; i < expressions.length; i++) {
                        PsiExpression psiExpression = expressions[i];
                        //sb.append("Class: " + psiClass + ", method: " + containingMethod + ", psiExpression: " + psiExpression + "\n");

                        // TODO viceradkovy zatim neumim: cz.winstrom.config.WSBundle#getMonths ale tady je reseni:
                        // viceradkovy literal: https://github.com/JetBrains/intellij-community/blob/3032b48e705b94daa88473964e9eb02e632e07a5/java/java-analysis-impl/src/com/siyeh/ig/psiutils/ExpressionUtils.java#L38
                        // pouziti: https://github.com/JetBrains/intellij-community/blob/3032b48e705b94daa88473964e9eb02e632e07a5/java/java-impl/src/com/siyeh/ipp/concatenation/CopyConcatenatedStringToClipboardIntention.java#L20
                        if (psiExpression instanceof PsiPolyadicExpression) {
                            //sb.append("[PsiPolyadicExpression] Class: " + psiClass + ", method: " + containingMethod + "\n");
                            continue;
                        }

                        //TODO pouzit pro konstantu pro nazev item - poradi parametru ve zdrojove metode
                        // konstanta s hodnotou
                        if (psiExpression instanceof PsiLiteralExpression) {
                            PsiJavaToken javaToken = PsiTreeUtil.getChildOfType(psiExpression, PsiJavaToken.class);
                            if (JavaTokenType.STRING_LITERAL == javaToken.getTokenType()) {
                                String text = normalizeName(javaToken.getText());

                                if (i == usage.getItemCallIdx()) {
                                    usage.setItemName(text);
                                } else if (i == usage.getDefaultCallIdx()) {
                                    usage.setDefaultMessage(text);
                                }


                                //sb.append("Class: " + psiClass + ", method: " + containingMethod + ", psiMethodCallExpression: " + psiExpression + ", setItemName: " + usage.getItemName() + "\n");
                            }
                        }


                        if (psiExpression instanceof PsiReferenceExpression) {
                            PsiJavaToken javaToken = PsiTreeUtil.getChildOfType(psiExpression, PsiJavaToken.class);

                            if (JavaTokenType.IDENTIFIER == javaToken.getTokenType()) {
                                usage.setItemNameParam(javaToken.getText());
                                //sb.append("Class: " + psiClass + ", method: " + containingMethod + ", variable: " + usage.getItemNameParam() + "\n");
                            }

                            // hodnota parametru GID
                            String canonicalText = ((PsiReferenceExpression) psiExpression).getCanonicalText();
                            fillUsageWithGidValue(constants, canonicalText, usage);

                            //TODO dodelat expression voBL.getMessage(EXPRESSION, "Výraz (%p)", expression)

                        }

                    }

                    boolean isGidAllowed = usage.getGid() == 4;


                    //usage.setDebug(sb.toString());
                    if (usage.isComplete() && isGidAllowed) {
                        usage.setPsiMethod(null);
                        usages.add(usage);
                    } else if (usage.getGid() == -1 || isGidAllowed) {

                        if (usage.isNotUsefull()) {
                            //LOG.warn("WHY: usage" + usage + ", current: " + current);
                            continue;
                        }

                        tmp.add(usage);
                    }

                }

            }

        } while (!tmp.isEmpty());

        return usages;
    }

    private void fillUsageWithGidValue(Map<String, Integer> constants, String canonicalText, L10nUsage usage) {
        String constantName = null;
        if (canonicalText.startsWith("cz.winstrom.config.WSConfig.GID_")) {
            constantName = canonicalText.substring("cz.winstrom.config.WSConfig.".length());
        } else if (canonicalText.startsWith("WSBundleConstants.GID_")) {
            constantName = canonicalText.substring("WSBundleConstants.".length());
        } else if (canonicalText.startsWith("WSBundle.GID_")) {
            constantName = canonicalText.substring("WSBundle.".length());
        } else if (canonicalText.startsWith("WSConfig.GID_")) {
            constantName = canonicalText.substring("WSConfig.".length());
        } else if (canonicalText.startsWith("GID_")) {
            constantName = canonicalText;
        } else if (canonicalText.contains("GID_")) {
            LOG.error("GID v neocekavanem tvaru: " + canonicalText);
        }

        if (constantName != null) {
            Integer gid = constants.get(constantName);
            if (gid == null) {
                LOG.error("GID nenalezen: " + canonicalText);
            } else {
                usage.setGid(gid);
            }
        }
    }

    private void fillPositionsOfParameters(PsiMethod method, L10nUsage start) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiParameter parameter = parameters[i];

            String canonicalText = parameter.getType().getCanonicalText();
            if ("int".equals(canonicalText)) {
                start.setGidParaIdx(i);
            } else if ("java.lang.String".equals(canonicalText)) {
                if (parameter.getName().contains("item") || parameter.getName().contains("label")) {
                    start.setItemParaIdx(i);
                } else if (parameter.getName().contains("default")) {
                    start.setDefaultParaIdx(i);
                }
            }

            //LOG.warn("Type: " + canonicalText + " name: " + parameter.getName() + " [" + i + "]" + "\n ");
        }
    }

    private Map<String, Integer> bundleConstants(Project project) {
        Map<String, Integer> constants = new HashMap<>();

        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiClass wsBundle = psiFacade.findClass("cz.winstrom.config.WSBundleConstants", GlobalSearchScope.allScope(project));
        PsiField[] allFields = wsBundle.getAllFields();
        for (PsiField field : allFields) {
            PsiLiteralExpression literal = PsiTreeUtil.getChildOfType(field, PsiLiteralExpression.class);
            constants.put(field.getName(), Integer.parseInt(literal.getText()));
        }

        return constants;
    }

    // Dokazu resit:
    // promenna jako parametr metody

    // Nedokazu resit:
    // pri dereferencovani prommenne ziskavam vyraz

    private void print(List<L10nUsage> usages) {

        Predicate<L10nUsage> debug = u -> debugClass.equals(u.clazz);
        Predicate<L10nUsage> all = u -> true;
        List<L10nUsage> collect = usages.stream().filter(all).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("Found [" + collect.size() + "]: \n");

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