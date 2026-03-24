/* SPDX-License-Identifier: GPL-3.0-or-later */

// expression     → assignment ;
// assignment     → IDENTIFIER "=" assignment | logical_or ;
// logic_or       → logic_and ( "OR" logic_and )* ;
// logic_and      → equality ( "AND" equality )* ;
// equality       → comparison ( ( "<>" | "==" ) comparison )* ;
// comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
// term           → factor ( ( "-" | "+" | "&" ) factor )* ;
// factor         → unary ( ( "/" | "*" | "%" ) unary )* ;
// unary          → ( "+" | "-" | "NOT" ) unary | primary ;
// primary        → INT
//                | FLOAT
//                | BOOL
//                | CHAR
//                | STRING
//                | "(" expression ")"
//                | IDENTIFIER ;

public sealed interface ParsingExpression permits
        ParsingExpression.Assign,
        ParsingExpression.Binary,
        ParsingExpression.Grouping,
        ParsingExpression.Literal,
        ParsingExpression.Logical,
        ParsingExpression.Unary,
        ParsingExpression.Variable {

    record Assign(Token name, ParsingExpression value, TokenType type) implements ParsingExpression {
    }

    record Binary(ParsingExpression left, Token operator, ParsingExpression right) implements ParsingExpression {
    }

    record Grouping(ParsingExpression expression) implements ParsingExpression {
    }

    record Literal(Object value) implements ParsingExpression {
    }

    record Logical(ParsingExpression left, Token operator, ParsingExpression right) implements ParsingExpression {
    }

    record Unary(Token operator, ParsingExpression right) implements ParsingExpression {
    }

    record Variable(Token name) implements ParsingExpression {
    }
}