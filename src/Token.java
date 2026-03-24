/* SPDX-License-Identifier: GPL-3.0-or-later */

import java.util.Map;
import java.util.Set;

public record Token(TokenType type, String lexeme, Object literal, int line, int column) {
    public static final Map<String, TokenType> reservedWords = Map.ofEntries(
            Map.entry("AND", TokenType.AND),
            Map.entry("OR", TokenType.OR),
            Map.entry("NOT", TokenType.NOT),
            Map.entry("PRINT", TokenType.PRINT),
            Map.entry("SCAN", TokenType.SCAN),
            Map.entry("DECLARE", TokenType.DECLARE),
            Map.entry("INT", TokenType.INT),
            Map.entry("BOOL", TokenType.BOOL),
            Map.entry("FLOAT", TokenType.FLOAT),
            Map.entry("CHAR", TokenType.CHAR),
            Map.entry("SCRIPT", TokenType.SCRIPT),
            Map.entry("AREA", TokenType.AREA),
            Map.entry("START", TokenType.START),
            Map.entry("END", TokenType.END),
            Map.entry("IF", TokenType.IF),
            Map.entry("ELSE", TokenType.ELSE),
            Map.entry("FOR", TokenType.FOR),
            Map.entry("REPEAT", TokenType.REPEAT),
            Map.entry("WHEN", TokenType.WHEN)
    );

    public static final Map<TokenType, String> tokenTypeToLexeme = Map.ofEntries(
            Map.entry(TokenType.LEFT_PARENTHESIS, "("),
            Map.entry(TokenType.RIGHT_PARENTHESIS, ")"),
            Map.entry(TokenType.LEFT_BRACE, "["),
            Map.entry(TokenType.RIGHT_BRACE, "]"),
            Map.entry(TokenType.COMMA, ","),
            Map.entry(TokenType.ASSIGNMENT, "="),
            Map.entry(TokenType.COLON, ":"),
            Map.entry(TokenType.OCTOTHORPE, "#"),
            Map.entry(TokenType.AMPERSAND, "&"),
            Map.entry(TokenType.DOLLAR, "$"),
            Map.entry(TokenType.ADDITION, "+"),
            Map.entry(TokenType.SUBTRACTION, "-"),
            Map.entry(TokenType.MULTIPLICATION, "*"),
            Map.entry(TokenType.DIVISION, "/"),
            Map.entry(TokenType.MODULO, "%"),
            Map.entry(TokenType.GREATER, ">"),
            Map.entry(TokenType.LESSER, "<"),
            Map.entry(TokenType.GREATER_EQUAL, ">="),
            Map.entry(TokenType.LESSER_EQUAL, "<="),
            Map.entry(TokenType.EQUAL, "=="),
            Map.entry(TokenType.NOT_EQUAL, "<>"),
            Map.entry(TokenType.AND, "AND"),
            Map.entry(TokenType.OR, "OR"),
            Map.entry(TokenType.NOT, "NOT"),
            Map.entry(TokenType.PRINT, "PRINT"),
            Map.entry(TokenType.SCAN, "SCAN"),
            Map.entry(TokenType.DECLARE, "DECLARE"),
            Map.entry(TokenType.INT, "INT"),
            Map.entry(TokenType.BOOL, "BOOL"),
            Map.entry(TokenType.FLOAT, "FLOAT"),
            Map.entry(TokenType.CHAR, "CHAR"),
            Map.entry(TokenType.SCRIPT, "SCRIPT"),
            Map.entry(TokenType.AREA, "AREA"),
            Map.entry(TokenType.START, "START"),
            Map.entry(TokenType.END, "END"),
            Map.entry(TokenType.IF, "IF"),
            Map.entry(TokenType.ELSE, "ELSE"),
            Map.entry(TokenType.FOR, "FOR"),
            Map.entry(TokenType.REPEAT, "REPEAT"),
            Map.entry(TokenType.WHEN, "WHEN")
    );

    public static final Set<TokenType> logicalComparisonOperators = Set.of(
            TokenType.GREATER, TokenType.LESSER,
            TokenType.GREATER_EQUAL, TokenType.LESSER_EQUAL,
            TokenType.EQUAL, TokenType.NOT_EQUAL,
            TokenType.AND, TokenType.OR, TokenType.NOT
    );

    public static boolean checkType(Object value, TokenType... variableType) {
        for (TokenType tokenType : variableType) {
            boolean match = switch (tokenType) {
                case BOOL ->
                        value instanceof TokenType instance ? instance == TokenType.BOOL || instance == TokenType.BOOL_LIT : value instanceof Boolean;
                case CHAR ->
                        value instanceof TokenType instance ? instance == TokenType.CHAR || instance == TokenType.CHAR_LIT : value instanceof Character;
                case FLOAT ->
                        value instanceof TokenType instance ? instance == TokenType.FLOAT || instance == TokenType.FLOAT_LIT : value instanceof Double;
                case INT ->
                        value instanceof TokenType instance ? instance == TokenType.INT || instance == TokenType.INT_LIT : value instanceof Integer;
                default -> false;
            };
            if (match) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Token(%s,\"%s\",%s,%d)".formatted(type, lexeme, literal, line);
    }
}