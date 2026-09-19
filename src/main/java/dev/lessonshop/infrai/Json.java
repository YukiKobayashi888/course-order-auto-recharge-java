package dev.lessonshop.infrai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.done()) throw new IllegalArgumentException("Trailing JSON content");
        return value;
    }

    static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(entry.getKey().toString())).append(':').append(stringify(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                first = false;
                out.append(stringify(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Cannot encode " + value.getClass().getName());
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    private static final class Parser {
        private final String source;
        private int at;

        private Parser(String source) { this.source = source; }
        private boolean done() { return at == source.length(); }
        private void whitespace() { while (!done() && Character.isWhitespace(source.charAt(at))) at++; }

        private Object value() {
            whitespace();
            if (done()) throw new IllegalArgumentException("Expected JSON value");
            return switch (source.charAt(at)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            at++;
            whitespace();
            if (take('}')) return map;
            do {
                whitespace();
                String key = string();
                whitespace();
                expect(':');
                map.put(key, value());
                whitespace();
            } while (take(','));
            expect('}');
            return map;
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            at++;
            whitespace();
            if (take(']')) return list;
            do { list.add(value()); whitespace(); } while (take(','));
            expect(']');
            return list;
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (!done()) {
                char c = source.charAt(at++);
                if (c == '"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                if (done()) throw new IllegalArgumentException("Incomplete JSON escape");
                char escaped = source.charAt(at++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (at + 4 > source.length()) throw new IllegalArgumentException("Incomplete unicode escape");
                        out.append((char) Integer.parseInt(source.substring(at, at + 4), 16));
                        at += 4;
                    }
                    default -> throw new IllegalArgumentException("Unknown JSON escape");
                }
            }
            throw new IllegalArgumentException("Unclosed JSON string");
        }

        private Object number() {
            int start = at;
            if (source.charAt(at) == '-') at++;
            while (!done() && Character.isDigit(source.charAt(at))) at++;
            if (!done() && source.charAt(at) == '.') {
                at++;
                while (!done() && Character.isDigit(source.charAt(at))) at++;
            }
            if (!done() && (source.charAt(at) == 'e' || source.charAt(at) == 'E')) {
                at++;
                if (!done() && (source.charAt(at) == '+' || source.charAt(at) == '-')) at++;
                while (!done() && Character.isDigit(source.charAt(at))) at++;
            }
            String token = source.substring(start, at);
            try { return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.parseDouble(token) : Long.parseLong(token); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid JSON number", error); }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, at)) throw new IllegalArgumentException("Invalid JSON literal");
            at += token.length();
            return value;
        }

        private boolean take(char expected) {
            if (!done() && source.charAt(at) == expected) { at++; return true; }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected '" + expected + "'");
        }
    }
}
