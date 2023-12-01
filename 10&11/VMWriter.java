import java.util.ArrayList;
import java.util.List;

public class VMWriter {

    private List<String[]> commands;

    public VMWriter() {
        this.commands = new ArrayList<>();
    }

    public String[] writePush(Segment segment, String index) {
        String[] output = {"push " + segment.getValue() + " " + index};
        commands.add(output);
        return output;
    }

    public String[] writePop(Segment segment, String index) {
        String[] output = {"pop " + segment.getValue() + " " + index};
        commands.add(output);
        return output;
    }

    public String[] writeArithmetic(Command command) {
        String[] output = {command.getValue()};
        commands.add(output);
        return output;
    }

    public String[] writeLabel(String label) {
        String[] output = {"label " + label};
        commands.add(output);
        return output;
    }

    public String[] writeGoto(String label) {
        String[] output = {"goto " + label};
        commands.add(output);
        return output;
    }

    public String[] writeIf(String label) {
        String[] output = {"if-goto " + label};
        commands.add(output);
        return output;
    }

    public String[] writeCall(String name, String nargs) {
        String[] output = {"call " + name + " " + nargs};
        commands.add(output);
        return output;
    }

    public String[] writeFunction(String name, String nlocals) {
        String[] output = {"function " + name + " " + nlocals};
        commands.add(output);
        return output;
    }

    public String[] writeReturn() {
        String[] output = {"return"};
        commands.add(output);
        return output;
    }

    public String output() {
        StringBuilder result = new StringBuilder();
        for (String[] commandArray : commands) {
            for (String command : commandArray) {
                result.append(command).append(" ");
            }
            result.append("\n");
        }
        return result.toString();
    }

    public static void main(String[] args) {
        VMWriter writer = new VMWriter();
        writer.writePush(Segment.CONST, "5");
        writer.writePop(Segment.LOCAL, "0");
        writer.writeArithmetic(Command.ADD);
        writer.writeLabel("LOOP");
        writer.writeGoto("END");
        writer.writeIf("COND");
        writer.writeCall("functionName", "2");
        writer.writeFunction("functionName", "3");
        writer.writeReturn();
        System.out.println(writer.output());
    }
}

enum Segment {
    CONST("constant"),
    ARG("argument"),
    LOCAL("local"),
    STATIC("static"),
    THIS("this"),
    THAT("that"),
    POINTER("pointer"),
    TEMP("temp");

    private final String value;

    Segment(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

enum Command {
    ADD("add"),
    SUB("sub"),
    NEG("neg"),
    EQ("eq"),
    GT("gt"),
    LT("lt"),
    AND("and"),
    OR("or"),
    NOT("not");

    private final String value;

    Command(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
