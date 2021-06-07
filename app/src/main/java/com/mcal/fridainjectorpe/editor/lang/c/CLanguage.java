package com.mcal.fridainjectorpe.editor.lang.c;

import com.mcal.fridainjectorpe.editor.lang.Language;

/**
 * Singleton class containing the symbols and operators of the C language
 */
public class CLanguage extends Language {
    private final static String[] keywords = {
            "char", "double", "float", "int", "long", "short", "void",
            "auto", "const", "extern", "register", "static", "volatile",
            "signed", "unsigned", "sizeof", "typedef",
            "enum", "struct", "union",
            "break", "case", "continue", "default", "do", "else", "for",
            "goto", "if", "return", "switch", "while"
    };
    private static Language _theOne = null;

    private CLanguage() {
        super.setKeywords(keywords);
    }

    public static Language getInstance() {
        if (_theOne == null) {
            _theOne = new CLanguage();
        }
        return _theOne;
    }
}