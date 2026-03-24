/* SPDX-License-Identifier: GPL-3.0-or-later */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private boolean varDeclarations = true;
    private boolean inScope = false;
    private int scopeCounter = 0;

    private final LexerJ lexerJ;
    private List<Token> tokens;
    private int current = 0;

    private final List<ParsingStatement> statements = new ArrayList<>();
    private final Map<String, TokenType> variablesType = new HashMap<>();

    public Parser(LexerJ lexerJ) {
        this.lexerJ = lexerJ;
    }

    List<ParsingStatement> parse(List<Token> tokens) throws Exception {
        this.tokens = tokens;
        expect(TokenType.SCRIPT, "Expected 'SCRIPT' at start of program.");
        expect(TokenType.AREA, "Expected 'AREA' after 'SCRIPT'.");
        expect(TokenType.EOL, "Expected new line after 'SCRIPT AREA'.");
        expect(TokenType.START, "Expected 'START' before program block.");
        expect(TokenType.SCRIPT, "Expected 'SCRIPT' after 'START'.");
        expect(TokenType.EOL, "Expected new line after 'START SCRIPT'.");
        inScope = true;
        scopeCounter++;
        varDeclarations = true;
        while (!check(TokenType.END) && !isAtEnd())
            statements.add(parseDeclaration());
        expect(TokenType.END, "Expected 'END' to close program block.");
        expect(TokenType.SCRIPT, "Expected 'SCRIPT' after 'END'.");
        if (!isAtEnd())
            expect(TokenType.EOL, "Expected new line after 'END SCRIPT'.");
        return statements;
    }

    private ParsingStatement parseDeclaration() throws Exception {
        if (match(TokenType.DECLARE)) {
            return parseVariableDeclaration();
        }
        varDeclarations = false;
        return parseStatement();
    }

    private ParsingStatement parseVariableDeclaration() throws Exception {
        if (!varDeclarations) {
            throw lexerJ.newError(prev(), "Misplaced variable declaration.");
        }

        if (!matchAny(TokenType.BOOL, TokenType.CHAR, TokenType.FLOAT, TokenType.INT)) {
            throw lexerJ.newError(current(), "Expected data type after 'DECLARE'.");
        }
        var type = prev().type();

        var name = parseVarName();
        var init = parseOptionalInit(name, type);
        registerVar(name, type);

        var returnVar = new ParsingStatement.Var(name, init);

        boolean many = false;
        while (match(TokenType.COMMA)) {
            if (!many) {
                many = true;
                statements.add(returnVar);
            }
            name = parseVarName();
            init = parseOptionalInit(name, type);
            registerVar(name, type);
            statements.add(new ParsingStatement.Var(name, init));
        }

        expect(TokenType.EOL, "Expected new line after declaration.");

        if (many) {
            returnVar = (ParsingStatement.Var) statements.removeLast();
        }

        return returnVar;
    }

    private Token parseVarName() throws Exception {
        if (check(TokenType.IDENTIFIER)) {
            return expect(TokenType.IDENTIFIER, "Expected variable name.");
        }

        if (Token.reservedWords.containsKey(current().lexeme())) {
            throw lexerJ.newError(current(), "Expected valid variable name but got reserved keyword.");
        }
        throw lexerJ.newError(current(), "Expected valid variable name.");
    }

    private ParsingExpression parseOptionalInit(Token name, TokenType type) throws Exception {
        if (!match(TokenType.ASSIGNMENT)) {
            return defaultLiteral(type);
        }

        var init = parseExpression();
        if (init instanceof ParsingExpression.Literal(var val)) {
            if (type == TokenType.FLOAT && Token.checkType(val, TokenType.INT))
                return new ParsingExpression.Literal(Double.parseDouble(val.toString()));
            if (!Token.checkType(val, type))
                throw lexerJ.newError(name, "Expected '%s' type.".formatted(type));
        }
        return init;
    }

    private void registerVar(Token name, TokenType type) throws Exception {
        if (variablesType.containsKey(name.lexeme())) {
            throw lexerJ.newError(name, "Variable name '%s' is already declared.".formatted(name.lexeme()));
        }
        variablesType.put(name.lexeme(), type);
    }

    private ParsingStatement parseStatement() throws Exception {
        if (!inScope) {
            throw lexerJ.newError(current(), "Statement is out of scope.");
        }
        if (match(TokenType.IF)) {
            return parseIf();
        }
        if (match(TokenType.PRINT)) {
            return parsePrint();
        }
        if (match(TokenType.SCAN)) {
            return parseScan();
        }
        if (match(TokenType.REPEAT)) {
            return parseRepeatWhen();
        }
        if (match(TokenType.FOR)) {
            return parseFor();
        }
        return parseExpressionStatement();
    }

    private ParsingStatement parseExpressionStatement() throws Exception {
        var expr = parseExpression();
        expect(TokenType.EOL, "Expected new line after expression.");
        return new ParsingStatement.Expression(expr);
    }

    private ParsingStatement parseIf() throws Exception {
        var ifToken = prev();
        expect(TokenType.LEFT_PARENTHESIS, "Expected '(' after 'IF'.");
        var condition = parseExpression();
        expectAndEOL(TokenType.RIGHT_PARENTHESIS, "Expected ')' after condition.");
        expect(TokenType.START, "Expected 'START' before 'IF' block.");
        expect(TokenType.IF, "Expected 'IF' after 'START'.");
        expect(TokenType.EOL, "Expected new line after 'START IF'.");
        var thenStmts = parseBlockUntilEnd();
        expect(TokenType.END, "Expected 'END' to close 'IF' block.");
        expect(TokenType.IF, "Expected 'IF' after 'END'.");
        if (!isAtEnd()) expect(TokenType.EOL, "Expected new line after 'END IF'.");

        ParsingStatement elseBranch = null;
        if (check(TokenType.ELSE)) {
            advance();
            if (match(TokenType.IF)) {
                elseBranch = parseIf();
            } else {
                expect(TokenType.EOL, "Expected new line after 'ELSE'.");
                expect(TokenType.START, "Expected 'START' before 'IF' block.");
                expect(TokenType.IF, "Expected 'IF' after 'START'.");
                expect(TokenType.EOL, "Expected new line after 'START IF'.");
                var elseStmts = parseBlockUntilEnd();
                expect(TokenType.END, "Expected 'END' to close 'IF' block.");
                expect(TokenType.IF, "Expected 'IF' after 'END'.");
                if (!isAtEnd()) expect(TokenType.EOL, "Expected new line after 'END IF'.");
                elseBranch = new ParsingStatement.Block(elseStmts);
            }
        }

        return new ParsingStatement.If(condition, new ParsingStatement.Block(thenStmts), elseBranch, ifToken);
    }

    private ParsingStatement parsePrint() throws Exception {
        expect(TokenType.COLON, "Expected ':' after 'PRINT'.");
        var value = parseExpression();
        expect(TokenType.EOL, "Expected new line after expression.");
        return new ParsingStatement.Print(value);
    }

    private ParsingStatement parseScan() throws Exception {
        expect(TokenType.COLON, "Expected ':' after 'SCAN'.");
        var vars = new ArrayList<ParsingExpression.Variable>();
        vars.add(new ParsingExpression.Variable(expect(TokenType.IDENTIFIER, "Expected identifier for SCAN.")));
        while (match(TokenType.COMMA))
            vars.add(new ParsingExpression.Variable(expect(TokenType.IDENTIFIER, "Expected identifier for SCAN.")));
        expect(TokenType.EOL, "Expected new line after expression.");
        return new ParsingStatement.Scan(vars.toArray(new ParsingExpression.Variable[0]));
    }

    private ParsingStatement parseRepeatWhen() throws Exception {
        expect(TokenType.WHEN, "Expected 'WHEN' after 'REPEAT'.");
        expect(TokenType.LEFT_PARENTHESIS, "Expected '(' after 'REPEAT WHEN'.");
        var condition = parseExpression();
        expectAndEOL(TokenType.RIGHT_PARENTHESIS, "Expected ')' after condition.");
        expect(TokenType.START, "Expected 'START' before 'REPEAT' block.");
        expect(TokenType.REPEAT, "Expected 'REPEAT' after 'START'.");
        expect(TokenType.EOL, "Expected new line after 'START REPEAT'.");
        var body = parseBlockUntilEnd();
        expect(TokenType.END, "Expected 'END' to close 'REPEAT' block.");
        expect(TokenType.REPEAT, "Expected 'REPEAT' after 'END'.");
        if (!isAtEnd()) {
            expect(TokenType.EOL, "Expected new line after 'END REPEAT'.");
        }
        return new ParsingStatement.RepeatWhen(condition, new ParsingStatement.Block(body));
    }

    private ParsingStatement parseFor() throws Exception {
        expect(TokenType.LEFT_PARENTHESIS, "Expected '(' after 'FOR'.");
        var init = parseExpression();
        expect(TokenType.COMMA, "Expected ',' after FOR initializer.");
        var cond = parseExpression();
        expect(TokenType.COMMA, "Expected ',' after FOR condition.");
        var incr = parseExpression();
        expectAndEOL(TokenType.RIGHT_PARENTHESIS, "Expected ')' after FOR clauses.");
        expect(TokenType.START, "Expected 'START' before 'FOR' block.");
        expect(TokenType.FOR, "Expected 'FOR' after 'START'.");
        expect(TokenType.EOL, "Expected new line after 'START FOR'.");
        var body = parseBlockUntilEnd();
        expect(TokenType.END, "Expected 'END' to close 'FOR' block.");
        expect(TokenType.FOR, "Expected 'FOR' after 'END'.");
        if (!isAtEnd()) {
            expect(TokenType.EOL, "Expected new line after 'END FOR'.");
        }
        return new ParsingStatement.For(init, cond, incr, new ParsingStatement.Block(body));
    }

    private List<ParsingStatement> parseBlockUntilEnd() throws Exception {
        var stmts = new ArrayList<ParsingStatement>();
        while (!check(TokenType.END) && !check(TokenType.ELSE) && !isAtEnd()) {
            stmts.add(parseDeclaration());
        }
        return stmts;
    }

    private ParsingExpression parseExpression() throws Exception {
        return parseAssignment();
    }

    private ParsingExpression parseAssignment() throws Exception {
        var expr = parseConcatenation();
        if (match(TokenType.ASSIGNMENT)) {
            var equals = prev();
            var value = parseAssignment();
            if (expr instanceof ParsingExpression.Variable(var name)) {
                var type = variablesType.get(name.lexeme());
                if (value instanceof ParsingExpression.Literal(var val)
                        && !Token.checkType(val, type)) {
                    throw lexerJ.newError(name, "Expected '%s' type.".formatted(type));
                }
                return new ParsingExpression.Assign(name, value, type);
            }
            throw lexerJ.newError(equals, "Invalid assignment target.");
        } else if (matchAny(TokenType.BOOL_LIT, TokenType.CHAR_LIT, TokenType.FLOAT_LIT,
                TokenType.INT_LIT, TokenType.STR_LIT, TokenType.IDENTIFIER)) {
            throw lexerJ.newError(prev(), "Missing expression operator.");
        }
        return expr;
    }

    private ParsingExpression parseConcatenation() throws Exception {
        var expr = parseLogicalOr();
        while (match(TokenType.AMPERSAND)) {
            var op = prev();
            var right = parseLogicalOr();
            expr = new ParsingExpression.Binary(expr, op, right);
        }
        return expr;
    }

    private ParsingExpression parseLogicalOr() throws Exception {
        var expr = parseLogicalAnd();
        while (match(TokenType.OR)) {
            var op = prev();
            var right = parseLogicalAnd();
            expectLogical(right);
            expr = new ParsingExpression.Logical(expr, op, right);
        }
        return expr;
    }

    private ParsingExpression parseLogicalAnd() throws Exception {
        var expr = parseEquality();
        while (match(TokenType.AND)) {
            var op = prev();
            var right = parseEquality();
            expectLogical(right);
            expr = new ParsingExpression.Logical(expr, op, right);
        }
        return expr;
    }

    private ParsingExpression parseEquality() throws Exception {
        var expr = parseComparison();
        while (matchAny(TokenType.NOT_EQUAL, TokenType.EQUAL)) {
            var op = prev();
            var right = parseComparison();
            expr = new ParsingExpression.Binary(expr, op, right);
        }
        return expr;
    }

    private ParsingExpression parseComparison() throws Exception {
        var expr = parseTerm();
        while (matchAny(TokenType.GREATER, TokenType.GREATER_EQUAL,
                TokenType.LESSER, TokenType.LESSER_EQUAL)) {
            var op = prev();
            var right = parseTerm();
            expr = new ParsingExpression.Binary(expr, op, right);
        }
        return expr;
    }

    private ParsingExpression parseTerm() throws Exception {
        var expr = parseFactor();
        while (matchAny(TokenType.SUBTRACTION, TokenType.ADDITION)) {
            var op = prev();
            var right = parseFactor();
            expr = new ParsingExpression.Binary(expr, op, right);
        }
        return expr;
    }

    private ParsingExpression parseFactor() throws Exception {
        var expr = parseUnary();
        while (matchAny(TokenType.DIVISION, TokenType.MULTIPLICATION, TokenType.MODULO)) {
            var op = prev();
            var right = parseUnary();
            expr = new ParsingExpression.Binary(expr, op, right);
        }
        return expr;
    }

    private ParsingExpression parseUnary() throws Exception {
        if (matchAny(TokenType.ADDITION, TokenType.SUBTRACTION, TokenType.NOT)) {
            var op = prev();
            var right = parseUnary();
            if (op.type() == TokenType.NOT) expectLogical(right);
            return new ParsingExpression.Unary(op, right);
        }
        return parsePrimary();
    }

    private ParsingExpression parsePrimary() throws Exception {
        if (match(TokenType.DOLLAR))
            return new ParsingExpression.Literal("\n");
        if (match(TokenType.OCTOTHORPE))
            return new ParsingExpression.Literal("\n");

        // Escape sequence: [x] → literal content; []] → ]; [[] → [; [#] → #
        if (match(TokenType.LEFT_BRACE)) {
            String content;
            if (check(TokenType.RIGHT_BRACE)) {
                advance(); // consume content ]
                expect(TokenType.RIGHT_BRACE, "Expected ']' to close escape sequence.");
                content = "]";
            } else {
                var sb = new StringBuilder();
                while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    sb.append(current().lexeme());
                    advance();
                }
                expect(TokenType.RIGHT_BRACE, "Expected ']' to close escape sequence.");
                content = sb.toString();
            }
            return new ParsingExpression.Literal(content.equals("#") ? "#" : content);
        }

        if (matchAny(TokenType.INT_LIT, TokenType.FLOAT_LIT,
                TokenType.BOOL_LIT, TokenType.CHAR_LIT, TokenType.STR_LIT))
            return new ParsingExpression.Literal(prev().literal());

        if (match(TokenType.IDENTIFIER)) {
            if (!varDeclarations && !variablesType.containsKey(prev().lexeme()))
                throw lexerJ.newError(prev(), "Undefined variable '%s'.".formatted(prev().lexeme()));
            return new ParsingExpression.Variable(prev());
        }

        if (match(TokenType.LEFT_PARENTHESIS)) {
            var expr = parseExpression();
            expect(TokenType.RIGHT_PARENTHESIS, "Expected ')' after expression.");
            return new ParsingExpression.Grouping(expr);
        }

        throw lexerJ.newError(current(), "Expected expression.");
    }

    private void expectLogical(ParsingExpression expr) throws Exception {
        var erroneous = prev();
        try {
            switch (expr) {
                case ParsingExpression.Logical ignored -> { /* ok */ }
                case ParsingExpression.Grouping(var inner) -> expectLogical(inner);
                case ParsingExpression.Unary(var op, var r)
                        when op.type() == TokenType.NOT -> expectLogical(r);
                case ParsingExpression.Binary(var l, var op, var r) -> {
                    if (!Token.logicalComparisonOperators.contains(op.type())) {
                        erroneous = op;
                        throw new Exception();
                    }
                }
                case ParsingExpression.Literal(var val) -> {
                    if (!Token.checkType(val, TokenType.BOOL)) throw new Exception();
                }
                case ParsingExpression.Variable(var name) -> {
                    if (!Token.checkType(TokenType.BOOL, variablesType.get(name.lexeme()))) {
                        erroneous = name;
                        throw new Exception();
                    }
                }
                default -> throw new Exception();
            }
        } catch (Exception e) {
            throw lexerJ.newError(erroneous, "Expected 'BOOL' evaluation result.");
        }
    }

    private Token expect(TokenType type, String message) throws Exception {
        if (check(type)) return advance();
        throw lexerJ.newError(current(), message);
    }

    private void expectAndEOL(TokenType type, String message) throws Exception {
        expect(type, message);
        expect(TokenType.EOL, "Missing new line after '%s'".formatted(Token.tokenTypeToLexeme.get(type)));
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchAny(TokenType... types) {
        for (var t : types) {
            if (match(t)) return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && current().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return prev();
    }

    private boolean isAtEnd() {
        return current().type() == TokenType.EOF;
    }

    private Token current() {
        return tokens.get(current);
    }

    private Token prev() {
        return tokens.get(current - 1);
    }

    private ParsingExpression.Literal defaultLiteral(TokenType type) {
        return new ParsingExpression.Literal(switch (type) {
            case BOOL -> false;
            case CHAR -> '\0';
            case FLOAT -> 0.0;
            case INT -> 0;
            default -> null;
        });
    }
}