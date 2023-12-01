import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompEngine {
   private Tokenizer tokenizer;
   private VMWriter codeWriter;
   private SymbolTable symbolTable;
   private Map<String, Segment> segmentMap;
   private Map<String, Integer> condLabelIndex;
   private String className;

   public CompEngine(String sourceText) {
      this.tokenizer = new Tokenizer(sourceText);
      this.codeWriter = new VMWriter();
      this.symbolTable = new SymbolTable();
      this.segmentMap = new HashMap<>();
      this.segmentMap.put("var", Segment.LOCAL);
      this.segmentMap.put("arg", Segment.ARG);
      this.segmentMap.put("static", Segment.STATIC);
      this.segmentMap.put("field", Segment.THIS);
      this.condLabelIndex = new HashMap<>();
      this.condLabelIndex.put("if", 0);
      this.condLabelIndex.put("while", 0);
      this.className = null;
   }

   public void compileClass() {
      System.out.println("<class>");
      eatKeyword("class");
      className = eatIdentifier();
      eatSymbol('{');
      compileClassVarDec();
      compileSubroutineZeroOrMore();
      eatSymbol('}');
      System.out.println("</class>");
   }

   public void compileClassVarDec() {
      String[] lhsValues = { "static", "field" };
      String nextToken = tokenizer.peek().getKey();
      while (Arrays.asList(lhsValues).contains(nextToken)) {
         System.out.println("<classVarDec>");

         String idenKind = eatKeywordOr(Arrays.asList("static", "field"));
         String idenType = compileType();
         String idenName = eatIdentifier();
         symbolTable.define(idenName, idenType, idenKind);

         compileVarNameZeroOrMore(idenType, idenKind);
         eatSymbol(';');

         System.out.println("</classVarDec>");
         nextToken = tokenizer.peek().getKey();
      }
   }

   public String compileType() {
      String nextToken = tokenizer.peek().getKey();
      List<String> keywords = Arrays.asList("int", "char", "boolean");
      if (keywords.contains(nextToken)) {
         return eatKeywordOr(keywords);
      } else {
         return eatIdentifier();
      }
   }

   public void compileSubroutineZeroOrMore() {
      String[] lhsValues = { "constructor", "function", "method" };
      String nextToken = tokenizer.peek().getKey();
      while (Arrays.asList(lhsValues).contains(nextToken)) {
         compileSubroutine();
         nextToken = tokenizer.peek().getKey();
      }
   }

   public void compileSubroutine() {
      System.out.println("<subroutineDec>");
      symbolTable.startSubroutine();

      String subroutineKind = eatKeywordOr(Arrays.asList("constructor", "function", "method"));

      if (subroutineKind.equals("method")) {
         symbolTable.define("this", className, "arg");
      }

      String nextToken = tokenizer.peek().getKey();

      String subroutineType;
      if (nextToken.equals("void")) {
         subroutineType = eatKeyword("void");
      } else {
         subroutineType = compileType();
      }

      String subroutineName = eatIdentifier();

      eatSymbol("(");
      compileParameterList();
      eatSymbol(")");

      subroutineName = String.format("%s.%s", className, subroutineName);
      compileSubroutineBody(subroutineName, subroutineKind);

      if (subroutineType.equals("void")) {
         codeWriter.writePush(Segment.CONST, 0);
      }

      codeWriter.writeReturn();
      System.out.println("</subroutineDec>");
   }

   public void compileParameterList() {
      System.out.println("<parameterList>");
      String nextToken = tokenizer.peek().getKey();
      List<String> typeKeywords = Arrays.asList("int", "char", "boolean");
      if (typeKeywords.contains(nextToken) || tokenizer.peek().getValue() == Token.IDENTIFIER) {
         compileParameter();
         compileParameterZeroOrMore();
      }
      System.out.println("</parameterList>");
   }

   public void compileParameter() {
      String paramType = compileType();
      String paramName = eatIdentifier();
      symbolTable.define(paramName, paramType, "arg");
   }

   public void compileParameterZeroOrMore() {
      String nextToken = tokenizer.peek().getKey();
      while (nextToken.equals(",")) {
         eatSymbol(",");
         compileParameter();
         nextToken = tokenizer.peek().getKey();
      }
   }

   public void compileSubroutineBody(String subroutineName, String subroutineKind) {
      System.out.println("<subroutineBody>");
      eatSymbol("{");

      compileVarDecZeroOrMore();

      codeWriter.writeFunction(subroutineName, symbolTable.varCount("var"));

      if (subroutineKind.equals("method")) {
         codeWriter.writePush(Segment.ARG, 0);
         codeWriter.writePop(Segment.POINTER, 0);
      }
      if (subroutineKind.equals("constructor")) {
         codeWriter.writePush(Segment.CONST, symbolTable.varCount("field"));
         codeWriter.writeCall("Memory.alloc", 1);
         codeWriter.writePop(Segment.POINTER, 0);
      }

      compileStatements();
      eatSymbol("}");
      System.out.println("</subroutineBody>");
   }

   public void compileVarDecZeroOrMore() {
      String nextToken = tokenizer.peek().getKey();
      while (nextToken.equals("var")) {
         compileVarDec();
         nextToken = tokenizer.peek().getKey();
      }
   }

   public void compileVarDec() {
      System.out.println("<varDec>");
      String idenKind = eatKeyword("var");
      String idenType = compileType();
      String idenName = eatIdentifier();

      symbolTable.define(idenName, idenType, idenKind);

      compileVarNameZeroOrMore(idenType, idenKind);

      eatSymbol(";");
      System.out.println("</varDec>");
   }

   public void compileVarNameZeroOrMore(String idenType, String idenKind) {
      String nextToken = tokenizer.peek().getKey();
      while (nextToken.equals(",")) {
         eatSymbol(",");
         String idenName = eatIdentifier();

         symbolTable.define(idenName, idenType, idenKind);

         nextToken = tokenizer.peek().getKey();
      }
   }

   public void compileStatements() {
      System.out.println("<statements>");
      String nextToken = tokenizer.peek().getKey();
      while (Arrays.asList("let", "if", "while", "do", "return").contains(nextToken)) {
         if (nextToken.equals("let")) {
            compileLetStatement();
         } else if (nextToken.equals("if")) {
            compileIfStatement();
         } else if (nextToken.equals("while")) {
            compileWhileStatement();
         } else if (nextToken.equals("do")) {
            compileDoStatement();
         } else if (nextToken.equals("return")) {
            compileReturnStatement();
         }
         nextToken = tokenizer.peek().getKey();
      }
      System.out.println("</statements>");
   }

   public void compileLetStatement() {
      System.out.println("<letStatement>");
      eatKeyword("let");
      String idenName = eatIdentifier();
      String idenType = symbolTable.typeOf(idenName);
      int idenIndex = symbolTable.indexOf(idenName);
      Segment idenSegment = segmentMap.get(symbolTable.kindOf(idenName));

      String nextToken = tokenizer.peek().getKey();
      if (nextToken.equals("[")) {
         codeWriter.writePush(idenSegment, idenIndex);

         eatSymbol('[');
         compileExpression();

         codeWriter.writeArithmetic(Command.ADD);

         eatSymbol(']');
      }

      eatSymbol('=');
      compileExpression();

      if (nextToken.equals("[")) {
         codeWriter.writePop(Segment.TEMP, 0);
         codeWriter.writePop(Segment.POINTER, 1);
         codeWriter.writePush(Segment.TEMP, 0);
         codeWriter.writePop(Segment.THAT, 0);
      } else {
         codeWriter.writePop(idenSegment, idenIndex);
      }

      eatSymbol(';');
      System.out.println("</letStatement>");
   }

   public void compileIfStatement() {
      System.out.println("<ifStatement>");

      eatKeyword("if");
      eatSymbol("(");
      compileExpression();
      eatSymbol(")");
      eatSymbol("{");

      codeWriter.writeArithmetic(Command.NOT);

      String falseLabel = "IF_LABEL_FALSE." + condLabelIndex.get("if");
      String endLabel = "IF_LABEL_END." + condLabelIndex.get("if");
      condLabelIndex.put("if", condLabelIndex.get("if") + 1);

      codeWriter.writeIf(falseLabel);
      compileStatements();

      codeWriter.writeGoto(endLabel);

      eatSymbol("}");

      codeWriter.writeLabel(falseLabel);
      String nextToken = tokenizer.peek().getKey();
      if (nextToken.equals("else")) {
         eatKeyword("else");
         eatSymbol("{");
         compileStatements();
         eatSymbol("}");
      }

      codeWriter.writeLabel(endLabel);
      System.out.println("</ifStatement>");
   }

   public void compileWhileStatement() {
      System.out.println("<whileStatement>");

      String startLabel = "WHILE_LABEL_START." + condLabelIndex.get("while");
      String falseLabel = "WHILE_LABEL_ELSE." + condLabelIndex.get("while");
      condLabelIndex.put("while", condLabelIndex.get("while") + 1);

      codeWriter.writeLabel(startLabel);

      eatKeyword("while");
      eatSymbol("(");

      compileExpression();

      codeWriter.writeArithmetic(Command.NOT);
      codeWriter.writeIf(falseLabel);

      eatSymbol(")");
      eatSymbol("{");
      compileStatements();
      eatSymbol("}");

      codeWriter.writeGoto(startLabel);
      codeWriter.writeLabel(falseLabel);

      String nextToken = tokenizer.peek().getKey();
      if (nextToken.equals("else")) {
         eatKeyword("else");
         eatSymbol("{");
         compileStatements();
         eatSymbol("}");
      }

      System.out.println("</whileStatement>");
   }

   public void compileDoStatement() {
      System.out.println("<doStatement>");
      eatKeyword("do");
      compileSubroutineCall();
      eatSymbol(";");

      codeWriter.writePop(Segment.TEMP, 0);
      System.out.println("</doStatement>");
   }

   public void compileReturnStatement() {
      System.out.println("<returnStatement>");
      eatKeyword("return");

      String nextToken = tokenizer.peek().getKey();
      if (!nextToken.equals(";")) {
         compileExpression();
      }

      eatSymbol(";");
      System.out.println("</returnStatement>");
   }

   public void compileExpression() {
      System.out.println("<expression>");
      compileTerm();
      compileTermOperationsZeroOrMore();
      System.out.println("</expression>");
   }

   public void compileTermOperationsZeroOrMore() {
      String nextToken = tokenizer.peek().getKey();
      List<String> binaryOperators = Arrays.asList("+", "-", "*", "/", "|", "&", "<", ">", "=");
      while (binaryOperators.contains(nextToken)) {
         eatSymbol(nextToken);
         compileTerm();
         switch (nextToken) {
            case "+":
               codeWriter.writeArithmetic(Command.ADD);
               break;
            case "*":
               codeWriter.writeCall("Math.multiply", 2);
               break;
            case "/":
               codeWriter.writeCall("Math.divide", 2);
               break;
            case "-":
               codeWriter.writeArithmetic(Command.SUB);
               break;
            case "|":
               codeWriter.writeArithmetic(Command.OR);
               break;
            case "&":
               codeWriter.writeArithmetic(Command.AND);
               break;
            case "<":
               codeWriter.writeArithmetic(Command.LT);
               break;
            case ">":
               codeWriter.writeArithmetic(Command.GT);
               break;
            case "=":
               codeWriter.writeArithmetic(Command.EQ);
               break;
         }
         nextToken = tokenizer.peek().getKey();
      }
   }

   public void compileTerm() {
      System.out.println("<term>");
      String nextToken = tokenizer.peek().getKey();
      Token nextType = tokenizer.peek().getValue();
      List<Token> lhsLiteralTypes = Arrays.asList(Token.INT_CONSTANT, Token.STRING_CONSTANT, Token.KEYWORD);
      if (lhsLiteralTypes.contains(nextType)) {
         if (nextType == Token.INT_CONSTANT) {
            int value = eatInteger();
            codeWriter.writePush(Segment.CONST, value);
         } else if (nextType == Token.STRING_CONSTANT) {
            String value = eatString();
            int strLength = value.length();
            codeWriter.writePush(Segment.CONST, strLength);
            codeWriter.writeCall("String.new", 1);
            for (char ch : value.toCharArray()) {
               codeWriter.writePush(Segment.CONST, (int) ch);
               codeWriter.writeCall("String.appendChar", 2);
            }
         } else {
            String value = eatKeywordOr(Arrays.asList("true", "false", "null", "this"));
            if (value.equals("true")) {
               codeWriter.writePush(Segment.CONST, 0);
               codeWriter.writeArithmetic(Command.NOT);
            } else if (value.equals("false")) {
               codeWriter.writePush(Segment.CONST, 0);
            } else if (value.equals("null")) {
               codeWriter.writePush(Segment.CONST, 0);
            } else if (value.equals("this")) {
               codeWriter.writePush(Segment.POINTER, 0);
            }
         }
      } else if (nextType == Token.SYMBOL) {
         if (nextToken.equals("(")) {
            eatSymbol("(");
            compileExpression();
            eatSymbol(")");
         } else {
            String value = eatSymbolOr(Arrays.asList("-", "~"));
            compileTerm();
            if (value.equals("-")) {
               codeWriter.writeArithmetic(Command.NEG);
            } else if (value.equals("~")) {
               codeWriter.writeArithmetic(Command.NOT);
            }
         }
      } else {
         String idenName = eatIdentifier();
         nextToken = tokenizer.peek().getKey();
         if (nextToken.equals("[")) {
            int idenIndex = symbolTable.indexOf(idenName);
            Segment segment = segmentMap.get(symbolTable.kindOf(idenName));
            codeWriter.writePush(segment, idenIndex);

            eatSymbol('[');
            compileExpression();

            codeWriter.writeArithmetic(Command.ADD);
            codeWriter.writePop(Segment.POINTER, 1);
            codeWriter.writePush(Segment.THAT, 0);

            eatSymbol(']');
         } else if (nextToken.equals("(") || nextToken.equals(".")) {
            compileSubroutineCallArgsAndBody(idenName);
         } else {
            int idenIndex = symbolTable.indexOf(idenName);
            Segment segment = segmentMap.get(symbolTable.kindOf(idenName));
            codeWriter.writePush(segment, idenIndex);
         }
      }
      System.out.println("</term>");
   }

   // ... (continue with the rest of the code)

   public void compileSubroutineCall() {
      String baseIden = eatIdentifier();
      compileSubroutineCallArgsAndBody(baseIden);
   }

   public void compileSubroutineCallArgsAndBody(String baseIden) {
      String nextToken = tokenizer.peek().getKey();
      Token nextType = tokenizer.peek().getValue();
      int nargs = 0;

      // Implicit method
      if (nextToken.equals("(")) {
         eatSymbol('(');
         codeWriter.writePush(Segment.POINTER, 0);
         nargs = compileExpressionList();
         eatSymbol(')');
         codeWriter.writeCall(String.format("%s.%s", className, baseIden), nargs + 1);
      } else { // Method or regular call
         eatSymbol('.');
         String fnIden = eatIdentifier();
         boolean baseIsInstance = Character.isLowerCase(baseIden.charAt(0));

         if (baseIsInstance) {
            int idenIndex = symbolTable.indexOf(baseIden);
            Segment segment = segmentMap.get(symbolTable.kindOf(baseIden));
            codeWriter.writePush(segment, idenIndex);
         }

         eatSymbol('(');
         nargs = compileExpressionList();
         eatSymbol(')');

         if (baseIsInstance) {
            String baseClassName = symbolTable.typeOf(baseIden);
            String fullName = String.format("%s.%s", baseClassName, fnIden);
            codeWriter.writeCall(fullName, nargs + 1);
         } else {
            codeWriter.writeCall(String.format("%s.%s", baseIden, fnIden), nargs);
         }
      }
   }

   public int compileExpressionList() {
      System.out.println("<expressionList>");
      int count = 0;
      String nextToken = tokenizer.peek().getKey();
      if (!nextToken.equals(")")) {
         compileExpression();
         count++;
         count += compileExpressionZeroOrMore();
      }
      System.out.println("</expressionList>");
      return count;
   }

   public int compileExpressionZeroOrMore() {
      String nextToken = tokenizer.peek().getKey();
      int count = 0;
      while (nextToken.equals(",")) {
         eatSymbol(',');
         compileExpression();
         count++;
         nextToken = tokenizer.peek().getKey();
      }
      return count;
   }

   public void eatSymbol(String value) {
      tokenizer.advance();
      if (!tokenizer.symbol().equals(value)) {
         throw new RuntimeException("Missing symbol: " + value);
      }
      System.out.println(tokenizer.xml());
   }

   public int eatInteger() {
      tokenizer.advance();
      if (tokenizer.tokenType() != Token.INT_CONSTANT) {
         throw new RuntimeException("Missing integer");
      }
      System.out.println(tokenizer.xml());
      return tokenizer.intValue();
   }

   public String eatString() {
      tokenizer.advance();
      if (tokenizer.tokenType() != Token.STRING_CONSTANT) {
         throw new RuntimeException("Missing string");
      }
      System.out.println(tokenizer.xml());
      return tokenizer.stringValue();
   }

   public String eatKeyword(String value) {
      tokenizer.advance();
      if (tokenizer.keyWord() != value) {
         throw new RuntimeException("Missing keyword: " + value);
      }
      System.out.println(tokenizer.xml());
      return tokenizer.keyWord();
   }

   public String eatKeywordOr(Set<String> values) {
      tokenizer.advance();
      if (!values.contains(tokenizer.keyWord())) {
         throw new RuntimeException("Missing keywords: " + values);
      }
      System.out.println(tokenizer.xml());
      return tokenizer.keyWord();
   }

   public String eatSymbolOr(Set<String> values) {
      tokenizer.advance();
      if (!values.contains(tokenizer.symbol())) {
         throw new RuntimeException("Missing symbols: " + values);
      }
      System.out.println(tokenizer.xml());
      return tokenizer.symbol();
   }

   public String eatIdentifier() {
      tokenizer.advance();
      if (tokenizer.tokenType() != Token.IDENTIFIER) {
         throw new RuntimeException("Missing identifier");
      }
      System.out.println(tokenizer.xml());
      return tokenizer.identifier();
   }

   public static void main(String[] args) {
      if (args.length < 1) {
         throw new RuntimeException("You need to supply an input file");
      }

      String filename = args[0];
      if (new File(filename).isDirectory()) {
         List<String> files = readFiles(filename);
         for (String fileName : files) {
            compileFile(fileName);
         }
      } else {
         compileFile(filename);
      }
   }

   public static List<String> readFiles(String folderPath) {
      File folder = new File(folderPath);
      File[] fileList = folder.listFiles();
      List<String> files = new ArrayList<>();
      if (fileList != null) {
         for (File file : fileList) {
            if (file.getName().endsWith(".jack")) {
               files.add(file.getAbsolutePath());
            }
         }
      }
      return files;
   }

   public static void compileFile(String filename) {
      try {
         String jackCode = Files.readString(Path.of(filename));
         CompEngine engine = new CompEngine(jackCode);
         engine.compileClass();
         String vmName = filename.replace(".jack", ".vm");
         try (FileWriter fileWriter = new FileWriter(vmName)) {
            String output = engine.codeWriter.output();
            fileWriter.write(output);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}