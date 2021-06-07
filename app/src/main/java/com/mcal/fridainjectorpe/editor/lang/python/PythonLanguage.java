package com.mcal.fridainjectorpe.editor.lang.python;

import com.mcal.fridainjectorpe.editor.lang.Language;

public class PythonLanguage extends Language {
    private final static String[] keywords = {
            "and", "assert", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "exec", "finally", "for", "from",
            "global", "if", "import", "in", "is", "lambda", "not", "or",
            "pass", "print", "raise", "return", "try", "while", "with",
            "yield", "True", "False", "None"
    };
    private final static char[] operators = {
            '(', ')', '{', '}', '.', ',', ';', '=', '+', '-',
            '/', '*', '&', '!', '|', ':', '[', ']', '<', '>',
            '~', '%', '^'
    }; // no ternary operator ? :
    private static PythonLanguage _theOne = null;


    private PythonLanguage() {
        super.setKeywords(keywords);
        super.setOperators(operators);
    }

    public static PythonLanguage getInstance() {
        if (_theOne == null) {
            _theOne = new PythonLanguage();
        }
        return _theOne;
    }

    @Override
    public boolean isWordStart(char c) {
        return (c == '@');
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
