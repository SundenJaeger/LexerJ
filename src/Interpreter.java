/* SPDX-License-Identifier: GPL-3.0-or-later */

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class Interpreter {
    private final LexerJ lexerJ;
    private final Storage global = new Storage();
    private final Scanner scanner;

    public Interpreter(LexerJ lexerJ, Scanner scanner) {
        this.lexerJ = lexerJ;
        this.scanner = scanner;
    }

    void interpret(List<ParsingStatement> statements) throws Exception {
        for (var stmt : statements) {
            execute(stmt);
        }
    }

    private void execute(ParsingStatement stmt) throws Exception {
        switch (stmt) {
            case ParsingStatement.Block(var statements) -> {
                for (var s : statements) {
                    execute(s);
                }
            }
            case ParsingStatement.Expression(var expr) -> evaluate(expr);
            case ParsingStatement.Print(var expr) -> System.out.print(stringify(evaluate(expr)));
            case ParsingStatement.Scan(var vars) -> scan(vars);
            case ParsingStatement.Var(var name, var init) -> {
                Object value = init != null ? evaluate(init) : null;
                global.define(name.lexeme(), value);
            }
            case ParsingStatement.If(var cond, var then, var elseBranch, var ifToken) -> {
                try {
                    if (toBoolean(evaluate(cond))) {
                        execute(then);
                    } else if (elseBranch != null) {
                        execute(elseBranch);
                    }
                } catch (Exception e) {
                    throw lexerJ.newError(ifToken, e.getMessage());
                }
            }
            case ParsingStatement.RepeatWhen(var cond, var body) -> {
                while (toBoolean(evaluate(cond))) {
                    execute(body);
                }
            }
            case ParsingStatement.For(var init, var cond, var incr, var body) -> {
                evaluate(init);
                while (toBoolean(evaluate(cond))) {
                    execute(body);
                    evaluate(incr);
                }
            }
        }
    }

    public void scan(ParsingExpression.Variable[] vars) throws Exception {
        String line = scanner.nextLine();
        String[] tokens = Arrays.stream(line.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        if (tokens.length < vars.length) {
            throw lexerJ.newError(vars[0].name(),
                    "Expected " + vars.length + " value(s) but got " + tokens.length + ".");
        }
        for (int x = 0; x < vars.length; x++) {
            var v = vars[x];
            Object value = global.get(v.name());
            try {
                switch (value.getClass().getName()) {
                    case "java.lang.Character" -> {
                        if (tokens[x].length() != 1) throw new Exception();
                        global.assign(v.name(), tokens[x].charAt(0));
                    }
                    case "java.lang.Double" -> global.assign(v.name(), Double.parseDouble(tokens[x]));
                    case "java.lang.Integer" -> global.assign(v.name(), Integer.parseInt(tokens[x]));
                    case "java.lang.Boolean" -> {
                        boolean belongs = tokens[x].equals("TRUE") || tokens[x].equals("FALSE");
                        if (!belongs) throw new Exception();
                        global.assign(v.name(), tokens[x].equals("TRUE"));
                    }
                    default -> throw new Exception();
                }
            } catch (Exception e) {
                throw lexerJ.newError(v.name(), "Unsupported input data type.");
            }
        }
    }

    public Object unary(Token op, Object right) throws Exception {
        return switch (op.type()) {
            case NOT -> {
                try {
                    yield !toBoolean(right);
                } catch (Exception e) {
                    throw lexerJ.newError(op, e.getMessage());
                }
            }
            case ADDITION -> {
                checkNumberOperand(op, right);
                yield right;
            }
            case SUBTRACTION -> {
                checkNumberOperand(op, right);
                yield switch (right) {
                    case Double d -> -d;
                    case Integer i -> -i;
                    default -> null;
                };
            }
            default -> throw lexerJ.newError(op, "Invalid unary operator.");
        };
    }

    private boolean toBoolean(Object object) throws Exception {
        if (object instanceof Boolean) {
            return (boolean) object;
        }

        throw new Exception("Operand must be a boolean.");
    }

    private Object evaluate(ParsingExpression expr) throws Exception {
        return switch (expr) {
            case ParsingExpression.Literal(var value) -> value;
            case ParsingExpression.Grouping(var inner) -> evaluate(inner);
            case ParsingExpression.Variable(var name) -> global.get(name);
            case ParsingExpression.Assign(var name, var value, var type) -> {
                Object val = evaluate(value);
                if (type == TokenType.FLOAT && Token.checkType(val, TokenType.INT)) {
                    val = Double.parseDouble(val.toString());
                }

                if (!Token.checkType(val, type)) {
                    throw lexerJ.newError(name, "Expected expression value as '%s'.".formatted(type));
                }

                global.assign(name, val);
                yield val;
            }
            case ParsingExpression.Unary(var op, var right) -> unary(op, evaluate(right));
            case ParsingExpression.Logical(var left, var op, var right) -> {
                var l = evaluate(left);
                try {
                    if (op.type() == TokenType.OR) {
                        if (toBoolean(l)) yield l;
                    } else {
                        if (!toBoolean(l)) yield l;
                    }
                } catch (Exception e) {
                    throw lexerJ.newError(op, e.getMessage());
                }
                yield evaluate(right);
            }
            case ParsingExpression.Binary(var left, var op, var right) -> binary(evaluate(left), op, evaluate(right));
        };
    }

    public Object binary(Object left, Token op, Object right) throws Exception {
        return switch (op.type()) {
            case GREATER -> {
                checkNumberOperands(op, left, right);
                yield compareNumbers(left, right) > 0;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(op, left, right);
                yield compareNumbers(left, right) >= 0;
            }
            case LESSER -> {
                checkNumberOperands(op, left, right);
                yield compareNumbers(left, right) < 0;
            }
            case LESSER_EQUAL -> {
                checkNumberOperands(op, left, right);
                yield compareNumbers(left, right) <= 0;
            }
            case EQUAL -> isEqual(left, right);
            case NOT_EQUAL -> !isEqual(left, right);
            case ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION -> {
                checkNumberOperands(op, left, right);
                yield applyArithmetic(left, right, op.type());
            }
            case MODULO -> {
                if (left instanceof Integer l && right instanceof Integer r) yield l % r;
                throw lexerJ.newError(op, "Operand must be an integer.");
            }
            case AMPERSAND -> stringify(left) + stringify(right);
            default -> throw lexerJ.newError(op, "Invalid binary operator.");
        };
    }

    private double compareNumbers(Object left, Object right) {
        double l = left instanceof Integer i ? i : (Double) left;
        double r = right instanceof Integer i ? i : (Double) right;
        return Double.compare(l, r);
    }

    private Object applyArithmetic(Object left, Object right, TokenType op) {
        if (left instanceof Integer l && right instanceof Integer r) {
            return switch (op) {
                case ADDITION -> l + r;
                case SUBTRACTION -> l - r;
                case MULTIPLICATION -> l * r;
                case DIVISION -> l / r;
                default -> null;
            };
        }
        double l = left instanceof Integer i ? i : (Double) left;
        double r = right instanceof Integer i ? i : (Double) right;
        return switch (op) {
            case ADDITION -> l + r;
            case SUBTRACTION -> l - r;
            case MULTIPLICATION -> l * r;
            case DIVISION -> l / r;
            default -> null;
        };
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) throws Exception {
        if (operand instanceof Double || operand instanceof Integer) {
            return;
        }

        throw lexerJ.newError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) throws Exception {
        if ((left instanceof Double || left instanceof Integer) && (right instanceof Double || right instanceof Integer)) {
            return;
        }

        throw lexerJ.newError(operator, "Operand must be a number.");
    }

    private String stringify(Object object) {
        if (object == null) {
            return "null";
        }
        if ("java.lang.Boolean".equals(object.getClass().getName())) {
            return object.toString().toUpperCase();
        }

        return object.toString();
    }
}