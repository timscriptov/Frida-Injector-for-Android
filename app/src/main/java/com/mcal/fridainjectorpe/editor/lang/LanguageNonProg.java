package com.mcal.fridainjectorpe.editor.lang;

/**
 * 无编程语言类
 */


public class LanguageNonProg extends Language {
    private final static String[] keywords = {};
    private final static char[] operators = {};
    private static LanguageNonProg _theOne = null;

    private LanguageNonProg() {
        super.setKeywords(keywords);
        super.setOperators(operators);
    }

    public static LanguageNonProg getInstance() {
        if (_theOne == null) {
            _theOne = new LanguageNonProg();
        }
        return _theOne;
    }

    @Override
    public boolean isProgLang() {
        return false;
    }

    @Override
    public boolean isEscapeChar(char c) {
        return false;
    }

    @Override
    public boolean isDelimiterA(char c) {
        return false;
    }

    @Override
    public boolean isDelimiterB(char c) {
        return false;
    }

    @Override
    public boolean isLineAStart(char c) {
        return false;
    }

    @Override
    public boolean isLineStart(char c0, char c1) {
        return false;
    }

    @Override
    public boolean isMultilineStartDelimiter(char c0, char c1) {
        return false;
    }

    @Override
    public boolean isMultilineEndDelimiter(char c0, char c1) {
        return false;
    }
}