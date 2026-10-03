import java.util.*;
import java.util.function.Function;

/**
 * MiniJavaParser
 * Tokenizer, recursive-descent parser, and AST node definitions for drone scripts.
 * Supports while, do-while, for, for-each, switch-case, if-else, functions, and arrays.
 */
public class MiniJavaParser {

    // ==========================================
    // AST Interfaces
    // ==========================================
    public interface Statement {
        void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException;
    }

    public interface Expression {
        Object evaluate(MiniJavaInterpreter.Environment env);
    }

    // ==========================================
    // Token Representation
    // ==========================================
    public static class Token {
        public final String text;
        public final int line;

        public Token(String text, int line) {
            this.text = text;
            this.line = line;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    // ==========================================
    // Lexer / Tokenizer
    // ==========================================
    public static List<Token> tokenize(String code) {
        List<Token> tokens = new ArrayList<>();
        int line = 1;
        int i = 0;
        int n = code.length();

        while (i < n) {
            char c = code.charAt(i);

            if (c == '\n') {
                line++;
                i++;
                continue;
            }
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Single line comment: // ...
            if (c == '/' && i + 1 < n && code.charAt(i + 1) == '/') {
                while (i < n && code.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            // Multi-line comment: /* ... */
            if (c == '/' && i + 1 < n && code.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) {
                    if (code.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                }
                i = Math.min(n, i + 2);
                continue;
            }

            // String and character literals: "...", '...'
            if (c == '"' || c == '\'') {
                char quote = c;
                int start = i;
                i++;
                while (i < n && code.charAt(i) != quote) {
                    if (code.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    i++;
                }
                if (i < n) {
                    i++;
                }
                tokens.add(new Token(code.substring(start, i), line));
                continue;
            }

            // Multi-character operators
            String[] multiOps = {"==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/="};
            boolean matched = false;
            for (String op : multiOps) {
                if (code.startsWith(op, i)) {
                    tokens.add(new Token(op, line));
                    i += op.length();
                    matched = true;
                    break;
                }
            }
            if (matched) {
                continue;
            }

            // Single character punctuation (including colon ':' for switch and brackets '[]' for arrays)
            if ("{}();,.<>+-*/%=!&|:[]".indexOf(c) != -1) {
                tokens.add(new Token(String.valueOf(c), line));
                i++;
                continue;
            }

            // Identifiers, keywords and numeric literals
            int start = i;
            while (i < n) {
                char ch = code.charAt(i);
                if (Character.isWhitespace(ch) || "{}();,.<>+-*/%=!&|:[]\"'".indexOf(ch) != -1) {
                    break;
                }
                i++;
            }
            if (i > start) {
                tokens.add(new Token(code.substring(start, i), line));
            }
        }
        return tokens;
    }

    // ==========================================
    // Parser Engine
    // ==========================================
    public static List<Statement> parse(String code) {
        List<Token> tokens = tokenize(code);
        int[] index = new int[]{0};
        List<Statement> statements = new ArrayList<>();

        while (index[0] < tokens.size()) {
            Statement s = parseStatement(tokens, index);
            if (s != null) {
                statements.add(s);
            }
        }
        return statements;
    }

    private static Statement parseStatement(List<Token> tokens, int[] index) {
        if (index[0] >= tokens.size()) {
            return null;
        }

        Token tok = tokens.get(index[0]);

        if (tok.text.equals(";")) {
            index[0]++;
            return null;
        }

        // Block statement: { ... }
        if (tok.text.equals("{")) {
            index[0]++;
            List<Statement> body = new ArrayList<>();
            while (index[0] < tokens.size() && !tokens.get(index[0]).text.equals("}")) {
                Statement s = parseStatement(tokens, index);
                if (s != null) {
                    body.add(s);
                }
            }
            if (index[0] < tokens.size() && tokens.get(index[0]).text.equals("}")) {
                index[0]++;
            }
            return new BlockStatement(tok.line, body);
        }

        // While loop: while (cond) { ... }
        if (tok.text.equals("while")) {
            int line = tok.line;
            index[0]++;
            Expression cond = parseParenExpression(tokens, index);
            Statement body = parseStatement(tokens, index);
            return new WhileStatement(line, cond, body);
        }

        // Do-while loop: do { ... } while (cond);
        if (tok.text.equals("do")) {
            int line = tok.line;
            index[0]++;
            Statement body = parseStatement(tokens, index);
            consume(tokens, index, "while");
            Expression cond = parseParenExpression(tokens, index);
            consumeOptional(tokens, index, ";");
            return new DoWhileStatement(line, body, cond);
        }

        // For loop: standard for(...) or enhanced for-each for(Type x : list)
        if (tok.text.equals("for")) {
            int line = tok.line;
            index[0]++;
            consume(tokens, index, "(");

            int colonIdx = findColonInForHeader(tokens, index[0]);
            if (colonIdx != -1) {
                String varName;
                if (isTypeKeyword(tokens.get(index[0]).text)) {
                    index[0]++;
                    if (index[0] + 1 < tokens.size() && tokens.get(index[0]).text.equals("[") && tokens.get(index[0] + 1).text.equals("]")) {
                        index[0] += 2;
                    }
                }
                varName = tokens.get(index[0]).text;
                index[0]++;
                consume(tokens, index, ":");
                Expression iterable = parseExpressionUntil(tokens, index, ")");
                consume(tokens, index, ")");
                Statement body = parseStatement(tokens, index);
                return new ForEachStatement(line, varName, iterable, body);
            } else {
                Statement init = parseSimpleStatement(tokens, index);
                Expression cond = parseExpressionUntil(tokens, index, ";");
                consume(tokens, index, ";");
                Statement update = parseSimpleStatementWithoutSemicolon(tokens, index);
                consume(tokens, index, ")");
                Statement body = parseStatement(tokens, index);
                return new ForStatement(line, init, cond, update, body);
            }
        }

        // Switch statement: switch (expr) { case ...: ... default: ... }
        if (tok.text.equals("switch")) {
            int line = tok.line;
            index[0]++;
            Expression target = parseParenExpression(tokens, index);
            consume(tokens, index, "{");
            List<SwitchStatement.SwitchCase> cases = new ArrayList<>();

            while (index[0] < tokens.size() && !tokens.get(index[0]).text.equals("}")) {
                Token cTok = tokens.get(index[0]);
                if (cTok.text.equals("case")) {
                    index[0]++;
                    Expression caseVal = parseExpressionUntil(tokens, index, ":");
                    consume(tokens, index, ":");
                    List<Statement> caseBody = new ArrayList<>();
                    while (index[0] < tokens.size() && !tokens.get(index[0]).text.equals("case")
                            && !tokens.get(index[0]).text.equals("default") && !tokens.get(index[0]).text.equals("}")) {
                        Statement s = parseStatement(tokens, index);
                        if (s != null) {
                            caseBody.add(s);
                        }
                    }
                    cases.add(new SwitchStatement.SwitchCase(caseVal, caseBody));
                } else if (cTok.text.equals("default")) {
                    index[0]++;
                    consume(tokens, index, ":");
                    List<Statement> defBody = new ArrayList<>();
                    while (index[0] < tokens.size() && !tokens.get(index[0]).text.equals("case")
                            && !tokens.get(index[0]).text.equals("default") && !tokens.get(index[0]).text.equals("}")) {
                        Statement s = parseStatement(tokens, index);
                        if (s != null) {
                            defBody.add(s);
                        }
                    }
                    cases.add(new SwitchStatement.SwitchCase(null, defBody));
                } else {
                    index[0]++;
                }
            }
            consume(tokens, index, "}");
            return new SwitchStatement(line, target, cases);
        }

        // If statement: if (cond) then else
        if (tok.text.equals("if")) {
            int line = tok.line;
            index[0]++;
            Expression cond = parseParenExpression(tokens, index);
            Statement thenStmt = parseStatement(tokens, index);
            Statement elseStmt = null;

            if (index[0] < tokens.size() && tokens.get(index[0]).text.equals("else")) {
                index[0]++;
                elseStmt = parseStatement(tokens, index);
            }
            return new IfStatement(line, cond, thenStmt, elseStmt);
        }

        // Function declaration: void myFunc(...) { ... }
        if (isTypeKeyword(tok.text) && index[0] + 2 < tokens.size() && tokens.get(index[0] + 2).text.equals("(")) {
            int line = tok.line;
            index[0]++; // Skip return type
            String name = tokens.get(index[0]).text;
            index[0]++;
            consume(tokens, index, "(");
            List<String> params = new ArrayList<>();
            while (index[0] < tokens.size() && !tokens.get(index[0]).text.equals(")")) {
                if (isTypeKeyword(tokens.get(index[0]).text)) {
                    index[0]++;
                    if (index[0] + 1 < tokens.size() && tokens.get(index[0]).text.equals("[") && tokens.get(index[0] + 1).text.equals("]")) {
                        index[0] += 2;
                    }
                }
                if (index[0] < tokens.size() && !tokens.get(index[0]).text.equals(")") && !tokens.get(index[0]).text.equals(",")) {
                    params.add(tokens.get(index[0]).text);
                    index[0]++;
                }
                if (index[0] < tokens.size() && tokens.get(index[0]).text.equals(",")) {
                    index[0]++;
                }
            }
            consume(tokens, index, ")");
            Statement body = parseStatement(tokens, index);
            return new DefStatement(line, name, params, body);
        }

        // Return statement
        if (tok.text.equals("return")) {
            int line = tok.line;
            index[0]++;
            Expression expr = null;
            if (index[0] < tokens.size() && !tokens.get(index[0]).text.equals(";")) {
                expr = parseExpressionUntil(tokens, index, ";");
            }
            consumeOptional(tokens, index, ";");
            return new ReturnStatement(line, expr);
        }

        // Break statement
        if (tok.text.equals("break")) {
            index[0]++;
            consumeOptional(tokens, index, ";");
            return new BreakStatement(tok.line);
        }

        // Continue statement
        if (tok.text.equals("continue")) {
            index[0]++;
            consumeOptional(tokens, index, ";");
            return new ContinueStatement(tok.line);
        }

        // Variable declaration: int x = 5; or String[] arr = {"A", "B"};
        if (isTypeKeyword(tok.text)) {
            int line = tok.line;
            index[0]++;
            if (index[0] + 1 < tokens.size() && tokens.get(index[0]).text.equals("[") && tokens.get(index[0] + 1).text.equals("]")) {
                index[0] += 2;
            }
            String varName = tokens.get(index[0]).text;
            index[0]++;
            if (index[0] + 1 < tokens.size() && tokens.get(index[0]).text.equals("[") && tokens.get(index[0] + 1).text.equals("]")) {
                index[0] += 2;
            }

            Expression initExpr = null;
            if (index[0] < tokens.size() && tokens.get(index[0]).text.equals("=")) {
                index[0]++;
                initExpr = parseExpressionUntil(tokens, index, ";");
            }
            consumeOptional(tokens, index, ";");
            return new VarDeclStatement(line, varName, initExpr);
        }

        return parseSimpleStatement(tokens, index);
    }

    private static int findColonInForHeader(List<Token> tokens, int startIndex) {
        int depth = 1;
        for (int i = startIndex; i < tokens.size(); i++) {
            String text = tokens.get(i).text;
            if (text.equals("(")) depth++;
            else if (text.equals(")")) {
                depth--;
                if (depth == 0) return -1;
            } else if (text.equals(":") && depth == 1) {
                return i;
            } else if (text.equals(";") && depth == 1) {
                return -1;
            }
        }
        return -1;
    }

    private static Statement parseSimpleStatement(List<Token> tokens, int[] index) {
        Statement s = parseSimpleStatementWithoutSemicolon(tokens, index);
        consumeOptional(tokens, index, ";");
        return s;
    }

    private static Statement parseSimpleStatementWithoutSemicolon(List<Token> tokens, int[] index) {
        if (index[0] >= tokens.size()) {
            return null;
        }

        int line = tokens.get(index[0]).line;

        // Assignments (x = 5, x++, x--, x += 2, etc.)
        if (index[0] + 1 < tokens.size()) {
            String next = tokens.get(index[0] + 1).text;
            String varName = tokens.get(index[0]).text;

            if (next.equals("++")) {
                index[0] += 2;
                return new IncDecStatement(line, varName, 1);
            }
            if (next.equals("--")) {
                index[0] += 2;
                return new IncDecStatement(line, varName, -1);
            }
            if (next.equals("=")) {
                index[0] += 2;
                Expression expr = parseExpressionUntil(tokens, index, ";", ")", ",");
                return new AssignStatement(line, varName, expr);
            }
            if (next.equals("+=")) {
                index[0] += 2;
                Expression expr = parseExpressionUntil(tokens, index, ";", ")", ",");
                return new AugAssignStatement(line, varName, "+", expr);
            }
            if (next.equals("-=")) {
                index[0] += 2;
                Expression expr = parseExpressionUntil(tokens, index, ";", ")", ",");
                return new AugAssignStatement(line, varName, "-", expr);
            }
        }

        Expression expr = parseExpressionUntil(tokens, index, ";", ")", ",");
        return new ExprStatement(line, expr);
    }

    private static Expression parseParenExpression(List<Token> tokens, int[] index) {
        consume(tokens, index, "(");
        int depth = 1;
        List<Token> exprTokens = new ArrayList<>();
        while (index[0] < tokens.size() && depth > 0) {
            Token t = tokens.get(index[0]++);
            if (t.text.equals("(")) {
                depth++;
            } else if (t.text.equals(")")) {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
            exprTokens.add(t);
        }
        return buildExpression(exprTokens);
    }

    private static Expression parseExpressionUntil(List<Token> tokens, int[] index, String... terminators) {
        List<Token> exprTokens = new ArrayList<>();
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;

        Set<String> termSet = new HashSet<>(Arrays.asList(terminators));

        while (index[0] < tokens.size()) {
            Token t = tokens.get(index[0]);
            if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0 && termSet.contains(t.text)) {
                break;
            }
            if (t.text.equals("(")) {
                parenDepth++;
            } else if (t.text.equals(")")) {
                parenDepth--;
            } else if (t.text.equals("{")) {
                braceDepth++;
            } else if (t.text.equals("}")) {
                braceDepth--;
            } else if (t.text.equals("[")) {
                bracketDepth++;
            } else if (t.text.equals("]")) {
                bracketDepth--;
            }

            exprTokens.add(t);
            index[0]++;
        }
        return buildExpression(exprTokens);
    }

    private static void consume(List<Token> tokens, int[] index, String expected) {
        if (index[0] < tokens.size() && tokens.get(index[0]).text.equals(expected)) {
            index[0]++;
        }
    }

    private static void consumeOptional(List<Token> tokens, int[] index, String expected) {
        if (index[0] < tokens.size() && tokens.get(index[0]).text.equals(expected)) {
            index[0]++;
        }
    }

    private static boolean isTypeKeyword(String text) {
        return text.equals("int") || text.equals("boolean") || text.equals("String")
                || text.equals("void") || text.equals("double") || text.equals("float")
                || text.equals("long") || text.equals("char") || text.equals("var");
    }

    // ==========================================
    // Expression Builder
    // ==========================================
    public static Expression buildExpression(List<Token> tokens) {
        if (tokens.isEmpty()) {
            return env -> null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(tokens.get(i).text);
        }
        String exprStr = sb.toString().trim();
        return parseStringExpression(exprStr);
    }

    public static Expression parseStringExpression(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return env -> null;
        }

        // Array literal: { "A", "B" } or [ "A", "B" ]
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            List<Expression> elementExprs = parseArgList(inner);
            return env -> {
                List<Object> list = new ArrayList<>();
                for (Expression e : elementExprs) {
                    list.add(e.evaluate(env));
                }
                return list;
            };
        }

        // Array access: arr[0] or crops[i]
        int openBracket = findTopLevelOp(trimmed, "[");
        if (openBracket != -1 && trimmed.endsWith("]")) {
            String arrayName = trimmed.substring(0, openBracket).trim();
            String indexStr = trimmed.substring(openBracket + 1, trimmed.length() - 1).trim();
            Expression arrayExpr = parseStringExpression(arrayName);
            Expression indexExpr = parseStringExpression(indexStr);

            return env -> {
                Object arrObj = arrayExpr.evaluate(env);
                Object idxObj = indexExpr.evaluate(env);
                int idx = idxObj instanceof Number ? ((Number) idxObj).intValue() : 0;

                if (arrObj instanceof List<?>) {
                    List<?> list = (List<?>) arrObj;
                    if (idx >= 0 && idx < list.size()) return list.get(idx);
                } else if (arrObj instanceof Object[]) {
                    Object[] arr = (Object[]) arrObj;
                    if (idx >= 0 && idx < arr.length) return arr[idx];
                }
                return null;
            };
        }

        // String equals method: getCrop().equals("WHEAT")
        if (trimmed.contains(".equals(") && trimmed.endsWith(")")) {
            int eqIdx = trimmed.indexOf(".equals(");
            Expression left = parseStringExpression(trimmed.substring(0, eqIdx));
            String inner = trimmed.substring(eqIdx + 8, trimmed.length() - 1);
            Expression right = parseStringExpression(inner);
            return env -> Objects.equals(left.evaluate(env), right.evaluate(env));
        }

        // Logical OR: ||
        int orIdx = findTopLevelOp(trimmed, "||");
        if (orIdx != -1) {
            Expression left = parseStringExpression(trimmed.substring(0, orIdx));
            Expression right = parseStringExpression(trimmed.substring(orIdx + 2));
            return env -> isTruthy(left.evaluate(env)) || isTruthy(right.evaluate(env));
        }

        // Logical AND: &&
        int andIdx = findTopLevelOp(trimmed, "&&");
        if (andIdx != -1) {
            Expression left = parseStringExpression(trimmed.substring(0, andIdx));
            Expression right = parseStringExpression(trimmed.substring(andIdx + 2));
            return env -> isTruthy(left.evaluate(env)) && isTruthy(right.evaluate(env));
        }

        // Logical NOT: !
        if (trimmed.startsWith("!")) {
            Expression inner = parseStringExpression(trimmed.substring(1).trim());
            return env -> !isTruthy(inner.evaluate(env));
        }

        // Comparison operators: ==, !=, <=, >=, <, >
        String[] cmpOps = {"==", "!=", "<=", ">=", "<", ">"};
        for (String op : cmpOps) {
            int opIdx = findTopLevelOp(trimmed, op);
            if (opIdx != -1) {
                Expression left = parseStringExpression(trimmed.substring(0, opIdx));
                Expression right = parseStringExpression(trimmed.substring(opIdx + op.length()));
                return env -> evaluateComparison(left.evaluate(env), op, right.evaluate(env));
            }
        }

        // Addition and subtraction (+, -)
        int addSubIdx = findTopLevelAddSub(trimmed);
        if (addSubIdx != -1) {
            char op = trimmed.charAt(addSubIdx);
            Expression left = parseStringExpression(trimmed.substring(0, addSubIdx));
            Expression right = parseStringExpression(trimmed.substring(addSubIdx + 1));
            return env -> {
                Object l = left.evaluate(env);
                Object r = right.evaluate(env);
                if (op == '+') {
                    if (l instanceof Number && r instanceof Number) {
                        return ((Number) l).doubleValue() + ((Number) r).doubleValue();
                    }
                    return String.valueOf(l) + String.valueOf(r);
                } else {
                    if (l instanceof Number && r instanceof Number) {
                        return ((Number) l).doubleValue() - ((Number) r).doubleValue();
                    }
                    return 0;
                }
            };
        }

        // Multiplication, division, and modulo (*, /, %)
        int mulDivIdx = findTopLevelMulDiv(trimmed);
        if (mulDivIdx != -1) {
            char op = trimmed.charAt(mulDivIdx);
            Expression left = parseStringExpression(trimmed.substring(0, mulDivIdx));
            Expression right = parseStringExpression(trimmed.substring(mulDivIdx + 1));
            return env -> {
                Object l = left.evaluate(env);
                Object r = right.evaluate(env);
                if (l instanceof Number && r instanceof Number) {
                    double ld = ((Number) l).doubleValue();
                    double rd = ((Number) r).doubleValue();
                    if (op == '*') return ld * rd;
                    if (op == '/') return rd == 0 ? 0 : ld / rd;
                    if (op == '%') return rd == 0 ? 0 : ld % rd;
                }
                return 0;
            };
        }

        // Literal boolean/null constants
        if (trimmed.equals("true")) return env -> true;
        if (trimmed.equals("false")) return env -> false;
        if (trimmed.equals("null")) return env -> null;

        // String literals
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            String str = trimmed.substring(1, trimmed.length() - 1);
            return env -> str;
        }

        // Numeric literals
        try {
            if (trimmed.contains(".")) {
                double d = Double.parseDouble(trimmed);
                return env -> d;
            } else {
                int i = Integer.parseInt(trimmed);
                return env -> i;
            }
        } catch (NumberFormatException ignored) {
        }

        // Function / Method invocation: harvest() or plant("WHEAT")
        if (trimmed.endsWith(")") && trimmed.contains("(")) {
            int openIdx = trimmed.indexOf('(');
            int closeIdx = trimmed.lastIndexOf(')');
            String fnName = trimmed.substring(0, openIdx).trim();
            String argsStr = trimmed.substring(openIdx + 1, closeIdx).trim();
            List<Expression> argExprs = parseArgList(argsStr);

            return env -> {
                Object fnObj = env.get(fnName);
                if (fnObj instanceof Function) {
                    @SuppressWarnings("unchecked")
                    Function<List<Object>, Object> func = (Function<List<Object>, Object>) fnObj;
                    List<Object> evaluatedArgs = new ArrayList<>();
                    for (Expression e : argExprs) {
                        evaluatedArgs.add(e.evaluate(env));
                    }
                    try {
                        return func.apply(evaluatedArgs);
                    } catch (RuntimeException re) {
                        if (re.getCause() instanceof MiniJavaInterpreter.StopException) {
                            throw (RuntimeException) re;
                        }
                        throw re;
                    }
                }
                return null;
            };
        }

        return env -> env.get(trimmed);
    }

    private static List<Expression> parseArgList(String argsStr) {
        List<Expression> list = new ArrayList<>();
        if (argsStr.isEmpty()) {
            return list;
        }

        int depth = 0;
        int last = 0;
        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                list.add(parseStringExpression(argsStr.substring(last, i).trim()));
                last = i + 1;
            }
        }
        if (last < argsStr.length()) {
            list.add(parseStringExpression(argsStr.substring(last).trim()));
        }
        return list;
    }

