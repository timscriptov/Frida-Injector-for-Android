package com.mcal.fridainjectorpe.editor.common;

public interface OnTextChangeListener {
    void onNewLine(String s, int caretPosition, int pos);

    void onDel(CharSequence text, int cursorPosition, int delCount);

    void onAdd(CharSequence text, int cursorPosition, int addCount);
}