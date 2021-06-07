package com.mcal.fridainjectorpe.editor.lang.ruby;

import com.mcal.fridainjectorpe.editor.lang.Language;

public class RubyLanguage extends Language {
    private final static String[] keywords = {
            "alias", "and", "BEGIN", "begin", "break", "case", "catch", "class", "def",
            "defined?", "do", "else", "elsif", "END", "end", "ensure", "false",
            "for", "if", "in", "module", "next", "nil", "not", "or", "public",
            "private", "protected", "raise", "redo", "rescue", "retry", "return", "self",
            "super", "then", "throw", "true", "undef", "unless", "until", "when", "while",
            "yield", "self", "nil", "true", "false", "TRUE", "FALSE", "NIL"
    };
    private static RubyLanguage _theOne = null;


    private RubyLanguage() {
        super.setKeywords(keywords);
    }

    public static RubyLanguage getInstance() {
        if (_theOne == null) {
            _theOne = new RubyLanguage();
        }
        return _theOne;
    }

    @Override
    public boolean isWordStart(char c) {
        return (c == '$');
    }

    @Override
    public boolean isLineAStart(char c) {
        return false;
    }

    @Override
    public boolean isLineBStart(char c) {
        return (c == '#');
    }

    @Override
    public boolean isLineStart(char c0, char c1) {
        return false;
    }

    @Override
    public boolean isMultilineStartDelimiter(char c0, char c1) {
        return false;
    }
}