    private static int findTopLevelOp(String s, String op) {
        int depth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i <= s.length() - op.length(); i++) {
            char c = s.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || s.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuotes = false;
                }
            }
            if (!inQuotes) {
                if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--;
                } else if (depth == 0 && s.startsWith(op, i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findTopLevelAddSub(String s) {
        int depth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || s.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuotes = false;
                }
            }
            if (!inQuotes) {
                if (c == ')') {
                    depth++;
                } else if (c == '(') {
                    depth--;
                } else if (depth == 0 && (c == '+' || c == '-') && i > 0
                        && s.charAt(i - 1) != '=' && s.charAt(i - 1) != '<' && s.charAt(i - 1) != '>'
                        && s.charAt(i - 1) != '!' && s.charAt(i - 1) != '+' && s.charAt(i - 1) != '-') {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findTopLevelMulDiv(String s) {
        int depth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || s.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuotes = false;
                }
            }
            if (!inQuotes) {
                if (c == ')') {
                    depth++;
                } else if (c == '(') {
                    depth--;
                } else if (depth == 0 && (c == '*' || c == '/' || c == '%')) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean evaluateComparison(Object l, String op, Object r) {
        if (op.equals("==")) return Objects.equals(l, r);
        if (op.equals("!=")) return !Objects.equals(l, r);
        if (l instanceof Number && r instanceof Number) {
            double ld = ((Number) l).doubleValue();
            double rd = ((Number) r).doubleValue();
            switch (op) {
                case "<": return ld < rd;
                case "<=": return ld <= rd;
                case ">": return ld > rd;
                case ">=": return ld >= rd;
            }
        }
        return false;
    }

    public static boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).doubleValue() != 0;
        if (val instanceof String) return !((String) val).isEmpty();
        return true;
    }

    // ==========================================
    // AST Statement Node Implementations
    // ==========================================
    public static class BlockStatement implements Statement {
        public final int line;
        public final List<Statement> statements;

        public BlockStatement(int line, List<Statement> statements) {
            this.line = line;
            this.statements = statements;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            for (Statement s : statements) {
                if (interp.isStopped()) {
                    throw new MiniJavaInterpreter.StopException();
                }
                s.execute(env, interp);
            }
        }
    }

    public static class WhileStatement implements Statement {
        public final int line;
        public final Expression condition;
        public final Statement body;

        public WhileStatement(int line, Expression condition, Statement body) {
            this.line = line;
            this.condition = condition;
            this.body = body;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            while (true) {
                interp.checkState(line, "while (...)");
                Object condVal = condition.evaluate(env);
                if (!isTruthy(condVal)) {
                    break;
                }
                try {
                    if (body != null) {
                        body.execute(env, interp);
                    }
                } catch (MiniJavaInterpreter.BreakException e) {
                    break;
                } catch (MiniJavaInterpreter.ContinueException e) {
                    // continue to next iteration
                }
            }
        }
    }

    public static class DoWhileStatement implements Statement {
        public final int line;
        public final Statement body;
        public final Expression condition;

        public DoWhileStatement(int line, Statement body, Expression condition) {
            this.line = line;
            this.body = body;
            this.condition = condition;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            do {
                interp.checkState(line, "do { ... } while (...)");
                try {
                    if (body != null) {
                        body.execute(env, interp);
                    }
                } catch (MiniJavaInterpreter.BreakException e) {
                    break;
                } catch (MiniJavaInterpreter.ContinueException e) {
                    // jump to condition evaluation
                }

                if (condition != null) {
                    Object condVal = condition.evaluate(env);
                    if (!isTruthy(condVal)) {
                        break;
                    }
                }
            } while (true);
        }
    }

    public static class ForStatement implements Statement {
        public final int line;
        public final Statement init;
        public final Expression condition;
        public final Statement update;
        public final Statement body;

        public ForStatement(int line, Statement init, Expression condition, Statement update, Statement body) {
            this.line = line;
            this.init = init;
            this.condition = condition;
            this.update = update;
            this.body = body;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            MiniJavaInterpreter.Environment loopEnv = new MiniJavaInterpreter.Environment(env);
            if (init != null) {
                init.execute(loopEnv, interp);
            }

            while (true) {
                interp.checkState(line, "for (...)");
                if (condition != null && !isTruthy(condition.evaluate(loopEnv))) {
                    break;
                }
                try {
                    if (body != null) {
                        body.execute(loopEnv, interp);
                    }
                } catch (MiniJavaInterpreter.BreakException e) {
                    break;
                } catch (MiniJavaInterpreter.ContinueException e) {
                    // jump to update step
                }
                if (update != null) {
                    update.execute(loopEnv, interp);
                }
            }
        }
    }

    public static class ForEachStatement implements Statement {
        public final int line;
        public final String varName;
        public final Expression iterableExpr;
        public final Statement body;

        public ForEachStatement(int line, String varName, Expression iterableExpr, Statement body) {
            this.line = line;
            this.varName = varName;
            this.iterableExpr = iterableExpr;
            this.body = body;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            Object iterVal = iterableExpr != null ? iterableExpr.evaluate(env) : null;
            List<Object> items = new ArrayList<>();

            if (iterVal instanceof Collection<?>) {
                items.addAll((Collection<?>) iterVal);
            } else if (iterVal instanceof Object[]) {
                for (Object o : (Object[]) iterVal) items.add(o);
            } else if (iterVal instanceof int[]) {
                for (int val : (int[]) iterVal) items.add(val);
            } else if (iterVal instanceof double[]) {
                for (double val : (double[]) iterVal) items.add(val);
            } else if (iterVal instanceof String) {
                for (char c : ((String) iterVal).toCharArray()) items.add(String.valueOf(c));
            } else if (iterVal != null) {
                items.add(iterVal);
            }

            MiniJavaInterpreter.Environment loopEnv = new MiniJavaInterpreter.Environment(env);
            for (Object item : items) {
                interp.checkState(line, "for (" + varName + " : ...)");
                loopEnv.set(varName, item);
                try {
                    if (body != null) {
                        body.execute(loopEnv, interp);
                    }
                } catch (MiniJavaInterpreter.BreakException e) {
                    break;
                } catch (MiniJavaInterpreter.ContinueException e) {
                    // continue to next item
                }
            }
        }
    }

    public static class SwitchStatement implements Statement {
        public static class SwitchCase {
            public final Expression caseExpr; // null for default
            public final List<Statement> statements;

            public SwitchCase(Expression caseExpr, List<Statement> statements) {
                this.caseExpr = caseExpr;
                this.statements = statements;
            }

            public boolean isDefault() {
                return caseExpr == null;
            }
        }

        public final int line;
        public final Expression targetExpr;
        public final List<SwitchCase> cases;

        public SwitchStatement(int line, Expression targetExpr, List<SwitchCase> cases) {
            this.line = line;
            this.targetExpr = targetExpr;
            this.cases = cases;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, "switch (...)");
            Object targetVal = targetExpr != null ? targetExpr.evaluate(env) : null;

            int startIndex = -1;
            int defaultIndex = -1;

            for (int i = 0; i < cases.size(); i++) {
                SwitchCase sc = cases.get(i);
                if (sc.isDefault()) {
                    defaultIndex = i;
                } else {
                    Object caseVal = sc.caseExpr.evaluate(env);
                    if (Objects.equals(targetVal, caseVal) || (targetVal != null && caseVal != null && targetVal.toString().equals(caseVal.toString()))) {
                        startIndex = i;
                        break;
                    }
                }
            }

            if (startIndex == -1) {
                startIndex = defaultIndex;
            }

            if (startIndex != -1) {
                try {
                    for (int i = startIndex; i < cases.size(); i++) {
                        for (Statement stmt : cases.get(i).statements) {
                            if (interp.isStopped()) {
                                throw new MiniJavaInterpreter.StopException();
                            }
                            stmt.execute(env, interp);
                        }
                    }
                } catch (MiniJavaInterpreter.BreakException e) {
                    // break handled cleanly
                }
            }
        }
    }

    public static class IfStatement implements Statement {
        public final int line;
        public final Expression condition;
        public final Statement thenStmt;
        public final Statement elseStmt;

        public IfStatement(int line, Expression condition, Statement thenStmt, Statement elseStmt) {
            this.line = line;
            this.condition = condition;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, "if (...)");
            if (isTruthy(condition.evaluate(env))) {
                if (thenStmt != null) {
                    thenStmt.execute(env, interp);
                }
            } else if (elseStmt != null) {
                elseStmt.execute(env, interp);
            }
        }
    }

    public static class VarDeclStatement implements Statement {
        public final int line;
        public final String name;
        public final Expression initExpr;

        public VarDeclStatement(int line, String name, Expression initExpr) {
            this.line = line;
            this.name = name;
            this.initExpr = initExpr;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, name + " = ...");
            Object val = initExpr != null ? initExpr.evaluate(env) : null;
            env.set(name, val);
        }
    }

    public static class AssignStatement implements Statement {
        public final int line;
        public final String name;
        public final Expression expr;

        public AssignStatement(int line, String name, Expression expr) {
            this.line = line;
            this.name = name;
            this.expr = expr;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, name + " = ...");
            Object val = expr != null ? expr.evaluate(env) : null;
            env.update(name, val);
        }
    }

    public static class AugAssignStatement implements Statement {
        public final int line;
        public final String name;
        public final String op;
        public final Expression expr;

        public AugAssignStatement(int line, String name, String op, Expression expr) {
            this.line = line;
            this.name = name;
            this.op = op;
            this.expr = expr;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, name + " " + op + "= ...");
            Object current = env.get(name);
            Object delta = expr != null ? expr.evaluate(env) : null;

            if (op.equals("+")) {
                if (current instanceof Number && delta instanceof Number) {
                    env.update(name, ((Number) current).doubleValue() + ((Number) delta).doubleValue());
                } else {
                    env.update(name, String.valueOf(current) + String.valueOf(delta));
                }
            } else if (op.equals("-") && current instanceof Number && delta instanceof Number) {
                env.update(name, ((Number) current).doubleValue() - ((Number) delta).doubleValue());
            }
        }
    }

    public static class IncDecStatement implements Statement {
        public final int line;
        public final String name;
        public final int delta;

        public IncDecStatement(int line, String name, int delta) {
            this.line = line;
            this.name = name;
            this.delta = delta;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, name + (delta > 0 ? "++" : "--"));
            Object current = env.get(name);
            if (current instanceof Number) {
                env.update(name, ((Number) current).intValue() + delta);
            } else {
                env.update(name, delta);
            }
        }
    }

    public static class DefStatement implements Statement {
        public final int line;
        public final String name;
        public final List<String> params;
        public final Statement body;

        public DefStatement(int line, String name, List<String> params, Statement body) {
            this.line = line;
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) {
            env.set(name, (Function<List<Object>, Object>) args -> {
                interp.checkCallDepth();
                MiniJavaInterpreter.Environment localEnv = new MiniJavaInterpreter.Environment(env);
                for (int i = 0; i < params.size(); i++) {
                    Object val = (args != null && i < args.size()) ? args.get(i) : null;
                    localEnv.set(params.get(i), val);
                }
                try {
                    if (body != null) {
                        body.execute(localEnv, interp);
                    }
                } catch (MiniJavaInterpreter.ReturnException ret) {
                    return ret.value;
                } catch (MiniJavaInterpreter.StopException e) {
                    throw new RuntimeException(e);
                } finally {
                    interp.leaveCall();
                }
                return null;
            });
        }
    }

    public static class ReturnStatement implements Statement {
        public final int line;
        public final Expression expr;

        public ReturnStatement(int line, Expression expr) {
            this.line = line;
            this.expr = expr;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, "return");
            Object val = expr != null ? expr.evaluate(env) : null;
            throw new MiniJavaInterpreter.ReturnException(val);
        }
    }

    public static class BreakStatement implements Statement {
        public final int line;

        public BreakStatement(int line) {
            this.line = line;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, "break");
            throw new MiniJavaInterpreter.BreakException();
        }
    }

    public static class ContinueStatement implements Statement {
        public final int line;

        public ContinueStatement(int line) {
            this.line = line;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, "continue");
            throw new MiniJavaInterpreter.ContinueException();
        }
    }

    public static class ExprStatement implements Statement {
        public final int line;
        public final Expression expr;

        public ExprStatement(int line, Expression expr) {
            this.line = line;
            this.expr = expr;
        }

        @Override
        public void execute(MiniJavaInterpreter.Environment env, MiniJavaInterpreter interp) throws MiniJavaInterpreter.StopException {
            interp.checkState(line, "exec");
            if (expr != null) {
                expr.evaluate(env);
            }
        }
    }
}
