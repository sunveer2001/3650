import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class Parser {
  private static final char A = 0;
  private static final char C = 1;
  private static final char L = 2;

  private Map<String, Integer> symbols = new HashMap<String, Integer>(50);
  private BufferedReader br;
  private int address = 16;

  public Parser(String file) {
   try {
       br = new BufferedReader(new FileReader(file));
       String[] regs = {
           "SP", "LCL", "ARG", "THIS", "THAT", "SCREEN", "KBD",
           "R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7", "R8", "R9", "R10", "R11", "R12", "R13", "R14", "R15"
       };

       for (int i = 0; i < regs.length; i++) {
           symbols.put(regs[i], i);
       }

       if (br != null) br.close();
       br = new BufferedReader(new FileReader(file));
   } catch (Exception e) {
       e.printStackTrace();
   }
}

private char type(String command) {
   char firstChar = command.charAt(0);
   switch (firstChar) {
       case '@':
           return A;
       case '(':
           return L;
       default:
           return C;
   }
}

private char target(String command) {
   int equal = command.indexOf('=');

   if (equal == -1) return 0; 
   
   String left = command.substring(0, equal);
   char res = 0;
   if (left.contains("A")) res = 4;
   if (left.contains("D")) res = 2;
   if (left.contains("M")) res = 1;
   return res;
}

private char convert(String command) {
   String s = command.replaceAll(".*=", "").replaceAll(";.*", "");
   String[] instruct = {
       "0",   "1",  "-1",  "D",   "A",   "!D",  "!A",  "-D",
       "-A",  "D+1", "A+1", "D-1", "A-1", "D+A", "D-A", "A-D",
       "D&A", "D|A", "M",   "!M",  "-M",  "M+1", "M-1", "D+M",
       "D-M", "M-D", "D&M", "D|M"
   };

   char[] binaryValues = {
       0b0101010, 0b0111111, 0b0111010, 0b0001100, 0b0110000, 0b0001101, 0b0110001, 0b0001111,
       0b0110011, 0b0011111, 0b0110111, 0b0001110, 0b0110010, 0b0000010, 0b0010011, 0b0000111,
       0b0000000, 0b0010101, 0b1110000, 0b1110001, 0b1110011, 0b1110111, 0b1110010, 0b1000010,
       0b1010011, 0b1000111, 0b1000000, 0b1010101
   };
   
   for (int i = 0; i < instruct.length; i++) {
       if (s.equals(instruct[i])) return binaryValues[i];
   }
   return null;
}


private char jump(String command) {
   if (command.indexOf(';') == -1) return 0;
   String right = command.replaceAll(".*;", "");
   String[] jumpInstruct = {"JGT", "JEQ", "JGE", "JLT", "JNE", "JLE", "JMP"};
   char[] jumpValues = {0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111};

   for (int i = 0; i < jumpInstruct.length; i++) {
       if (right.equals(jumpInstruct[i])) return jumpValues[i];
   }
   return 0; 
}

   private String nextIn() {
      String current;
      while ((current = br.readLine()) != null) {
         current = current.replaceAll("\\s+|//.*", "").trim();
         if (!current.isEmpty()) {
            return current;
         }
      }
      if (br != null) br.close();
      return null;
   }

  public String nextOut() {
   String s;
   while ((s = nextIn()) != null && type(s) == L);
   if (s == null) return null;

   if (type(s) == A) {
       String value = s.substring(1);
       int addr = Character.isDigit(value.charAt(0)) ? Integer.parseInt(value) : symbols.getOrDefault(value, address++);
       return String.format("%16s", Integer.toBinaryString(addr)).replace(' ', '0');
   }

   int raw = 0b1110000000000000 + (convert(s) << 6) + (target(s) << 3) + jump(s);
   return String.format("%16s", Integer.toBinaryString(raw)).replace(' ', '0');
}

   public static void main(String[] args) {
      Parser p = new Parser(args[0]);
      for (String s; (s = p.nextOut()) != null; ) {
         System.out.println(s);   
      }
   }

}
