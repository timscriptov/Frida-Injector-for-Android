package com.mcal.fridainjectorpe.editor.lang.javascript;

import com.mcal.fridainjectorpe.editor.lang.Language;
import com.mcal.fridainjectorpe.editor.lang.lua.LuaFormatter;
import com.mcal.fridainjectorpe.editor.lang.lua.LuaTokenizer;

public class JavaScriptLanguage extends Language {
    private final static String[] keywords = {
            "abstract", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "debugger", "default", "delete", "do",
            "double", "else", "enum", "export", "extends", "false", "final",
            "finally", "float", "for", "function", "goto", "if", "implements",
            "import", "in", "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected", "public", "return",
            "short", "static", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "true", "try", "typeof", "var", "void",
            "volatile", "while", "with"
    };

    private final static char[] JAVASCRIPT_OPERATORS = {
            '(', ')', '{', '}', ',', ';', '=', '+', '-',
            '/', '*', '&', '!', '|', ':', '[', ']', '<', '>',
            '?', '~', '%', '^'
    };

    private static JavaScriptLanguage _theOne = null;

    private JavaScriptLanguage() {
        super.setOperators(JAVASCRIPT_OPERATORS);
        super.setKeywords(keywords);
        super.setNames(keywords);
    }

    public static JavaScriptLanguage getInstance() {
        if (_theOne == null) {
            _theOne = new JavaScriptLanguage();
        }
        return _theOne;
    }

    @Override
    public LuaTokenizer getTokenizer() {
        return LuaTokenizer.getInstance();
    }

    @Override
    public LuaFormatter getFormatter() {
        return LuaFormatter.getInstance();
    }

    public boolean isLineAStart(char c) {
        return false;
    }
}
