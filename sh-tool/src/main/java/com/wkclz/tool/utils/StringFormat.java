package com.wkclz.tool.utils;

/**
 * 简单的字符串格式化工具，使用 {} 作为占位符，按顺序替换。
 * 示例：
 *   StringFormat.format("Hello, {}! You have {} messages.", "Alice", 5)
 *   => "Hello, Alice! You have 5 messages."
 */
public final class StringFormat {

    private StringFormat() {
        // 工具类，禁止实例化
    }

    /**
     * 使用 {} 作为占位符，按顺序替换为提供的参数。
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


    public static void main(String[] args) {
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
    }
    
}