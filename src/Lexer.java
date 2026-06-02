/* SPDX-License-Identifier: GPL-3.0-or-later */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class Lexer {
    private LexerJ lexerJ;
    private String sourceCode;
    private List<Token> tokens = new ArrayList<Token>();
    private Stack<Token> codeBlock = new Stack<Token>();
    private int line = 0;
    private int column = 0;
    boolean firstInLine = true;

    public Lexer(LexerJ lexerJ) {
        this.lexerJ = lexerJ;
        this.sourceCode = lexerJ.getSourceCode();
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<Token> run() throws Exception {
        int index;
        for (int i = 0; i < sourceCode.length(); i++, column++) {
            char current = sourceCode.charAt(i);
            if (firstInLine && current == '\n') {
                line++;
                column = 0;
            }
            if (!firstInLine && current == '\n') {
                firstInLine = true;
                column = 0;
                tokens.add(new Token(TokenType.EOL, "EOL", null, line++, column));
            }
            if (!Character.isWhitespace(current)) {
                switch (current) {
                    case '(' ->
                            tokens.add(new Token(TokenType.LEFT_PARENTHESIS, Character.toString(current), null, line, column));
                    case ')' ->
                            tokens.add(new Token(TokenType.RIGHT_PARENTHESIS, Character.toString(current), null, line, column));
                    case '[' ->
                            tokens.add(new Token(TokenType.LEFT_BRACE, Character.toString(current), null, line, column));
                    case ']' ->
                            tokens.add(new Token(TokenType.RIGHT_BRACE, Character.toString(current), null, line, column));
                    case ',' -> tokens.add(new Token(TokenType.COMMA, Character.toString(current), null, line, column));
                    case ':' -> tokens.add(new Token(TokenType.COLON, Character.toString(current), null, line, column));
                    case '#' ->
                            tokens.add(new Token(TokenType.OCTOTHORPE, Character.toString(current), null, line, column));
                    case '&' ->
                            tokens.add(new Token(TokenType.AMPERSAND, Character.toString(current), null, line, column));
                    case '$' ->
                            tokens.add(new Token(TokenType.DOLLAR, Character.toString(current), null, line, column));
                    case '+' -> {
                        if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '+') {
                            tokens.add(new Token(TokenType.INCREMENT, "++", null, line, column));
                            i++; column++;
                        } else {
                            tokens.add(new Token(TokenType.ADDITION, Character.toString(current), null, line, column));
                        }
                    }
                    case '-' -> {
                        if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '-') {
                            tokens.add(new Token(TokenType.DECREMENT, "--", null, line, column));
                            i++; column++;
                        } else {
                            tokens.add(new Token(TokenType.SUBTRACTION, Character.toString(current), null, line, column));
                        }
                    }
                    case '*' ->
                            tokens.add(new Token(TokenType.MULTIPLICATION, Character.toString(current), null, line, column));
                    case '/' ->
                            tokens.add(new Token(TokenType.DIVISION, Character.toString(current), null, line, column));
                    case '%' -> {
                        // E check ang comment %%
                        if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '%') {
                            index = comment(i);
                            column += index - i;
                            i = index;
                            continue;
                        }
                        tokens.add(new Token(TokenType.MODULO, Character.toString(current), null, line, column));
                    }
                    case '=' -> {
                        index = assign_equal(i);
                        column += index - i;
                        i = index;
                    }
                    case '<' -> {
                        index = lesser_equal_nequal(i);
                        column += index - i;
                        i = index;
                    }
                    case '>' -> {
                        index = greater_equal(i);
                        column += index - i;
                        i = index;
                    }
                    default -> {
                        if (Quotation.equalsSingleQuote(current)) {
                            index = character_literal(i);
                            column += index - i;
                            i = index;
                            break;
                        } else if (Quotation.equalsDoubleQuote(current)) {
                            index = bool_literal(i);
                            if (index == i) {
                                int[] result = string_literal(i);
                                if (result[0] == 1)
                                    i = result[1];
                                index = result[2];
                            }
                            column += index - i;
                            i = index;
                            break;
                        } else if (current == '.' || Character.isDigit(current)) {
                            index = number_literal(i);
                            column += index - i;
                            i = index;
                            break;
                        } else if (current == '_' || Character.isAlphabetic(current)) {
                            index = words(i);
                            column += index - i;
                            i = index;
                            break;
                        }
                        throw lexerJ.newError(line, column, Character.toString(current), "Invalid character.");
                    }
                }
                firstInLine = false;
            }
        }
        tokens.add(new Token(TokenType.EOF, "EOF", null, line, column));
        if (!codeBlock.isEmpty())
            throw lexerJ.newError(line, column, "START", "'START' is missing 'END'");
        return tokens;
    }

    private int comment(int i) {
        char current = sourceCode.charAt(i);
        while (current != '\n')
            current = sourceCode.charAt(++i);
        line++;
        column = 0;
        firstInLine = true;
        return i;
    }

    private int assign_equal(int i) {
        ++i;
        char current = sourceCode.charAt(i);
        if (current == '=') {
            tokens.add(new Token(TokenType.EQUAL, "==", null, line, column));
            return i;
        }
        --i;
        current = sourceCode.charAt(i);
        tokens.add(new Token(TokenType.ASSIGNMENT, Character.toString(current), null, line, column));
        return i;
    }

    private int lesser_equal_nequal(int i) {
        ++i;
        char current = sourceCode.charAt(i);
        if (current == '=') {
            tokens.add(new Token(TokenType.LESSER_EQUAL, "<=", null, line, column));
            return i;
        }
        if (current == '>') {
            tokens.add(new Token(TokenType.NOT_EQUAL, "<>", null, line, column));
            return i;
        }
        --i;
        current = sourceCode.charAt(i);
        tokens.add(new Token(TokenType.LESSER, Character.toString(current), null, line, column));
        return i;
    }

    private int greater_equal(int i) {
        ++i;
        char current = sourceCode.charAt(i);
        if (current == '=') {
            tokens.add(new Token(TokenType.GREATER_EQUAL, ">=", null, line, column));
            return i;
        }
        --i;
        current = sourceCode.charAt(i);
        tokens.add(new Token(TokenType.GREATER, Character.toString(current), null, line, column));
        return i;
    }

    private int character_literal(int i) throws Exception {
        ++i;
        char current = sourceCode.charAt(i);
        if (Quotation.equalsSingleQuote(current)) {
            tokens.add(new Token(TokenType.CHAR_LIT, "", '\0', line, column));
            return i;
        }
        int[] SCResult = special_characters(i);
        if (SCResult[0] != -1) {
            i = SCResult[1];
            current = (char) SCResult[2];
            tokens.add(new Token(TokenType.CHAR_LIT, Character.toString(current), current, line, column));
            current = sourceCode.charAt(++i);
            if (!Quotation.equalsSingleQuote(current))
                throw lexerJ.newError(line, column, sourceCode.substring(i - 1, i + 1), "Invalid char literal.");
            return i;
        }
        ++i;
        current = sourceCode.charAt(i);
        if (Quotation.equalsSingleQuote(current)) {
            --i;
            current = sourceCode.charAt(i);
            tokens.add(new Token(TokenType.CHAR_LIT, Character.toString(current), current, line, column));
            return ++i;
        }

        throw lexerJ.newError(line, column, sourceCode.substring(i - 1, i + 1), "Invalid char literal.");
    }

    private int[] evaluateDFA(
            int currentIndex,
            int initialState,
            HashSet<Integer> finalState,
            HashSet<Integer> deadState,
            int[][] charStateTransitionTable,
            CharacterToIndexFunction charToIndex,
            boolean terminateIfAny) {
        int currentState = initialState;
        while (currentIndex < sourceCode.length()
                && !deadState.contains(currentState)
                && (terminateIfAny || !finalState.contains(currentState))) {
            int characterIndex = charToIndex.apply(sourceCode.charAt(currentIndex));
            if (terminateIfAny && characterIndex == -1)
                break;
            currentState = charStateTransitionTable[currentState][characterIndex];
            currentIndex++;
        }
        return new int[]{currentIndex == sourceCode.length() ? 1 : 0, currentState, currentIndex - 1};
    }

    private boolean stringToBool(String lexeme) {
        return lexeme.equals("TRUE");
    }

    private int bool_literal(int i) throws Exception {
        int returnIndex = i;
        int[][] charStateTransitionTable = {
                // F, A, L, S, E, T, R, U, "
                {1, 9, 9, 9, 9, 7, 9, 9, 9}, // 0
                {9, 2, 9, 9, 9, 9, 9, 9, 9}, // 1
                {9, 9, 3, 9, 9, 9, 9, 9, 9}, // 2
                {9, 9, 9, 4, 9, 9, 9, 9, 9}, // 3
                {9, 9, 9, 9, 5, 9, 9, 9, 9}, // 4
                {9, 9, 9, 9, 9, 9, 9, 9, 6}, // 5
                {6, 6, 6, 6, 6, 6, 6, 6, 6}, // 6
                {9, 9, 9, 9, 9, 9, 8, 9, 9}, // 7
                {9, 9, 9, 9, 9, 9, 9, 4, 9}, // 8
                {9, 9, 9, 9, 9, 9, 9, 9, 9}, // 9
        };
        HashSet<Integer> finalState = new HashSet<>();
        finalState.add(6);

        HashSet<Integer> deadState = new HashSet<>();
        deadState.add(9);

        CharacterToIndexFunction charToIndex = (character) -> {
            int translated = -1;
            switch (character) {
                case 'F' -> translated = 0;
                case 'A' -> translated = 1;
                case 'L' -> translated = 2;
                case 'S' -> translated = 3;
                case 'E' -> translated = 4;
                case 'T' -> translated = 5;
                case 'R' -> translated = 6;
                case 'U' -> translated = 7;
                default -> {
                    if (Quotation.equalsDoubleQuote(character)) {
                        translated = 8;
                    }
                }
            }
            return translated;
        };
        int[] result = evaluateDFA(++i, 0, finalState, deadState, charStateTransitionTable, charToIndex, true);
        if (result[0] == 1)
            throw lexerJ.newError(line, column, sourceCode.substring(i, result[2]), "Unclosed bool literal.");
        if (finalState.contains(result[1])) {
            String boolLexeme = sourceCode.substring(i, result[2]);
            tokens.add(new Token(TokenType.BOOL_LIT, boolLexeme, stringToBool(boolLexeme), line, column));
            returnIndex = result[2];
        }
        return returnIndex;
    }

    private int escape(int i) throws Exception {
        int returnIndex = i;
        int[][] charStateTransitionTable = {
                // [, *, ]
                {1, 4, 4}, // 0
                {2, 2, 2}, // 1
                {5, 5, 3}, // 2
                {3, 3, 3}, // 3
                {4, 4, 4}, // 4
                {5, 5, 5}, // 5
        };
        HashSet<Integer> finalState = new HashSet<>();
        finalState.add(3);

        HashSet<Integer> deadState = new HashSet<>();
        deadState.add(4);
        deadState.add(5);

        CharacterToIndexFunction charToIndex = (character) -> {
            int translated = 1;
            switch (character) {
                case '[' -> translated = 0;
                case ']' -> translated = 2;
            }
            return translated;
        };
        int[] result = evaluateDFA(i, 0, finalState, deadState, charStateTransitionTable, charToIndex, false);
        String res = sourceCode.substring(i, result[2] + 1);
        if (result[0] == 1)
            throw lexerJ.newError(line, column, res, "Unclosed string literal.");
        if (deadState.contains(result[1])) {
            throw lexerJ.newError(line, column, res, "Invalid escape.");
        }
        returnIndex = result[2];
        return returnIndex;
    }

    private String unescapeJavaString(String st) {
        StringBuilder sb = new StringBuilder(st.length());
        int i = 0;
        int len = st.length();

        while (i < len) {
            char ch = st.charAt(i);

            if (ch != '\\') {
                sb.append(ch);
                continue;
            }

            if (i + 1 >= len) {
                sb.append('\\');
                break;
            }

            char next = st.charAt(i + 1);

            if (isOctalDigit(next)) {
                int[] result = parseOctal(st, i + 1);
                sb.append((char) result[0]);
                i = result[1];
                continue;
            }

            switch (next) {
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case '\"' -> sb.append('\"');
                case '\'' -> sb.append('\'');
                case '\\' -> sb.append('\\');

                case 'u' -> {
                    int[] result = parseUnicode(st, i);
                    sb.append(Character.toChars(result[0]));
                    i = result[1];
                    continue;
                }

                default -> sb.append(next);
            }

            i += 2;
        }

        return sb.toString();
    }

    private boolean isOctalDigit(char c) {
        return c >= '0' && c <= '7';
    }

    private int[] parseOctal(String st, int start) {
        int len = st.length();
        int end = start;
        StringBuilder code = new StringBuilder();

        for (int j = 0; j < 3 && end < len && isOctalDigit(st.charAt(end)); j++) {
            code.append(st.charAt(end++));
        }

        int value = Integer.parseInt(code.toString(), 8);
        return new int[]{value, end};
    }

    private int[] parseUnicode(String st, int i) {
        if (i + 5 >= st.length()) {
            return new int[]{'u', i + 2};
        }

        String hex = st.substring(i + 2, i + 6);
        int code = Integer.parseInt(hex, 16);

        return new int[]{code, i + 6};
    }

    private int[] special_characters(int i) throws Exception {
        int[] result = new int[3];
        char current = sourceCode.charAt(i);
        if (current == '[' || current == ']') {
            i = escape(i);
            result[2] = sourceCode.charAt(i - 1);
        } else if (current == '#') {
            result[2] = '\n';
        } else if (current == '\\') {
            if (sourceCode.charAt(++i) == 'n')
                throw lexerJ.newError(line, column, sourceCode.substring(i - 1, i + 1), "Invalid new line character.");
            result[2] = unescapeJavaString(String.format("\\%c", sourceCode.charAt(i))).charAt(0);
        } else {
            result[0] = -1;
        }
        result[1] = i;
        return result;
    }

    private int[] string_literal(int i) throws Exception {
        StringBuilder literal = new StringBuilder();
        int startIndex = i;
        int startColumn = column;
        int startLine = line;
        int[] SCResult;
        int[] result = new int[]{0, 0, 0};
        for (++i; i < sourceCode.length(); i++) {
            char current = sourceCode.charAt(i);
            if (current == '\n') {
                line++;
                column = 0;
                result[0] = 1;
                result[1] = i;
            }
            if (Quotation.equalsDoubleQuote(current))
                break;
            SCResult = special_characters(i);
            if (SCResult[0] != -1) {
                i = SCResult[1];
                literal.append(Character.toString(SCResult[2]));
            } else
                literal.append(current);
        }
        if (i >= sourceCode.length()) {
            throw lexerJ.newError(result[0] == 1 ? startLine : line, result[0] == 1 ? startColumn : column,
                    Character.toString(sourceCode.charAt(startIndex)), "Unclosed string literal.");
        }
        tokens.add(new Token(TokenType.STR_LIT, literal.toString(), literal.toString(), line, column));
        result[2] = i;
        return result;
    }

    private int number_literal(int i) throws Exception {
        int returnIndex = i;
        int[][] charStateTransitionTable = {
                // D, .
                {1, 3}, // 0
                {1, 2}, // 1
                {4, 5}, // 2
                {4, 5}, // 3
                {4, 5}, // 4
                {5, 5}, // 5
        };
        HashSet<Integer> finalState = new HashSet<>(List.of(1, 2, 4));
        HashSet<Integer> deadState = new HashSet<>(List.of(5));

        CharacterToIndexFunction charToIndex = (character) -> {
            int translated = -1;
            if (Character.isDigit(character))
                translated = 0;
            else if (character == '.')
                translated = 1;
            return translated;
        };
        int[] result = evaluateDFA(i, 0, finalState, deadState, charStateTransitionTable, charToIndex, true);
        String res = sourceCode.substring(i, result[2] + 1);
        if (result[0] == 1)
            throw lexerJ.newError(line, column, res, "Unclosed code block.");
        if (deadState.contains(result[1]))
            throw lexerJ.newError(line, column, res, "Invalid number literal.");
        if (result[1] == 1) {
            tokens.add(new Token(TokenType.INT_LIT, res, Integer.parseInt(res), line, column));
            returnIndex = result[2];
        } else if (finalState.contains(result[1])) {
            tokens.add(new Token(TokenType.FLOAT_LIT, res, Double.parseDouble(res), line, column));
            returnIndex = result[2];
        }
        return returnIndex;
    }

    private int words(int i) throws Exception {
        int returnIndex = i;
        int[][] charStateTransitionTable = {
                // _, $, A, D
                {1, 1, 1, 2}, // 0
                {1, 1, 1, 1}, // 1
                {2, 2, 2, 2}, // 2
        };
        HashSet<Integer> finalState = new HashSet<>(List.of(1));
        HashSet<Integer> deadState = new HashSet<Integer>(List.of(2));

        CharacterToIndexFunction charToIndex = (character) -> {
            int translated = -1;
            if (character == '_')
                translated = 0;
            else if (character == '$')
                translated = 1;
            else if (Character.isLetter(character))
                translated = 2;
            else if (Character.isDigit(character))
                translated = 3;
            return translated;
        };
        int[] result = evaluateDFA(i, 0, finalState, deadState, charStateTransitionTable, charToIndex, true);
        String res = sourceCode.substring(i, result[2] + 1);
        if (result[0] == 1)
            throw lexerJ.newError(line, column, res, "Invalid syntax.");
        if (finalState.contains(result[1])) {
            Token temp;
            if (Token.reservedWords.containsKey(res)) {
                temp = new Token(Token.reservedWords.get(res), res, null, line, column);
                switch (temp.type()) {
                    case START -> codeBlock.push(temp);
                    case END -> {
                        if (codeBlock.isEmpty()) {
                            throw lexerJ.newError(line, column, "END", "'END' is missing 'START'");
                        }
                        codeBlock.pop();
                    }
                }
                tokens.add(temp);
            } else {
                temp = new Token(TokenType.IDENTIFIER, res, null, line, column);
                tokens.add(temp);
            }
            returnIndex = result[2];
        }
        return returnIndex;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            result.append(String.format("[%d] - %s\n", i, tokens.get(i)));
        }
        return result.toString();
    }
}