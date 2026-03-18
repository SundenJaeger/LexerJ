/* SPDX-License-Identifier: GPL-3.0-or-later */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    boolean varDeclarations = true;
    boolean isDeclaring = false;
    boolean inControlStructure = false;
    boolean inScope = false;
    int scopeCounter = 0;
    LexerJ lexerJ;
    private List<Token> tokens;
    private int current = 0;
    List<ParsingStatement> statements = new ArrayList<>();
    private final Map<String, TokenType> variablesType = new HashMap<String, TokenType>();

    public Parser(LexerJ lexerJ) {
        this.lexerJ = lexerJ;
    }

    private ParsingExpression.Literal getDefaultLiteral(TokenType type) {
        Object value = switch (type) {
            case BOOL -> false;
            case CHAR -> '\0';
            case FLOAT -> (double) 0;
            case INT -> 0;
            default -> null;
        };

        return new ParsingExpression.Literal(value);
    }

    List<ParsingStatement> parse(List<Token> tokens) throws Exception {
        this.tokens = tokens;
        // Expect: SCRIPT AREA EOL
        expectThenNext(TokenType.SCRIPT, "Expected 'SCRIPT' at start of program.");
        expectThenNext(TokenType.AREA, "Expected 'AREA' after 'SCRIPT'.");
        expectThenNext(TokenType.EOL, "Expected new line after 'SCRIPT AREA'.");
        // Expect: START SCRIPT EOL
        expectThenNext(TokenType.START, "Expected 'START' before program block.");
        expectThenNext(TokenType.SCRIPT, "Expected 'SCRIPT' after 'START'.");
        expectThenNext(TokenType.EOL, "Expected new line after 'START SCRIPT'.");
        inScope = true;
        scopeCounter++;
        varDeclarations = true;
        while (!compareCurrent(TokenType.END) && !isAtEnd())
            statements.add(parseDeclaration());
        // Expect: END SCRIPT
        expectThenNext(TokenType.END, "Expected 'END' to close program block.");
        expectThenNext(TokenType.SCRIPT, "Expected 'SCRIPT' after 'END'.");
        if (!isAtEnd())
            expectThenNext(TokenType.EOL, "Expected new line after 'END SCRIPT'.");

        return statements;
    }

    private ParsingStatement parseDeclaration() throws Exception {
        if (compareMultipleThenNext(TokenType.DECLARE))
            return parseVariableDeclaration();

        return parseStatement();
    }

    private ParsingStatement parseVariableDeclaration() throws Exception {
        if (!isDeclaring)
            isDeclaring = true;
        if (!varDeclarations)
            throw lexerJ.newError(getPrevious(), "Misplaced variable declaration.");

        // Type comes immediately after DECLARE
        if (!compareMultipleThenNext(TokenType.BOOL, TokenType.CHAR, TokenType.FLOAT, TokenType.INT))
            throw lexerJ.newError(getCurrent(), "Expected data type after 'DECLARE'.");
        TokenType type = getPrevious().type;

        // Parse first variable
        Token name;
        if (compareCurrent(TokenType.IDENTIFIER))
            name = expectThenNext(TokenType.IDENTIFIER, "Expected variable name.");
        else if (Token.reservedWords.containsKey(getCurrent().lexeme))
            throw lexerJ.newError(getCurrent(), "Expected valid variable name but got reserved keyword.");
        else
            throw lexerJ.newError(getCurrent(), "Expected valid variable name.");

        ParsingExpression initializer = null;
        if (compareMultipleThenNext(TokenType.ASSIGNMENT)) {
            initializer = parseExpression();
            if (initializer instanceof ParsingExpression.Literal initial) {
                if (type == TokenType.FLOAT && Token.checkType(initial.value, TokenType.INT)) {
                    double x = Double.parseDouble(initial.value.toString());
                    initializer = new ParsingExpression.Literal(x);
                } else if (!Token.checkType(initial.value, type))
                    throw lexerJ.newError(name, String.format("Expected '%s' type.", type));
            }
        } else
            initializer = getDefaultLiteral(type);

        ParsingStatement.Var returnVar = new ParsingStatement.Var(name, initializer);

        if (!variablesType.containsKey(name.lexeme))
            variablesType.put(name.lexeme, type);
        else
            throw lexerJ.newError(name, String.format("Variable name '%s' is already declared.", name.lexeme));

        boolean manyDeclaration = false;
        while (compareMultipleThenNext(TokenType.COMMA)) {
            if (!manyDeclaration) {
                manyDeclaration = true;
                statements.add(new ParsingStatement.Var(name, initializer));
            }
            name = expectThenNext(TokenType.IDENTIFIER, "Expected variable name.");
            if (compareMultipleThenNext(TokenType.ASSIGNMENT)) {
                initializer = parseExpression();
                if (initializer instanceof ParsingExpression.Literal initial) {
                    if (type == TokenType.FLOAT && Token.checkType(initial.value, TokenType.INT)) {
                        double x = Double.parseDouble(initial.value.toString());
                        initializer = new ParsingExpression.Literal(x);
                    } else if (!Token.checkType(initial.value, type))
                        throw lexerJ.newError(name, String.format("Expected '%s' type.", type));
                }
            } else
                initializer = getDefaultLiteral(type);
            if (!variablesType.containsKey(name.lexeme))
                variablesType.put(name.lexeme, type);
            else
                throw lexerJ.newError(name, String.format("Variable name '%s' is already declared.", name.lexeme));
            statements.add(new ParsingStatement.Var(name, initializer));
        }

        expectThenNext(TokenType.EOL, "Expected new line after declaration.");
        if (isDeclaring)
            isDeclaring = false;

        if (manyDeclaration)
            returnVar = (ParsingStatement.Var) statements.removeLast();

        return returnVar;
    }

    private ParsingStatement parseStatement() throws Exception {
        if (!inScope)
            throw lexerJ.newError(getCurrent(), "Statement is out of scope.");
        if (compareMultipleThenNext(TokenType.IF))
            return parseIf();
        if (compareMultipleThenNext(TokenType.PRINT))
            return parsePrint();
        if (compareMultipleThenNext(TokenType.SCAN))
            return parseScan();
        if (compareMultipleThenNext(TokenType.REPEAT))
            return parseRepeatWhen();
        if (compareMultipleThenNext(TokenType.FOR))
            return parseFor();

        return parseExpressionStatement();
    }

    private ParsingStatement parseExpressionStatement() throws Exception {
        if (!inScope && !isDeclaring)
            throw lexerJ.newError(getCurrent(), "Out of scope expression is only allowed in variable declaration.");
        ParsingExpression expr = parseExpression();
        expectThenNext(TokenType.EOL, "Expected new line after expression.");

        return new ParsingStatement.Expression(expr);
    }

    private ParsingExpression parseExpression() throws Exception {
        return parseAssignment();
    }

    private ParsingExpression parseAssignment() throws Exception {
        ParsingExpression expr = parseConcatenation();
        if (compareMultipleThenNext(TokenType.ASSIGNMENT)) {
            Token equals = getPrevious();
            ParsingExpression value = parseAssignment();
            if (expr instanceof ParsingExpression.Variable) {
                Token name = ((ParsingExpression.Variable) expr).name;
                TokenType type;
                type = variablesType.get(name.lexeme);
                if (value instanceof ParsingExpression.Literal
                        && !Token.checkType(((ParsingExpression.Literal) value).value, type))
                    throw lexerJ.newError(name, String.format("Expected '%s' type.", type));
                return new ParsingExpression.Assign(name, value, type);
            }
            throw lexerJ.newError(equals, "Invalid assignment target.");
        } else if (compareMultipleThenNext(TokenType.BOOL_LIT, TokenType.CHAR_LIT, TokenType.FLOAT_LIT,
                TokenType.INT_LIT, TokenType.STR_LIT, TokenType.IDENTIFIER)) {
            throw lexerJ.newError(getPrevious(), "Missing expression operator.");
        }

        return expr;
    }

    private ParsingExpression parseConcatenation() throws Exception {
        ParsingExpression expr = parseLogicalOr();
        while (compareMultipleThenNext(TokenType.AMPERSAND)) {
            Token operator = getPrevious();
            ParsingExpression right = parseLogicalOr();
            expr = new ParsingExpression.Binary(expr, operator, right);
        }

        return expr;
    }

    private ParsingExpression parseLogicalOr() throws Exception {
        ParsingExpression expr = parseLogicalAnd();
        while (compareMultipleThenNext(TokenType.OR)) {
            Token operator = getPrevious();
            ParsingExpression right = parseLogicalAnd();
            expectLogicalExpressions(right);
            expr = new ParsingExpression.Logical(expr, operator, right);
        }

        return expr;
    }

    private ParsingExpression parseLogicalAnd() throws Exception {
        ParsingExpression expr = parseEquality();
        while (compareMultipleThenNext(TokenType.AND)) {
            Token operator = getPrevious();
            ParsingExpression right = parseEquality();
            expectLogicalExpressions(right);
            expr = new ParsingExpression.Logical(expr, operator, right);
        }

        return expr;
    }

    private ParsingExpression parseEquality() throws Exception {
        ParsingExpression expr = parseComparison();
        while (compareMultipleThenNext(TokenType.NOT_EQUAL, TokenType.EQUAL)) {
            Token operator = getPrevious();
            ParsingExpression right = parseComparison();
            expr = new ParsingExpression.Binary(expr, operator, right);
        }

        return expr;
    }

    private ParsingExpression parseComparison() throws Exception {
        ParsingExpression expr = parseTerm();
        while (compareMultipleThenNext(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESSER,
                TokenType.LESSER_EQUAL)) {
            Token operator = getPrevious();
            ParsingExpression right = parseTerm();
            expr = new ParsingExpression.Binary(expr, operator, right);
        }

        return expr;
    }

    private ParsingExpression parseTerm() throws Exception {
        ParsingExpression expr = parseFactor();
        while (compareMultipleThenNext(TokenType.SUBTRACTION, TokenType.ADDITION)) {
            Token operator = getPrevious();
            ParsingExpression right = parseFactor();
            expr = new ParsingExpression.Binary(expr, operator, right);
        }

        return expr;
    }

    private ParsingExpression parseFactor() throws Exception {
        ParsingExpression expr = parseUnary();
        while (compareMultipleThenNext(TokenType.DIVISION, TokenType.MULTIPLICATION, TokenType.MODULO)) {
            Token operator = getPrevious();
            ParsingExpression right = parseUnary();
            expr = new ParsingExpression.Binary(expr, operator, right);
        }

        return expr;
    }

    private ParsingExpression parseUnary() throws Exception {
        if (compareMultipleThenNext(TokenType.ADDITION, TokenType.SUBTRACTION, TokenType.NOT)) {
            Token operator = getPrevious();
            ParsingExpression right = parseUnary();
            if (operator.type == TokenType.NOT)
                expectLogicalExpressions(right);
            return new ParsingExpression.Unary(operator, right);
        }

        return parsePrimary();
    }

    private ParsingExpression parsePrimary() throws Exception {
        if (compareMultipleThenNext(TokenType.DOLLAR))
            return new ParsingExpression.Literal("\n");
        if (compareMultipleThenNext(TokenType.OCTOTHORPE))
            return new ParsingExpression.Literal("\n");
        // Escape code: [x] — e.g. [#]->#, [[]->[ , []]->]
        if (compareMultipleThenNext(TokenType.LEFT_BRACE)) {
            String content;
            if (compareCurrent(TokenType.RIGHT_BRACE)) {
                // []] case: first ] is content, second ] closes the escape
                next(); // consume content ]
                expectThenNext(TokenType.RIGHT_BRACE, "Expected ']' to close escape sequence.");
                content = "]";
            } else {
                StringBuilder sb = new StringBuilder();
                while (!compareCurrent(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    sb.append(getCurrent().lexeme);
                    next();
                }
                expectThenNext(TokenType.RIGHT_BRACE, "Expected ']' to close escape sequence.");
                content = sb.toString();
            }
            if (content.equals("#"))
                return new ParsingExpression.Literal("#");
            return new ParsingExpression.Literal(content);
        }
        if (compareMultipleThenNext(TokenType.INT_LIT, TokenType.FLOAT_LIT, TokenType.BOOL_LIT, TokenType.CHAR_LIT,
                TokenType.STR_LIT))
            return new ParsingExpression.Literal(getPrevious().literal);
        if (compareMultipleThenNext(TokenType.IDENTIFIER)) {
            if (!varDeclarations && !variablesType.containsKey(getPrevious().lexeme))
                throw lexerJ.newError(getPrevious(), String.format("Undefined variable '%s'.", getPrevious().lexeme));
            return new ParsingExpression.Variable(getPrevious());
        }
        if (compareMultipleThenNext(TokenType.LEFT_PARENTHESIS)) {
            ParsingExpression expr = parseExpression();
            expectThenNext(TokenType.RIGHT_PARENTHESIS, "Expected ')' after expression.");
            return new ParsingExpression.Grouping(expr);
        }

        throw lexerJ.newError(getCurrent(), "Expected expression.");
    }

    private ParsingStatement parseIf() throws Exception {
        Token ifToken = getPrevious();
        expectThenNext(TokenType.LEFT_PARENTHESIS, "Expected '(' after 'IF'.");
        ParsingExpression condition = parseExpression();
        expectTokenAndEOLNext(TokenType.RIGHT_PARENTHESIS, "Expected ')' after condition.");
        // Expect START IF EOL
        expectThenNext(TokenType.START, "Expected 'START' before 'IF' block.");
        expectThenNext(TokenType.IF, "Expected 'IF' after 'START'.");
        expectThenNext(TokenType.EOL, "Expected new line after 'START IF'.");
        List<ParsingStatement> thenStmts = parseBlockStatements();
        // Expect END IF
        expectThenNext(TokenType.END, "Expected 'END' to close 'IF' block.");
        expectThenNext(TokenType.IF, "Expected 'IF' after 'END'.");
        if (!isAtEnd()) expectThenNext(TokenType.EOL, "Expected new line after 'END IF'.");

        ParsingStatement thenBranch = new ParsingStatement.Block(thenStmts);
        ParsingStatement elseBranch = null;

        if (compareCurrent(TokenType.ELSE)) {
            next(); // consume ELSE
            if (compareMultipleThenNext(TokenType.IF)) {
                // ELSE IF — recurse
                elseBranch = parseIf();
            } else {
                expectThenNext(TokenType.EOL, "Expected new line after 'ELSE'.");
                expectThenNext(TokenType.START, "Expected 'START' before 'IF' block.");
                expectThenNext(TokenType.IF, "Expected 'IF' after 'START'.");
                expectThenNext(TokenType.EOL, "Expected new line after 'START IF'.");
                List<ParsingStatement> elseStmts = parseBlockStatements();
                expectThenNext(TokenType.END, "Expected 'END' to close 'IF' block.");
                expectThenNext(TokenType.IF, "Expected 'IF' after 'END'.");
                if (!isAtEnd()) expectThenNext(TokenType.EOL, "Expected new line after 'END IF'.");
                elseBranch = new ParsingStatement.Block(elseStmts);
            }
        }

        return new ParsingStatement.If(condition, thenBranch, elseBranch, ifToken);
    }

    private List<ParsingStatement> parseBlockStatements() throws Exception {
        List<ParsingStatement> stmts = new ArrayList<>();
        while (!compareCurrent(TokenType.END) && !compareCurrent(TokenType.ELSE) && !isAtEnd())
            stmts.add(parseDeclaration());
        return stmts;
    }

    private ParsingStatement parsePrint() throws Exception {
        expectThenNext(TokenType.COLON, "Expected ':' after 'PRINT'.");
        ParsingExpression value = parseExpression();
        expectThenNext(TokenType.EOL, "Expected new line after expression.");

        return new ParsingStatement.Print(value);
    }

    private ParsingStatement parseScan() throws Exception {
        expectThenNext(TokenType.COLON, "Expected ':' after 'SCAN'.");
        List<ParsingExpression.Variable> variables = new ArrayList<ParsingExpression.Variable>();
        variables.add(
                new ParsingExpression.Variable(expectThenNext(TokenType.IDENTIFIER, "Expected identifier for scan")));
        while (compareMultipleThenNext(TokenType.COMMA))
            variables.add(new ParsingExpression.Variable(
                    expectThenNext(TokenType.IDENTIFIER, "Expected identifier for scan")));
        expectThenNext(TokenType.EOL, "Expected new line after expression.");

        return new ParsingStatement.Scan(variables.toArray(new ParsingExpression.Variable[0]));
    }

    private ParsingStatement parseRepeatWhen() throws Exception {
        expectThenNext(TokenType.WHEN, "Expected 'WHEN' after 'REPEAT'.");
        expectThenNext(TokenType.LEFT_PARENTHESIS, "Expected '(' after 'REPEAT WHEN'.");
        ParsingExpression condition = parseExpression();
        expectTokenAndEOLNext(TokenType.RIGHT_PARENTHESIS, "Expected ')' after condition.");
        expectThenNext(TokenType.START, "Expected 'START' before 'REPEAT' block.");
        expectThenNext(TokenType.REPEAT, "Expected 'REPEAT' after 'START'.");
        expectThenNext(TokenType.EOL, "Expected new line after 'START REPEAT'.");
        List<ParsingStatement> bodyStmts = parseBlockStatements();
        expectThenNext(TokenType.END, "Expected 'END' to close 'REPEAT' block.");
        expectThenNext(TokenType.REPEAT, "Expected 'REPEAT' after 'END'.");
        if (!isAtEnd()) expectThenNext(TokenType.EOL, "Expected new line after 'END REPEAT'.");

        return new ParsingStatement.RepeatWhen(condition, new ParsingStatement.Block(bodyStmts));
    }

    private ParsingStatement parseFor() throws Exception {
        expectThenNext(TokenType.LEFT_PARENTHESIS, "Expected '(' after 'FOR'.");
        ParsingExpression initializer = parseExpression();
        expectThenNext(TokenType.COMMA, "Expected ',' after FOR initializer.");
        ParsingExpression condition = parseExpression();
        expectThenNext(TokenType.COMMA, "Expected ',' after FOR condition.");
        ParsingExpression increment = parseExpression();
        expectTokenAndEOLNext(TokenType.RIGHT_PARENTHESIS, "Expected ')' after FOR clauses.");
        expectThenNext(TokenType.START, "Expected 'START' before 'FOR' block.");
        expectThenNext(TokenType.FOR, "Expected 'FOR' after 'START'.");
        expectThenNext(TokenType.EOL, "Expected new line after 'START FOR'.");
        List<ParsingStatement> bodyStmts = parseBlockStatements();
        expectThenNext(TokenType.END, "Expected 'END' to close 'FOR' block.");
        expectThenNext(TokenType.FOR, "Expected 'FOR' after 'END'.");
        if (!isAtEnd()) expectThenNext(TokenType.EOL, "Expected new line after 'END FOR'.");

        return new ParsingStatement.For(initializer, condition, increment, new ParsingStatement.Block(bodyStmts));
    }

    private Object expectLogicalExpressions(ParsingExpression expectFrom) throws Exception {
        TokenType type = TokenType.BOOL;
        Token erroneous = getPrevious();
        if (expectFrom instanceof ParsingExpression.Grouping)
            return expectLogicalExpressions(((ParsingExpression.Grouping) expectFrom).expression);
        if (expectFrom instanceof ParsingExpression.Unary instance) {
            if (instance.operator.type == TokenType.NOT)
                return expectLogicalExpressions(instance.right);
        }
        try {
            if (expectFrom instanceof ParsingExpression.Logical)
                return null;
            if (expectFrom instanceof ParsingExpression.Binary instance) {
                if (!Token.logicalComparisonOperators.contains(instance.operator.type)) {
                    erroneous = instance.operator;
                    throw new Exception();
                }
            } else if (expectFrom instanceof ParsingExpression.Literal instance) {
                if (!Token.checkType(instance.value, type))
                    throw new Exception();
            } else if (expectFrom instanceof ParsingExpression.Variable instance) {
                if (!Token.checkType(type, variablesType.get(instance.name.lexeme))) {
                    erroneous = instance.name;
                    throw new Exception();
                }
            } else
                throw new Exception();
        } catch (Exception e) {
            throw lexerJ.newError(erroneous, String.format("Expected '%s' evaluation result.", type));
        }
        return null;
    }

    private void expectTokenAndEOLNext(TokenType type, String expectMessage) throws Exception {
        expectThenNext(type, expectMessage);
        expectThenNext(TokenType.EOL,
                String.format("Missing new line after '%s'", Token.tokenTypeToLexeme.get(type)));
    }

    private void expectTokenAndEOL(TokenType type, String expectMessage) throws Exception {
        int tempCurrent = current;
        expectTokenAndEOLNext(type, expectMessage);
        current = tempCurrent;
    }

    private Token expectThenNext(TokenType type, String message) throws Exception {
        if (compareCurrent(type))
            return next();

        throw lexerJ.newError(getCurrent(), message);
    }

    private boolean compareMultipleThenNext(TokenType... types) {
        for (TokenType type : types) {
            if (compareCurrent(type)) {
                next();
                return true;
            }
        }

        return false;
    }

    private boolean compareCurrent(TokenType type) {
        if (isAtEnd())
            return false;
        return getCurrent().type == type;
    }

    private Token next() {
        if (!isAtEnd())
            current++;
        return getPrevious();
    }

    private boolean isAtEnd() {
        return getCurrent().type == TokenType.EOF;
    }

    private Token getCurrent() {
        return tokens.get(current);
    }

    private Token getPrevious() {
        return tokens.get(current - 1);
    }
}