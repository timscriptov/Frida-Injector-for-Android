package com.mcal.fridainjectorpe.editor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;

import com.mcal.fridainjectorpe.App;
import com.mcal.fridainjectorpe.editor.common.OnKeyShortcutListener;
import com.mcal.fridainjectorpe.editor.lang.Language;
import com.mcal.fridainjectorpe.editor.lang.lua.LuaLanguage;
import com.mcal.fridainjectorpe.editor.util.Document;
import com.mcal.fridainjectorpe.editor.util.DocumentProvider;
import com.mcal.fridainjectorpe.editor.util.Lexer;
import com.mcal.fridainjectorpe.editor.view.YoyoNavigationMethod;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class TextEditor extends TextEditorField {

    private Context mContext;
    private boolean isWordWrap = false;
    private int mCaretIndex = 0;
    @SuppressLint("SdCardPath")
    private File fontDir = new File("/sdcard/android/fonts/");

    public TextEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public TextEditor(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public TextEditor(Context context) {
        super(context);
        init(context);
    }

    private void init(@NotNull Context context) {
        mContext = context;
        initFont(fontDir);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float size = TypedValue.applyDimension(2, BASE_TEXT_SIZE_PIXELS, dm);
        setTextSize((int) size);
        setShowLineNumbers(true);
        setHighlightCurrentRow(true);
        setTypeface(Typeface.MONOSPACE);
        //自动换行
        setWordWrap(App.preferences.getBoolean("wordwrap", false));
        setAutoIndentWidth(4);
        setLanguage(LuaLanguage.getInstance());
        setNavigationMethod(new YoyoNavigationMethod(this));
    }


    public void initFont(File fontDir) {
        setTypeface(Typeface.MONOSPACE);
        File df = new File(fontDir + "default.ttf");
        if (df.exists())
            setTypeface(Typeface.createFromFile(df));
        File bf = new File(fontDir + "bold.ttf");
        if (bf.exists())
            setBoldTypeface(Typeface.createFromFile(bf));
        File tf = new File(fontDir + "italic.ttf");
        if (tf.exists())
            setItalicTypeface(Typeface.createFromFile(tf));
    }


    private OnKeyShortcutListener mKeyShortcutListener = (keyCode, event) -> {
        final int filteredMetaState = event.getMetaState() & ~KeyEvent.META_CTRL_MASK;
        if (KeyEvent.metaStateHasNoModifiers(filteredMetaState)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    selectAll();
                    return true;
                case KeyEvent.KEYCODE_X:
                    cut();
                    return true;
                case KeyEvent.KEYCODE_C:
                    copy();
                    return true;
                case KeyEvent.KEYCODE_V:
                    paste();
                    return true;
            }
        }
        return false;
    };

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mCaretIndex != 0 && right > 0) {
            moveCaret(mCaretIndex);
            mCaretIndex = 0;
        }
    }

    public void addNames(@NotNull String[] names) {
        Language lang = Lexer.getLanguage();
        String[] old = lang.getNames();
        String[] news = new String[old.length + names.length];
        System.arraycopy(old, 0, news, 0, old.length);
        System.arraycopy(names, 0, news, old.length, names.length);
        lang.setNames(news);
        //Lexer.setLanguage(lang);
        //setTabSpaces(4);
        respan();
        invalidate();
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return mKeyShortcutListener.onKeyShortcut(keyCode, event)
                || super.onKeyShortcut(keyCode, event);
    }

    public void setOnKeyShortcutListener(OnKeyShortcutListener l) {
        mKeyShortcutListener = l;
    }

    //自动换行
    @Override
    public void setWordWrap(boolean enable) {
        isWordWrap = enable;
        super.setWordWrap(enable);
    }

    public void insert(int idx, String text) {
        selectText(false);
        moveCaret(idx);
        paste(text);
        //mDocProvide.insert(idx,text);
    }

    public DocumentProvider getText() {
        return createDocumentProvider();
    }

    public String getString() {
        return getText().toString();
    }

    public void setText(CharSequence c) {
        //TextBuffer text = new TextBuffer();
        Document doc = new Document(this);
        doc.setWordWrap(isWordWrap);
        doc.setText(c);
        setDocumentProvider(new DocumentProvider(doc));
        //doc.analyzeWordWrap();
    }

    public void setText(@NotNull CharSequence c, boolean isRep) {
        replaceText(0, getLength() - 1, c.toString());
    }

    public String getSelectedText() {
        return hDoc.subSequence(getSelectionStart(), getSelectionEnd() - getSelectionStart()).toString();
    }

    public void setSelection(int index) {
        selectText(false);
        if (!hasLayout())
            moveCaret(index);
        else
            mCaretIndex = index;
    }

    public void gotoLine(int line) {
        if (line > hDoc.getRowCount()) {
            line = hDoc.getRowCount();
        }
        int i = getText().getLineOffset(line - 1);
        setSelection(i);
    }

    public void undo() {
        DocumentProvider doc = createDocumentProvider();
        int newPosition = doc.undo();
        if (newPosition >= 0) {
            setEdited(true);
            respan();
            selectText(false);
            moveCaret(newPosition);
            invalidate();
        }
    }

    public void redo() {
        DocumentProvider doc = createDocumentProvider();
        int newPosition = doc.redo();
        if (newPosition >= 0) {
            setEdited(true);
            respan();
            selectText(false);
            moveCaret(newPosition);
            invalidate();
        }
    }
}