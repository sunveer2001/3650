import java.io.*;

public class Writer {

    private String name;
    private PrintStream ps;
    private String module;
    private String func;
    private int n;

    public Writer(File file) {
        try {
            ps = new PrintStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        n = 0;
        func = "Sys.init";
        name = Translator.getInfo(file);
    }

    public void incN() {
        n++;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void writeInit() {
        ps.println("@256\nD=A\n@SP\nM=D\n");
        writeCall("Sys.init", 0);
    }

    private void writeUnary(String cmd) {
        ps.println("@SP\nA=M-1");
        if (cmd.equals("neg")) {
            ps.println("M=-M");
        } else {
            ps.println("M=!M");
        }
        ps.println();
    }

    private void writeRelational(String cmd) {
        ps.println("D=M-D\n@relat.mt." + module + "." + n);
        ps.println("D;J" + cmd.toUpperCase() + "\n@SP\nA=M-1\nM=0");
        ps.println("@relat.end." + module + "." + n + "\n0;JMP");
        ps.println("(relat.mt." + module + "." + n + ")\n@SP\nA=M-1\nM=-1");
        ps.println("(relat.end." + module + "." + n + ")");
    }


    private void writeBinary(String cmd) {
        ps.println("@SP\nM=M-1\nA=M\nD=M\nA=A-1");
        switch (cmd) {
            case "eq":
            case "gt":
            case "lt":
                writeRelational(cmd);
                break;
            case "add":
                ps.println("M=M+D");
                break;
            case "sub":
                ps.println("M=M-D");
                break;
            case "and":
                ps.println("M=M&D");
                break;
            case "or":
                ps.println("M=M|D");
                break;
            default:
                break;
        }
        ps.println();
    }

    public void writeArithm(String cmd) {
        if (cmd.equals("neg") || cmd.equals("not")) writeUnary(cmd);
        else writeBinary(cmd);
    }

    private String segmentConstant(String seg) {
        switch (seg) {
            case "local": return "LCL";
            case "argument": return "ARG";
            case "this": return "THIS";
            case "that": return "THAT";
            default: return null;
        }
    }

    private void writeVD(String seg, int i) {
        switch (seg) {
            case "temp":
                ps.println("@5\nD=A\n@" + i + "\nA=A+D\nD=M");
                break;
            case "constant":
                ps.println("@" + i + "\nD=A");
                break;
            case "static":
                ps.println("@" + module + "." + i + "\nD=M");
                break;
            case "pointer":
                if (i == 0) ps.println("@THIS");
                else ps.println("@THAT");
                ps.println("D=M");
                break;
            default:
                ps.println("@" + segmentConstant(seg) + "\nD=M\n@" + i + "\nA=A+D\nD=M");
                break;
        }
    }

    private void writePushExpr(String expr) {
        ps.println("@SP\nA=M\nM=" + expr + "\n@SP\nM=M+1");
    }

    public void writePush(String seg, int i) {
        writeVD(seg, i);
        writePushExpr("D");
        ps.println();
    }

    private void writeDV(String seg, int i) {
        switch (seg) {
            case "static":
                ps.println("@" + module + "." + i + "\nM=D");
                break;
            case "pointer":
                if (i == 0) ps.println("@THIS");
                else ps.println("@THAT");
                ps.println("M=D");
                break;
            default:
                ps.println("@R13\nM=D");
                if (seg.equals("temp")) ps.println("@5\nD=A");
                else ps.println("@" + segmentConstant(seg)+"\nD=M");
                ps.println("@" + i + "\nD=A+D\n@R14\nM=D\n@R13\nD=M\n@R14\nA=M\nM=D\n");
                break;
        }
    }

    public void writePop(String seg, int i) {
        ps.println("@SP\nM=M-1\nA=M\nD=M");
        writeDV(seg, i);
        ps.println();
    }

    public void writeGoto(String labelName) {
        ps.println("@" + func + "$" + labelName);
        ps.println("0;JMP");
        ps.println();
    }

    public void writeLabel(String labelName) {
        ps.println("(" + func + "$" + labelName + ")");
        ps.println();
    }

    public void writeIfGoto(String labelName) {
        ps.println("@SP\nM=M-1\nA=M\nD=M\n@" + func + "$" + labelName+"\nD;JNE\n");
    }

    public void writeReturn() {
        ps.println("@" + name + "$restore\n0;JMP\n");
    }

    public void writeCall(String func, int nVars) {
        String returnLabel = func + "$ret." + n;

        ps.println("@" + returnLabel+"\nD=A");
        writePushExpr("D");

        ps.println("@LCL\nD=M");
        writePushExpr("D");

        ps.println("@ARG\nD=M");
        writePushExpr("D");

        ps.println("@THIS\nD=M");
        writePushExpr("D");

        ps.println("@THAT\nD=M");
        writePushExpr("D");

        ps.println("@SP\nD=M\n@LCL\nM=D");

        ps.println("@5\nD=D-A\n@" + nVars + "\nD=D-A\n@ARG\nM=D");

        ps.println("@" + func + "\n0;JMP");
        ps.println("(" + returnLabel + ")\n");
    }

    public void writeEndLoop() {
        ps.println("(" + name + "$end)\n@" + name + "$end\n0;JMP\n");
    }

    public void writeRestore() {
        ps.println("(" + name + "$restore)");
        ps.println("@ARG\nD=M\n@R14\nM=D");
        ps.println("@LCL\nD=M-1\n@R13\nAM=D\nD=M\n@THAT\nM=D");
        ps.println("@R13\nD=M-1\nAM=D\nD=M\n@THIS\nM=D");
        ps.println("@R13\nD=M-1\nAM=D\nD=M\n@ARG\nM=D");
        ps.println("@R13\nD=M-1\nAM=D\nD=M\n@LCL\nM=D");
        ps.println("@R13\nA=M-1\nD=M\n@R13\nM=D");    
        ps.println("@SP\nA=M-1\nD=M\n@R14\nA=M\nM=D");    
        ps.println("@R14\nD=M+1\n@SP\nM=D");   
        ps.println("@R13\nA=M\n0;JMP\n");
    }

    public void writeFunction(String func, int args) {
        ps.println("(" + func + ")");
        for (int i = 0; i < args; i++) {
            writePushExpr("0");
        }
        ps.println();
    }

    public void close() {
        ps.close();
    }
}