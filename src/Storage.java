/* SPDX-License-Identifier: GPL-3.0-or-later */

import java.util.HashMap;
import java.util.Map;

class Storage {
    private final Map<String, Object> variables = new HashMap<>();

    Object get(Token name) throws Exception {
        if (variables.containsKey(name.lexeme()))
            return variables.get(name.lexeme());
        throw new Exception("Undefined variable '%s'.".formatted(name.lexeme()));
    }

    void assign(Token name, Object value) throws Exception {
        if (variables.containsKey(name.lexeme())) {
            variables.put(name.lexeme(), value);
            return;
        }
        throw new Exception("Undefined variable '%s'.".formatted(name.lexeme()));
    }

    void define(String name, Object value) {
        variables.put(name, value);
    }
}