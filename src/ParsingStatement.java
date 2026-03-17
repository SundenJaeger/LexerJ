// program        → "SCRIPT" "AREA" EOL declaration* EOF ;
// declaration    → varDecl
//                | statement ;
// varDecl        → "DECLARE" dataType IDENTIFIER ( "=" expression )? ( "," IDENTIFIER ( "=" expression )? )* "EOL" ;
// statement      → exprStmt
//                | printStmt
//                | scanStmt
//                | ifStmt
//                | repeatStmt
//                | forStmt
//                | block ;
// exprStmt       → expression "EOL" ;
// printStmt      → "PRINT" ":" expression "EOL" ;
// scanStmt       → "SCAN" ":" IDENTIFIER ( "," IDENTIFIER )* "EOL" ;
// ifStmt         → "IF" "(" expression ")" EOL "START" "IF" EOL statement "END" "IF" EOL
//                ( "ELSE" "IF" "(" expression ")" EOL "START" "IF" EOL statement "END" "IF" EOL )*
//                ( "ELSE" EOL "START" "IF" EOL statement "END" "IF" EOL )? ;
// repeatStmt     → "REPEAT" "WHEN" "(" expression ")" EOL "START" "REPEAT" EOL statement "END" "REPEAT" EOL ;
// forStmt        → "FOR" "(" expression ";" expression ";" expression ")" EOL "START" "FOR" EOL statement "END" "FOR" EOL ;
// block          → "START" "SCRIPT" EOL declaration* "END" "SCRIPT" EOL ;

import java.util.List;

public abstract class ParsingStatement {
    interface Visitor<R> {
        R block(Block stmt) throws Exception;

        R expression(Expression stmt) throws Exception;

        R ifS(If stmt) throws Exception;

        R print(Print stmt) throws Exception;

        R scan(Scan stmt) throws Exception;

        R var(Var stmt) throws Exception;

        R repeatWhen(RepeatWhen stmt) throws Exception;

        R forS(For stmt) throws Exception;
    }

    static class Block extends ParsingStatement {
        Block(List<ParsingStatement> statements) {
            this.statements = statements;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.block(this);
        }

        final List<ParsingStatement> statements;
    }

    static class Expression extends ParsingStatement {
        Expression(ParsingExpression expression) {
            this.expression = expression;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.expression(this);
        }

        final ParsingExpression expression;
    }

    static class If extends ParsingStatement {
        If(ParsingExpression condition, ParsingStatement thenBranch, ParsingStatement elseBranch, Token ifToken) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
            this.ifToken = ifToken;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.ifS(this);
        }

        final Token ifToken;
        final ParsingExpression condition;
        final ParsingStatement thenBranch;
        final ParsingStatement elseBranch;
    }

    static class Print extends ParsingStatement {
        Print(ParsingExpression expression) {
            this.expression = expression;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.print(this);
        }

        final ParsingExpression expression;
    }

    static class Scan extends ParsingStatement {
        Scan(ParsingExpression.Variable[] variables) {
            this.variables = variables;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.scan(this);
        }

        final ParsingExpression.Variable[] variables;
    }

    static class Var extends ParsingStatement {
        Var(Token name, ParsingExpression initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.var(this);
        }

        final Token name;
        final ParsingExpression initializer;
    }

    static class RepeatWhen extends ParsingStatement {
        RepeatWhen(ParsingExpression condition, ParsingStatement body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.repeatWhen(this);
        }

        final ParsingExpression condition;
        final ParsingStatement body;
    }

    static class For extends ParsingStatement {
        For(ParsingExpression initializer, ParsingExpression condition, ParsingExpression increment, ParsingStatement body) {
            this.initializer = initializer;
            this.condition = condition;
            this.increment = increment;
            this.body = body;
        }

        @Override
        <R> R visit(Visitor<R> visitor) throws Exception {
            return visitor.forS(this);
        }

        final ParsingExpression initializer;
        final ParsingExpression condition;
        final ParsingExpression increment;
        final ParsingStatement body;
    }

    abstract <R> R visit(Visitor<R> visitor) throws Exception;
}