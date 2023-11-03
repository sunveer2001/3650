import java.io.*;

public class Translator {

    public static String getInfo(File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    public static void main(String[] args) throws IOException {
        boolean writeEnv;
        File file = new File(args[0]);
        File dir;
        File[] inFiles;
        FilenameFilter vmFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".vm");
            }
        };
        String program;
        if (file.isDirectory()) {
            program = file.getName();
            inFiles = file.listFiles(vmFilter);
            dir = file.getAbsoluteFile();
        } else {
            inFiles = new File[1];
            inFiles[0] = file;
            program = getInfo(file);
            dir = file.getAbsoluteFile().getParentFile();
        }
        writeEnv = new File(dir, "Sys.vm").exists();

        File outFile = new File(dir, program + ".asm");
        Writer codeWriter = new Writer(outFile);
        if (writeEnv) {
            codeWriter.writeInit();
        }
        for (File f : inFiles) {
            String module = getInfo(f);
            codeWriter.setModule(module);
            Parser parser = new Parser(f);
            String[] strs;
            while ((strs = parser.advance()) != null) {
                execute(codeWriter, strs);
            }
            parser.close();
        }
        codeWriter.writeEndLoop();
        codeWriter.writeRestore();
        codeWriter.close();
    }

    private static void execute(Writer cw, String[] strs) {
        switch (strs[0]) {
        case "push":
            cw.writePush(strs[1], Integer.parseInt(strs[2]));
            break;
        case "pop":
            cw.writePop(strs[1], Integer.parseInt(strs[2]));
            break;
        case "label":
            cw.writeLabel(strs[1]);
            break;
        case "goto":
            cw.writeGoto(strs[1]);
            break;
        case "if-goto":
            cw.writeIfGoto(strs[1]);
            break;
        case "return":
            cw.writeReturn();
            break;
        case "call":
            cw.writeCall(strs[1], Integer.parseInt(strs[2]));
            break;
        case "function":
            cw.writeFunction(strs[1], Integer.parseInt(strs[2]));
            break;
        default:
            cw.writeArithm(strs[0]);
            break;
        }
        cw.incN();
    }

}