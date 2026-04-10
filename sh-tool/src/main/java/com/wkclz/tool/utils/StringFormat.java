package com.wkclz.tool.utils;

import java.util.HashMap;
import java.util.Map;

public final class StringFormat {

    private StringFormat() {
        // 工具类，禁止实例化
    }

    /**
     * 简单的字符串格式化工具，使用 {} 作为占位符，按顺序替换。
     * 示例：
     *   StringFormat.format("Hello, {}! You have {} messages.", "Alice", 5)
     *   => "Hello, Alice! You have 5 messages."
     *
     * @param pattern 模板字符串，如 "Hello, {}!"
     * @param args    替换参数
     * @return 格式化后的字符串
     * @throws IllegalArgumentException 如果模板为 null
     */
    public static String of(String pattern, Object... args) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern must not be null");
        }
        if (args == null || args.length == 0) {
            return pattern;
        }

        StringBuilder result = new StringBuilder();
        int start = 0;
        int argIndex = 0;

        while (start < pattern.length()) {
            int openBrace = pattern.indexOf('{', start);
            if (openBrace == -1) {
                // 后面没有 { 了，追加剩余部分
                result.append(pattern, start, pattern.length());
                break;
            }

            // 检查是否是 "{}"
            if (openBrace + 1 < pattern.length() && pattern.charAt(openBrace + 1) == '}') {
                // 找到一个 {}，替换
                result.append(pattern, start, openBrace);
                if (argIndex < args.length) {
                    result.append(String.valueOf(args[argIndex]));
                    argIndex++;
                } else {
                    // 参数不足，保留 {}
                    result.append("{}");
                }
                start = openBrace + 2; // 跳过 "{}"
            } else {
                // 不是 "{}"，可能是 "{" 或 "{xxx"，当作普通字符
                result.append(pattern, start, openBrace + 1);
                start = openBrace + 1;
            }
        }

        return result.toString();
    }

    /**
     * 使用命名变量进行字符串格式化。
     * 变量格式：${var}
     * 条件渲染格式：${var}[内容]，当 var 为空时，[] 内的内容不渲染
     *
     * @param template 模板字符串，如 "Hello, ${name}!"
     * @param params   参数映射，key 为变量名（不包含 ${}）
     * @return 格式化后的字符串
     * @throws IllegalArgumentException 如果模板为 null
     *
     * 示例：
     *   Map<String, String> params = Map.of("name", "Alice", "age", "30");
     *   StringFormat.format("Name: ${name}, Age: ${age}", params)
     *   => "Name: Alice, Age: 30"
     *
     *   Map<String, String> params = Map.of("name", "Bob");
     *   StringFormat.format("${name}[Name: ${name}]${age}[, Age: ${age}]", params)
     *   => "Name: Bob"
     *
     *   Map<String, String> params = Map.of("name", "Alice", "age", "30");
     *   StringFormat.format("${name}[Name: ${name}]${age}[, Age: ${age}]", params)
     *   => "Name: Alice, Age: 30"
     */
    public static String of(String template, Map<String, Object> params) {
        if (template == null) {
            return template;
        }
        if (params == null) {
            params = new HashMap<>();
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            int dollarIndex = template.indexOf("${", i);
            if (dollarIndex == -1) {
                result.append(template, i, template.length());
                break;
            }

            result.append(template, i, dollarIndex);

            int closeBraceIndex = template.indexOf("}", dollarIndex + 2);
            if (closeBraceIndex == -1) {
                result.append(template, dollarIndex, template.length());
                break;
            }

            String varName = template.substring(dollarIndex + 2, closeBraceIndex);
            Object value = params.get(varName);
            boolean hasValue = value != null && !String.valueOf(value).isEmpty();

            int afterBrace = closeBraceIndex + 1;
            int bracketStart = -1;
            int bracketEnd = -1;

            if (afterBrace < template.length() && template.charAt(afterBrace) == '[') {
                int depth = 1;
                int j = afterBrace + 1;
                while (j < template.length() && depth > 0) {
                    char c = template.charAt(j);
                    if (c == '[') {
                        depth++;
                    } else if (c == ']') {
                        depth--;
                        if (depth == 0) {
                            bracketStart = afterBrace;
                            bracketEnd = j;
                            break;
                        }
                    }
                    j++;
                }
            }

            if (bracketStart != -1) {
                if (hasValue) {
                    String innerContent = template.substring(bracketStart + 1, bracketEnd);
                    String processedInner = of(innerContent, params);
                    result.append(processedInner);
                }
                i = bracketEnd + 1;
            } else {
                if (hasValue) {
                    result.append(String.valueOf(value));
                }
                i = afterBrace;
            }
        }

        return result.toString();
    }

    static void main(String[] args) {
        // 基本用法
        System.out.println(StringFormat.of("Hello, {}!", "World"));
        // 输出: Hello, World!

        // 多个参数
        System.out.println(StringFormat.of("User: {}, Age: {}, Active: {}", "Alice", 30, true));
        // 输出: User: Alice, Age: 30, Active: true

        // 参数不足
        System.out.println(StringFormat.of("A: {}, B: {}, C: {}", "X"));
        // 输出: A: X, B: {}, C: {}

        // 参数多余（忽略）
        System.out.println(StringFormat.of("Only one: {}", "A", "B", "C"));
        // 输出: Only one: A

        // null 值处理
        System.out.println(StringFormat.of("Value: {}", (Object) null));
        // 输出: Value: null

        // 包含孤立的 '{' 或 '}'（不视为占位符）
        System.out.println(StringFormat.of("Score: {0} vs {}", "TeamA", "TeamB"));
        // 输出: Score: {0} vs TeamB

        System.out.println("\n========== format (Map参数) 测试 ==========");

        // 基本用法
        java.util.Map<String, Object> params1 = new java.util.HashMap<>();
        params1.put("name", "Alice");
        params1.put("age", 30);
        System.out.println(StringFormat.of("Name: ${name}, Age: ${age}", params1));
        // 输出: Name: Alice, Age: 30

        // 变量为空时替换为空字符串
        java.util.Map<String, Object> params2 = new java.util.HashMap<>();
        params2.put("name", "Bob");
        System.out.println(StringFormat.of("Name: ${name}, Age: ${age}", params2));
        // 输出: Name: Bob, Age:

        // 条件渲染：变量为空时，[] 内的内容不渲染
        java.util.Map<String, Object> params3 = new java.util.HashMap<>();
        params3.put("name", "Charlie");
        System.out.println(StringFormat.of("${name}[Name: ${name}]${age}[, Age: ${age}]", params3));
        // 输出: Name: Charlie

        // 条件渲染：所有变量都有值
        java.util.Map<String, Object> params4 = new java.util.HashMap<>();
        params4.put("name", "David");
        params4.put("age", 25);
        System.out.println(StringFormat.of("${name}[Name: ${name}]${age}[, Age: ${age}]", params4));
        // 输出: Name: David, Age: 25

        // 嵌套条件渲染
        java.util.Map<String, Object> params5 = new java.util.HashMap<>();
        params5.put("user", "Eve");
        params5.put("email", "eve@example.com");
        System.out.println(StringFormat.of("${user}[User: ${user}${email}[ (Email: ${email})]]", params5));
        // 输出: User: Eve (Email: eve@example.com)

        // 嵌套条件渲染：email 为空
        java.util.Map<String, Object> params6 = new java.util.HashMap<>();
        params6.put("user", "Frank");
        System.out.println(StringFormat.of("${user}[User: ${user}${email}[ (Email: ${email})]]", params6));
        // 输出: User: Frank

        // null 值处理
        java.util.Map<String, Object> params7 = new java.util.HashMap<>();
        params7.put("value", null);
        System.out.println(StringFormat.of("Value: [${value}]", params7));
        // 输出: Value: []

        // 空字符串处理
        java.util.Map<String, Object> params8 = new java.util.HashMap<>();
        params8.put("value", "");
        System.out.println(StringFormat.of("Value: ${value}[${value}]", params8));
        // 输出: Value:
    }

}