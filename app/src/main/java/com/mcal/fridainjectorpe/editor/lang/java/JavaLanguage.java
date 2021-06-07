package com.mcal.fridainjectorpe.editor.lang.java;

import com.mcal.fridainjectorpe.editor.lang.Language;

public class JavaLanguage extends Language {

    private final static String[] keywords = {
            "void", "boolean", "byte", "char", "short", "int", "long", "float", "double", "strictfp",
            "import", "package", "new", "class", "interface", "extends", "implements", "enum",
            "public", "private", "protected", "static", "abstract", "final", "native", "volatile",
            "assert", "try", "throw", "throws", "catch", "finally", "instanceof", "super", "this",
            "if", "else", "for", "do", "while", "switch", "case", "default",
            "continue", "break", "return", "synchronized", "transient",
            "true", "false", "null"
    };
    private static JavaLanguage _theOne = null;


    private JavaLanguage() {
        super.setKeywords(keywords);
    }

    public static JavaLanguage getInstance() {
        if (_theOne == null) {
            _theOne = new JavaLanguage();
        }
        return _theOne;
    }

    @Override
    public JavaTokenizer getTokenizer() {
        return JavaTokenizer.getInstance();
    }

    @Override
    public JavaFormatter getFormatter() {
        return JavaFormatter.getInstance();
    }

    /**
     * Java has no preprocessors. Override base class implementation
     */
    public boolean isLineAStart(char c) {
        return false;
    }
}
