/* SPDX-License-Identifier: GPL-3.0-or-later */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LexerJ {
    private Lexer lexer;
    private Parser parser;
    private Interpreter interpret;
    private final StringBuilder sourceCode = new StringBuilder();

    public LexerJ(String filePath) {
        File file = new File(filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String temp;

            while ((temp = br.readLine()) != null) {
                sourceCode.append(temp).append("\n");
            }

        } catch (Exception e) {
            System.out.print("[Error] File not found.");
        }
    }

    public String getSourceCode() {
        return sourceCode.toString();
    }

    private String getCodeAtLine(int lineNumber) {
        int start = 0;
        int end = 0;
        int line = 0;
        for (int i = 0; i < sourceCode.length(); i++) {
            if (sourceCode.charAt(i) == '\n') {
                if (line > 0) {
                    start = end + 1;
                }
                end = i;
                if (line == lineNumber) {
                    break;
                }
                line++;
            }
        }

        return sourceCode.substring(start, end);
    }

    public Exception newError(Token token, String message) {
        String lineCode = getCodeAtLine(token.line);
        String errorPoint = " ".repeat(token.column - 1) + "^";

        return new Exception(
                String.format("%s\n[line: %d, column: %d] on %s '%s'.\n%s\n%s", message, token.line + 1, token.column,
                        token.type,
                        token.lexeme,
                        lineCode, errorPoint));
    }

    public Exception newError(int line, int column, String atFault, String message) {
        String lineCode = getCodeAtLine(line);
        String errorPoint = " ".repeat(column - 1) + "^";

        return new Exception(
                String.format("%s\n[line: %d, column: %d] on %s.\n%s\n%s", message, line + 1, column, atFault, lineCode,
                        errorPoint));
    }

    public void execute() {
        String errorType = "";
        try {
            lexer = new Lexer(this);
            List<Token> tokens;
            try {
                tokens = lexer.run();
            } catch (Exception e) {
                errorType = "Lexer-Error";
                throw e;
            }
            parser = new Parser(this);
            List<ParsingStatement> statements;
            try {
                statements = parser.parse(tokens);
            } catch (Exception e) {
                errorType = "Parser-Error";
                throw e;
            }
            interpret = new Interpreter(this);
            try {
                interpret.interpret(statements);
            } catch (Exception e) {
                errorType = "Interpreter-Error";
                throw e;
            }
        } catch (Exception e) {
            System.out.printf("[%s] %s", errorType, e.getMessage());
        }
    }
}