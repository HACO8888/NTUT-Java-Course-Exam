package com.bigtwo.net;

import com.bigtwo.model.*;

import java.util.*;

public class JsonUtil {

    public static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":");
            sb.append(valueToJson(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String valueToJson(Object val) {
        if (val == null) return "null";
        if (val instanceof String) return "\"" + escape((String) val) + "\"";
        if (val instanceof Number || val instanceof Boolean) return val.toString();
        if (val instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) val;
            return toJson(m);
        }
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(valueToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(val.toString()) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static Map<String, Object> parseJson(String json) {
        json = json.trim();
        if (!json.startsWith("{")) return Collections.emptyMap();
        return parseObject(new int[]{1}, json);
    }

    private static Map<String, Object> parseObject(int[] pos, String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace(pos, json);
        if (pos[0] < json.length() && json.charAt(pos[0]) == '}') {
            pos[0]++;
            return map;
        }
        while (pos[0] < json.length()) {
            skipWhitespace(pos, json);
            String key = parseString(pos, json);
            skipWhitespace(pos, json);
            expect(pos, json, ':');
            skipWhitespace(pos, json);
            Object value = parseValue(pos, json);
            map.put(key, value);
            skipWhitespace(pos, json);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
            } else {
                break;
            }
        }
        skipWhitespace(pos, json);
        if (pos[0] < json.length() && json.charAt(pos[0]) == '}') pos[0]++;
        return map;
    }

    private static Object parseValue(int[] pos, String json) {
        skipWhitespace(pos, json);
        char c = json.charAt(pos[0]);
        if (c == '"') return parseString(pos, json);
        if (c == '{') { pos[0]++; return parseObject(pos, json); }
        if (c == '[') return parseArray(pos, json);
        if (c == 't' || c == 'f') return parseBoolean(pos, json);
        if (c == 'n') { pos[0] += 4; return null; }
        return parseNumber(pos, json);
    }

    private static String parseString(int[] pos, String json) {
        expect(pos, json, '"');
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char next = json.charAt(pos[0]++);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<Object> parseArray(int[] pos, String json) {
        expect(pos, json, '[');
        List<Object> list = new ArrayList<>();
        skipWhitespace(pos, json);
        if (pos[0] < json.length() && json.charAt(pos[0]) == ']') {
            pos[0]++;
            return list;
        }
        while (pos[0] < json.length()) {
            skipWhitespace(pos, json);
            list.add(parseValue(pos, json));
            skipWhitespace(pos, json);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
            } else {
                break;
            }
        }
        skipWhitespace(pos, json);
        if (pos[0] < json.length() && json.charAt(pos[0]) == ']') pos[0]++;
        return list;
    }

    private static Number parseNumber(int[] pos, String json) {
        int start = pos[0];
        boolean hasDecimal = false;
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if (c == '.') hasDecimal = true;
            if (Character.isDigit(c) || c == '-' || c == '.' || c == 'e' || c == 'E' || c == '+') {
                pos[0]++;
            } else {
                break;
            }
        }
        String num = json.substring(start, pos[0]);
        if (hasDecimal) return Double.parseDouble(num);
        long val = Long.parseLong(num);
        if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return (int) val;
        return val;
    }

    private static boolean parseBoolean(int[] pos, String json) {
        if (json.startsWith("true", pos[0])) { pos[0] += 4; return true; }
        pos[0] += 5;
        return false;
    }

    private static void skipWhitespace(int[] pos, String json) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) pos[0]++;
    }

    private static void expect(int[] pos, String json, char expected) {
        if (pos[0] < json.length() && json.charAt(pos[0]) == expected) pos[0]++;
    }

    // ── Card helpers ─────────────────────────────────────────────────

    public static Map<String, Object> cardToMap(Card c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rank", c.getRank().name());
        m.put("suit", c.getSuit().name());
        return m;
    }

    public static Card parseCard(Map<String, Object> map) {
        return new Card(
            Rank.valueOf((String) map.get("rank")),
            Suit.valueOf((String) map.get("suit"))
        );
    }

    public static List<Object> cardsToList(List<Card> cards) {
        List<Object> list = new ArrayList<>();
        for (Card c : cards) list.add(cardToMap(c));
        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<Card> parseCardList(List<Object> list) {
        List<Card> cards = new ArrayList<>();
        for (Object o : list) cards.add(parseCard((Map<String, Object>) o));
        return cards;
    }

    public static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    public static int getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    public static boolean getBool(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List) return (List<Object>) v;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Map) return (Map<String, Object>) v;
        return Collections.emptyMap();
    }

    public static Map<String, Object> msg(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        return m;
    }
}
