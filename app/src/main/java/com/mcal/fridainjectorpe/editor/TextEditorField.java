package com.mcal.fridainjectorpe.editor;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import com.mcal.fridainjectorpe.editor.lang.Language;
import com.mcal.fridainjectorpe.editor.util.DocumentProvider;
import com.mcal.fridainjectorpe.editor.util.Lexer;
import com.mcal.fridainjectorpe.editor.view.AutoCompletePanel;
import com.mcal.fridainjectorpe.editor.view.FreeScrollingTextField;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public abstract class TextEditorField extends FreeScrollingTextField {

    public TextEditorField(Context context) {
        super(context);
    }

    public TextEditorField(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextEditorField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setLanguage(Language lan) {
        Lexer.setLanguage(lan);
        AutoCompletePanel.setLanguage(lan);
    }

    public void format() {
        selectText(false);
        CharSequence text = "";//Lexer.getFormatter().format(new DocumentProvider(mDocProvide), mAutoIndentWidth);
        hDoc.beginBatchEdit();
        hDoc.deleteAt(0, hDoc.docLength() - 1, System.nanoTime());
        hDoc.insertBefore(text.toString().toCharArray(), 0, System.nanoTime());
        hDoc.endBatchEdit();
        hDoc.clearSpans();
        respan();
        invalidate();
    }

    public DocumentProvider getDocumentProvider() {
        return hDoc;
    }//to do

    public TextFieldUiState getUiState() {
        return new TextFieldUiState(this);
    }

    public void restoreUiState(Parcelable state) {
        TextFieldUiState uiState = (TextFieldUiState) state;
        if (uiState.doc != null)
            setDocumentProvider(uiState.doc);
        final int caretPosition = uiState.caretPosition;
        setScrollX(uiState.scrollX);
        setScrollY(uiState.scrollY);
        // If the text field is in the process of being created, it may not
        // have its width and height set yet.
        // Therefore, post UI restoration tasks to run later.
        if (uiState.selectMode) {
            final int selStart = uiState.selectBegin;
            final int selEnd = uiState.selectEnd;

            post(() -> {
                setSelectionRange(selStart, selEnd - selStart);
                if (caretPosition < selEnd) {
                    focusSelectionStart(); //caret at the end by default
                }
            });
        } else {
            post(() -> moveCaret(caretPosition));
        }
    }

    //*********************************************************************
    //**************** UI State for saving and restoring ******************
    //*********************************************************************
    public static class TextFieldUiState implements Parcelable {
        public static final Parcelable.Creator<TextFieldUiState>
                CREATOR = new Parcelable.Creator<TextFieldUiState>() {
            @NotNull
            @Contract("_ -> new")
            @Override
            public TextFieldUiState createFromParcel(Parcel in) {
                return new TextFieldUiState(in);
            }

            @NotNull
            @Contract(value = "_ -> new", pure = true)
            @Override
            public TextFieldUiState[] newArray(int size) {
                return new TextFieldUiState[size];
            }
        };
        final int caretPosition;
        final int scrollX, scrollY;
        final boolean selectMode;
        final int selectBegin, selectEnd;
        DocumentProvider doc;

        public TextFieldUiState(@NotNull TextEditorField textField) {
            caretPosition = textField.getCaretPosition();
            scrollX = textField.getScrollX();
            scrollY = textField.getScrollY();
            selectMode = textField.isSelectText();
            selectBegin = textField.getSelectionStart();
            selectEnd = textField.getSelectionEnd();
            doc = textField.getDocumentProvider();
        }

        private TextFieldUiState(@NotNull Parcel in) {
            caretPosition = in.readInt();
            scrollX = in.readInt();
            scrollY = in.readInt();
            selectMode = in.readInt() != 0;
            selectBegin = in.readInt();
            selectEnd = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NotNull Parcel out, int flags) {
            out.writeInt(caretPosition);
            out.writeInt(scrollX);
            out.writeInt(scrollY);
            out.writeInt(selectMode ? 1 : 0);
            out.writeInt(selectBegin);
            out.writeInt(selectEnd);
        }
    }
}