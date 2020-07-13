package com.mcal.fridainjectorpe.editor.lang;

public interface Formatter {
    public int createAutoIndent(CharSequence text);

    public CharSequence format(CharSequence text, int width);
}