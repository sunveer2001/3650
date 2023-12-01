import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum Token {
    KEYWORD, SYMBOL, IDENTIFIER, INT_CONSTANT, STRING_CONSTANT
}

public class Tokenizer {
    private String sourceText;
    private String currentToken;
    private Token currentType;
    private Token nextType;
    private String key;

    public Tokenizer(String sourceText) {
        this.sourceText = sourceText;
        this.currentToken = null;
        this.currentType = null;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String advance() {
        while (matchWhitespace()) {
            sourceText = sourceText.trim();
        }

        while (matchComment() != null) {
            String commentMatch = matchComment();
            int size = commentMatch.length();
            sourceText = sourceText.substring(size);
            while (matchWhitespace()) {
                sourceText = sourceText.trim();
            }
        }

        Map.Entry<String, Token> tokenEntry = matchesToken();
        if (tokenEntry != null) {
            String value = tokenEntry.getKey();
            currentToken = value;
            currentType = tokenEntry.getValue();
            sourceText = sourceText.substring(value.length());
            return currentToken;
        }

        return null;
    }

    public Map<String, Token> peek() {
        String originalToken = currentToken;
        String originalSourceText = sourceText;
        Token originalType = currentType;
        Token nextType = currentType;

        advance();
        String nextToken = currentToken;
        sourceText = originalSourceText;
        currentToken = originalToken;
        currentType = originalType;

        Map<String, Token> tokens = new HashMap<>();
        tokens.put(nextToken, nextType);
        return tokens;
    }

    private Map.Entry<String, Token> matchesToken() {
        Map.Entry<String, Token> match = matchKeyword();
        if (match != null) {
            return match;
        }

        match = matchIdentifier();
        if (match != null) {
            return match;
        }

        match = matchInteger();
        if (match != null) {
            return match;
        }

        match = matchSymbol();
        if (match != null) {
            return match;
        }

        match = matchString();
        if (match != null) {
            return match;
        }

        return null;
    }

    private String matchComment() {
        String res = matchInlineComment();
        if (res == null) {
            return matchBlockComment();
        }
        return res;
    }

    private String matchInlineComment() {
        Pattern p = Pattern.compile("//.+");
        Matcher matcher = p.matcher(sourceText);
        if (matcher.matches()) {
            return matcher.group();
        }
        return null;
    }

    private String matchBlockComment() {
        Pattern p = Pattern.compile("/\\*(.|\\n)*?\\*/");
        Matcher matcher = p.matcher(sourceText);
        if (matcher.matches()) {
            return matcher.group();
        }
        return null;
    }

    private Map.Entry<String, Token> matchIdentifier() {
        Pattern p = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
        Matcher matcher = p.matcher(sourceText);
        if (matcher.matches()) {
            return Map.entry(matcher.group(), Token.IDENTIFIER);
        }
        return null;
    }

    private Map.Entry<String, Token> matchString() {
        Pattern p = Pattern.compile("\"[^\"]+\"");
        Matcher matcher = p.matcher(sourceText);
        if (matcher.matches()) {
            return Map.entry(matcher.group(), Token.STRING_CONSTANT);
        }
        return null;
    }

    private Map.Entry<String, Token> matchInteger() {
        Pattern p = Pattern.compile("\\d+");
        Matcher matcher = p.matcher(sourceText);
        if (matcher.matches()) {
            return Map.entry(matcher.group(), Token.INT_CONSTANT);
        }
        return null;
    }

    private Map.Entry<String, Token> matchSymbol() {
        Pattern p = Pattern.compile("[\\{\\}\\(\\)\\[\\]\\.\\,\\;\\+\\-\\*\\/\\&\\|\\<\\>\\=\\~]");
        Matcher matcher = p.matcher(sourceText);
        if (matcher.matches()) {
            return Map.entry(matcher.group(), Token.SYMBOL);
        }
        return null;
    }

    private Map.Entry<String, Token> matchKeyword() {
        String endInSpace = "(class|constructor|function|method|field|static|var|int|char|boolean|void|let|do)\\b";
        String other = "true|false|null|this|if|else|while|return";
        Pattern p1 = Pattern.compile(endInSpace);
        Matcher matcher = p1.matcher(sourceText);
        if (matcher.matches()) {
            return Map.entry(matcher.group(), Token.KEYWORD);
        }

        Pattern p2 = Pattern.compile(other);
        matcher = p2.matcher(sourceText);
        if (matcher.matches()) {
            return Map.entry(matcher.group(), Token.KEYWORD);
        }

        return null;
    }

    private boolean matchWhitespace() {
        Pattern p = Pattern.compile("\\s+");
        Matcher matcher = p.matcher(sourceText);
        return matcher.matches();
    }

    public Token tokenType() {
        return currentType;
    }

    public String keyWord() {
        if (tokenType() == Token.KEYWORD) {
            return currentToken;
        }
        return null;
    }

    public String symbol() {
        if (tokenType() == Token.SYMBOL) {
            return currentToken;
        }
        return null;
    }

    public String identifier() {
        if (tokenType() == Token.IDENTIFIER) {
            return currentToken;
        }
        return null;
    }

    public int intValue() {
        if (tokenType() == Token.INT_CONSTANT) {
            return Integer.parseInt(currentToken);
        }
        return -1;
    }

    public String stringValue() {
        if (tokenType() == Token.STRING_CONSTANT) {
            return currentToken.replace("\"", "");
        }
        return null;
    }

    public Object tokenValue() {
        if (tokenType() == Token.KEYWORD) {
            return currentToken;
        }
        if (tokenType() == Token.SYMBOL) {
            return symbol();
        }
        if (tokenType() == Token.IDENTIFIER) {
            return identifier();
        }
        if (tokenType() == Token.INT_CONSTANT) {
            return intValue();
        }
        if (tokenType() == Token.STRING_CONSTANT) {
            return stringValue();
        }
        return null;
    }

    public String xml() {
        Map<String, String> symbolRewrite = Map.of("<", "&lt;", ">", "&gt;", "\"", "&quot;", "&", "&amp;");
        StringBuilder result = new StringBuilder();
        if (tokenType() == Token.KEYWORD) {
            result.append("<keyword> ").append(currentToken).append(" </keyword>");
        } else if (tokenType() == Token.SYMBOL) {
            result.append("<symbol> ").append(symbolRewrite.getOrDefault(symbol(), symbol())).append(" </symbol>");
        } else if (tokenType() == Token.IDENTIFIER) {
            result.append("<identifier> ").append(identifier()).append(" </identifier>");
        } else if (tokenType() == Token.INT_CONSTANT) {
            result.append("<integerConstant> ").append(intValue()).append(" </integerConstant>");
        } else if (tokenType() == Token.STRING_CONSTANT) {
            result.append("<stringConstant> ").append(stringValue()).append(" </stringConstant>");
        }
        return result.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("You need to supply an input file");
        }
        String fileName = args[0];
        tokenize(fileName);
    }

    public static void tokenize(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            System.out.println("<tokens>");
            Tokenizer tokenizer = new Tokenizer(reader.readLine());
            while (tokenizer.advance() != null) {
                System.out.println(tokenizer.xml());
            }
            System.out.println("</tokens>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
