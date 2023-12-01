import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    private Map<String, Map<String, Object>> subroutineScope;
    private Map<String, Map<String, Object>> classScope;
    private Map<String, Integer> kindIndex;

    public SymbolTable() {
        this.subroutineScope = new HashMap<>();
        this.classScope = new HashMap<>();
        this.kindIndex = new HashMap<>();
        this.kindIndex.put("var", 0);
        this.kindIndex.put("arg", 0);
        this.kindIndex.put("field", 0);
        this.kindIndex.put("static", 0);
    }

    public void startSubroutine() {
        this.subroutineScope = new HashMap<>();
        this.kindIndex.put("var", 0);
        this.kindIndex.put("arg", 0);
    }

    public void define(String idenName, String idenType, String idenKind) {
        String[] classScopeKinds = {"field", "static"};
        String[] subroutineScopeKinds = {"var", "arg"};

        int currIndex = this.kindIndex.get(idenKind);
        if (List.of(classScopeKinds).contains(idenKind)) {
            this.classScope.put(idenName, Map.of(
                    "iden_type", idenType,
                    "iden_kind", idenKind,
                    "iden_index", currIndex
            ));
        } else if (List.of(subroutineScopeKinds).contains(idenKind)) {
            this.subroutineScope.put(idenName, Map.of(
                    "iden_type", idenType,
                    "iden_kind", idenKind,
                    "iden_index", currIndex
            ));
        }
        this.kindIndex.put(idenKind, currIndex + 1);
    }

    public int varCount(String idenKind) {
        return this.kindIndex.get(idenKind);
    }

    public String kindOf(String idenName) {
        if (this.subroutineScope.containsKey(idenName)) {
            return (String) this.subroutineScope.get(idenName).get("iden_kind");
        }
        if (this.classScope.containsKey(idenName)) {
            return (String) this.classScope.get(idenName).get("iden_kind");
        }
        return null;
    }

    public String typeOf(String idenName) {
        if (this.subroutineScope.containsKey(idenName)) {
            return (String) this.subroutineScope.get(idenName).get("iden_type");
        }
        if (this.classScope.containsKey(idenName)) {
            return (String) this.classScope.get(idenName).get("iden_type");
        }
        return null;
    }

    public int indexOf(String idenName) {
        if (this.subroutineScope.containsKey(idenName)) {
            return (int) this.subroutineScope.get(idenName).get("iden_index");
        }
        if (this.classScope.containsKey(idenName)) {
            return (int) this.classScope.get(idenName).get("iden_index");
        }
        return -1;
    }

    public void printXml(String idenName) {
        System.out.println("<identifierInfo>");
        System.out.println("  <kind> " + this.kindOf(idenName) + " </kind>");
        System.out.println("  <type> " + this.typeOf(idenName) + " </type>");
        System.out.println("  <index> " + this.indexOf(idenName) + " </index>");
        System.out.println("</identifierInfo>");
    }
}
