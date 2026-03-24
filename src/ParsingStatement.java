/* SPDX-License-Identifier: GPL-3.0-or-later */

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

public sealed interface ParsingStatement permits
        ParsingStatement.Block,
        ParsingStatement.Expression,
        ParsingStatement.If,
        ParsingStatement.Print,
        ParsingStatement.Scan,
        ParsingStatement.Var,
        ParsingStatement.RepeatWhen,
        ParsingStatement.For {

    record Block(List<ParsingStatement> statements) implements ParsingStatement {
    }

    record Expression(ParsingExpression expression) implements ParsingStatement {
    }

    record If(ParsingExpression condition, ParsingStatement thenBranch, ParsingStatement elseBranch,
              Token ifToken) implements ParsingStatement {
    }

    record Print(ParsingExpression expression) implements ParsingStatement {
    }

    record Scan(ParsingExpression.Variable[] variables) implements ParsingStatement {
    }

    record Var(Token name, ParsingExpression initializer) implements ParsingStatement {
    }

    record RepeatWhen(ParsingExpression condition, ParsingStatement body) implements ParsingStatement {
    }

    record For(ParsingExpression initializer, ParsingExpression condition, ParsingExpression increment,
               ParsingStatement body) implements ParsingStatement {
    }
}