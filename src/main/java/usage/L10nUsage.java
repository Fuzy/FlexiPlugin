package usage;

import com.intellij.psi.PsiMethod;

public class L10nUsage {
    int gid;
    String itemName;
    String defaultMessage;

    String method;

    String clazz;

    PsiMethod psiMethod;

    String gidParam;

    int gidIndex;

    String itemNameParam;

    int itemNameIndex;

    String debug;

    L10nUsage origin;

    public L10nUsage() {
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public void setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }

    public void setPsiMethod(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }

    public String getGidParam() {
        return gidParam;
    }

    public void setGidParam(String gidParam) {
        this.gidParam = gidParam;
    }

    public int getGidIndex() {
        return gidIndex;
    }

    public void setGidIndex(int gidIndex) {
        this.gidIndex = gidIndex;
    }

    public String getItemNameParam() {
        return itemNameParam;
    }

    public void setItemNameParam(String itemNameParam) {
        this.itemNameParam = itemNameParam;
    }

    public int getItemNameIndex() {
        return itemNameIndex;
    }

    public void setItemNameIndex(int itemNameIndex) {
        this.itemNameIndex = itemNameIndex;
    }

    public String getDebug() {
        return debug;
    }

    public void setDebug(String debug) {
        this.debug = debug;
    }

    public L10nUsage getOrigin() {
        return origin;
    }

    public void setOrigin(L10nUsage origin) {
        this.origin = origin;
    }

    public boolean isComplete() {
        return itemName != null; //   gid != 0 && defaultMessage != null
    }

    @Override
    public String toString() {
        return "L10nUsage{" +
                "gid=" + gid +
                ", itemName='" + itemName + '\'' +
                ", defaultMessage='" + defaultMessage + '\'' +
                ", method='" + method + '\'' +
                ", clazz='" + clazz + '\'' +
                ", psiMethod=" + psiMethod +
                ", gidParam='" + gidParam + '\'' +
                ", itemNameParam='" + itemNameParam + '\'' +
                ", debug='" + debug + '\'' +
                ", origin=" + origin +
                '}';
    }
}
