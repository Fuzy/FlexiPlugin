package usage;

public class L10nUsage {
    int gid;
    String itemName;
    String defaultMessage;

    String method;

    String clazz;

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

    public boolean isComplete() {
        return gid != 0 && itemName != null; //  && defaultMessage != null
    }

    @Override
    public String toString() {
        return "L10nUsage{" +
                "gid=" + gid +
                ", itemName='" + itemName + '\'' +
                ", defaultMessage='" + defaultMessage + '\'' +
                ", method='" + method + '\'' +
                ", clazz='" + clazz + '\'' +
                '}';
    }
}
