/*
 *****************************************************************************
 *
 * --------------------------------- row length
 * Hello World(\n)                 | 12
 * This is a test of the caret(\n) | 28
 * func|t|ions(\n)                 | 10
 * of this program(EOF)            | 16
 * ---------------------------------
 *
 * The figure illustrates the convention for counting characters.
 * Rows 36 to 39 of a hypothetical text file are shown.
 * The 0th char of the file is off-screen.
 * Assume the first char on screen is the 257th char.
 * The caret is before the char 't' of the word "functions". The caret is drawn
 * as a filled blue rectangle enclosing the 't'.
 *
 * mCaretPosition == 257 + 12 + 28 + 4 == 301
 *
 * Note 1: EOF (End Of File) is a real char with a length of 1
 * Note 2: Characters enclosed in parentheses are non-printable
 *
 *****************************************************************************
 *
 * There is a difference between rows and lines in TextWarrior.
 * Rows are displayed while lines are a pure logical construct.
 * When there is no word-wrap, a line of text is displayed as a row on screen.
 * With word-wrap, a very long line of text may be split across several rows
 * on screen.
 *
 */
package com.mcal.fridainjectorpe.editor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.ClipboardManager;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.CharacterPickerDialog;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;

import com.mcal.fridainjectorpe.editor.common.OnCaretScrollListener;
import com.mcal.fridainjectorpe.editor.common.OnRowChangedListener;
import com.mcal.fridainjectorpe.editor.common.OnSelectionChangedListener;
import com.mcal.fridainjectorpe.editor.common.OnTextChangeListener;
import com.mcal.fridainjectorpe.editor.lang.Language;
import com.mcal.fridainjectorpe.editor.util.Document;
import com.mcal.fridainjectorpe.editor.util.DocumentProvider;
import com.mcal.fridainjectorpe.editor.util.Lexer;
import com.mcal.fridainjectorpe.editor.util.Pair;
import com.mcal.fridainjectorpe.editor.util.TextWarriorException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * A custom text view that uses a solid shaded caret (aka cursor) instead of a
 * blinking caret and allows a variety of navigation methods to be easily
 * integrated.
 * <p>
 * It also has a built-in syntax highlighting feature. The global programming
 * language syntax to use is specified with Lexer.setLanguage(Language).
 * To disable syntax highlighting, simply pass LanguageNonProg to that function.
 * <p>
 * Responsibilities
 * 1. Display text
 * 2. Display padding
 * 3. Scrolling
 * 4. Store and display caret position and selection range
 * 5. Store font type, font size, and tab length
 * 6. Interpret non-touch input events and shortcut keystrokes, triggering
 * the appropriate inner class controller actions
 * 7. Reset view, set cursor position and selection range
 * <p>
 * Inner class controller responsibilities
 * 1. Caret movement
 * 2. Activate/deactivate selection mode
 * 3. Cut, copy, paste, delete, insert
 * 4. Schedule areas to repaint and analyze for spans in response to edits
 * 5. Directs scrolling if caret movements or edits causes the caret to be off-screen
 * 6. Notify rowListeners when caret row changes
 * 7. Provide helper methods for InputConnection to setComposingText from the IME
 * <p>
 * This class is aware that the underlying text buffer uses an extra char (EOF)
 * to mark the end of the text. The text size reported by the text buffer includes
 * this extra char. Some bounds manipulation is done so that this implementation
 * detail is hidden from client classes.
 */
public abstract class FreeScrollingTextField extends View implements Document.TextFieldMetrics {

    //---------------------------------------------------------------------
    //--------------------------  Caret Scroll  ---------------------------
    public final static int SCROLL_UP = 0;
    public final static int SCROLL_DOWN = 1;
    public final static int SCROLL_LEFT = 2;
    public final static int SCROLL_RIGHT = 3;
    /**
     * Scale factor for the width of a caret when on a NEWLINE or EOF char.
     * A factor of 1.0 is equals to the width of a space character
     */
    protected static float EMPTY_CARET_WIDTH_SCALE = 0.75f;
    /**
     * When in selection mode, the caret height is scaled by this factor
     */
    protected static float SEL_CARET_HEIGHT_SCALE = 0.5f;
    protected static int DEFAULT_TAB_LENGTH_SPACES = 4;
    protected static int BASE_TEXT_SIZE_PIXELS = 16;
    protected static long SCROLL_PERIOD = 250; //in milliseconds
    protected static int SCROLL_EDGE_SLOP = 150;
    /*
     * Hash map for determining which characters to let the user choose from when
     * a hardware key is long-pressed. For example, long-pressing "e" displays
     * choices of "é, è, ê, ë" and so on.
     * This is biased towards European locales, but is standard Android behavior
     * for TextView.
     *
     * Copied from android.text.method.QwertyKeyListener, dated 2006
     */
    /*
     * Hash map for determining which characters to let the user choose from when
     * a hardware key is long-pressed. For example, long-pressing "e" displays
     * choices of "é, è, ê, ë" and so on.
     * This is biased towards European locales, but is standard Android behavior
     * for TextView.
     *
     * Copied from android.text.method.QwertyKeyListener, dated 2006
     */
    private static SparseArray<String> PICKER_SETS = new SparseArray<String>();

    static {
        PICKER_SETS.put('A', "\u00C0\u00C1\u00C2\u00C4\u00C6\u00C3\u00C5\u0104\u0100");
        PICKER_SETS.put('C', "\u00C7\u0106\u010C");
        PICKER_SETS.put('D', "\u010E");
        PICKER_SETS.put('E', "\u00C8\u00C9\u00CA\u00CB\u0118\u011A\u0112");
        PICKER_SETS.put('G', "\u011E");
        PICKER_SETS.put('L', "\u0141");
        PICKER_SETS.put('I', "\u00CC\u00CD\u00CE\u00CF\u012A\u0130");
        PICKER_SETS.put('N', "\u00D1\u0143\u0147");
        PICKER_SETS.put('O', "\u00D8\u0152\u00D5\u00D2\u00D3\u00D4\u00D6\u014C");
        PICKER_SETS.put('R', "\u0158");
        PICKER_SETS.put('S', "\u015A\u0160\u015E");
        PICKER_SETS.put('T', "\u0164");
        PICKER_SETS.put('U', "\u00D9\u00DA\u00DB\u00DC\u016E\u016A");
        PICKER_SETS.put('Y', "\u00DD\u0178");
        PICKER_SETS.put('Z', "\u0179\u017B\u017D");
        PICKER_SETS.put('a', "\u00E0\u00E1\u00E2\u00E4\u00E6\u00E3\u00E5\u0105\u0101");
        PICKER_SETS.put('c', "\u00E7\u0107\u010D");
        PICKER_SETS.put('d', "\u010F");
        PICKER_SETS.put('e', "\u00E8\u00E9\u00EA\u00EB\u0119\u011B\u0113");
        PICKER_SETS.put('g', "\u011F");
        PICKER_SETS.put('i', "\u00EC\u00ED\u00EE\u00EF\u012B\u0131");
        PICKER_SETS.put('l', "\u0142");
        PICKER_SETS.put('n', "\u00F1\u0144\u0148");
        PICKER_SETS.put('o', "\u00F8\u0153\u00F5\u00F2\u00F3\u00F4\u00F6\u014D");
        PICKER_SETS.put('r', "\u0159");
        PICKER_SETS.put('s', "\u00A7\u00DF\u015B\u0161\u015F");
        PICKER_SETS.put('t', "\u0165");
        PICKER_SETS.put('u', "\u00F9\u00FA\u00FB\u00FC\u016F\u016B");
        PICKER_SETS.put('y', "\u00FD\u00FF");
        PICKER_SETS.put('z', "\u017A\u017C\u017E");
        PICKER_SETS.put(KeyCharacterMap.PICKER_DIALOG_INPUT,
                "\u2026\u00A5\u2022\u00AE\u00A9\u00B1[]{}\\|");
        PICKER_SETS.put('/', "\\");

        // From packages/inputmethods/LatinIME/res/xml/kbd_symbols.xml

        PICKER_SETS.put('1', "\u00b9\u00bd\u2153\u00bc\u215b");
        PICKER_SETS.put('2', "\u00b2\u2154");
        PICKER_SETS.put('3', "\u00b3\u00be\u215c");
        PICKER_SETS.put('4', "\u2074");
        PICKER_SETS.put('5', "\u215d");
        PICKER_SETS.put('7', "\u215e");
        PICKER_SETS.put('0', "\u207f\u2205");
        PICKER_SETS.put('$', "\u00a2\u00a3\u20ac\u00a5\u20a3\u20a4\u20b1");
        PICKER_SETS.put('%', "\u2030");
        PICKER_SETS.put('*', "\u2020\u2021");
        PICKER_SETS.put('-', "\u2013\u2014");
        PICKER_SETS.put('+', "\u00b1");
        PICKER_SETS.put('(', "[{<");
        PICKER_SETS.put(')', "]}>");
        PICKER_SETS.put('!', "\u00a1");
        PICKER_SETS.put('"', "\u201c\u201d\u00ab\u00bb\u02dd");
        PICKER_SETS.put('?', "\u00bf");
        PICKER_SETS.put(',', "\u201a\u201e");

        // From packages/inputmethods/LatinIME/res/xml/kbd_symbols_shift.xml

        PICKER_SETS.put('=', "\u2260\u2248\u221e");
        PICKER_SETS.put('<', "\u2264\u00ab\u2039");
        PICKER_SETS.put('>', "\u2265\u00bb\u203a");
    }

    //光标宽度
    public final int mCursorWidth = 5;
    protected boolean isEdited = false; // whether the text field is dirtied
    protected TouchNavigationMethod mNavMethod;
    protected DocumentProvider hDoc; // the model in MVC
    protected int mCaretPosition = 0;
    protected int mSelectionAnchor = -1; // inclusive
    protected int mSelectionEdge = -1; // exclusive
    protected int mTabLength = DEFAULT_TAB_LENGTH_SPACES;
    protected ColorScheme mColorScheme;
    protected boolean isHighlightRow = true;
    protected boolean isShowNonPrinting = false;
    protected boolean isAutoIndent = true;
    protected int mAutoIndentWidth = 4;
    protected boolean isLongPressCaps = false;
    protected AutoCompletePanel mAutoCompletePanel;
    protected boolean isAutoComplete = true;
    private Scroller mScroller;
    private TextFieldController mFieldController; // the controller in MVC
    private TextFieldInputConnection mInputConnection;
    private OnTextChangeListener mTextListener;
    private OnRowChangedListener mRowListener;
    private OnSelectionChangedListener mSelModeListener;
    private OnCaretScrollListener mCaretListener;
    private int mCaretRow = 0; // can be calculated, but stored for efficiency purposes
    private Paint mTextPaint, mLineNumPaint;
    /**
     * Max amount that can be scrolled horizontally based on the longest line
     * displayed on screen so far
     */
    private int mTopOffset, mLeftOffset;
    private int mLineMaxWidth, xExtent;
    private int mAlphaWidth, mSpaceWidth;
    private long mLastScroll;
    private boolean isAutoCompeted = true; //代码提示
    private boolean isShowRegion = true;
    private boolean isShowLineNumbers = true;
    private boolean isCursorVisiable = true;
    private boolean isLayout = false;
    private boolean isTextChanged = false;
    private boolean isCaretScrolled = false;
    private final Runnable mScrollCaretDownTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretDown();
            if (!caretOnLastRowOfFile()) {
                postDelayed(mScrollCaretDownTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable mScrollCaretUpTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretUp();
            if (!caretOnFirstRowOfFile()) {
                postDelayed(mScrollCaretUpTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable mScrollCaretLeftTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretLeft(false);
            if (mCaretPosition > 0 &&
                    mCaretRow == hDoc.findRowNumber(mCaretPosition - 1)) {
                postDelayed(mScrollCaretLeftTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable mScrollCaretRightTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretRight(false);
            if (!caretOnEOF() &&
                    mCaretRow == hDoc.findRowNumber(mCaretPosition + 1)) {
                postDelayed(mScrollCaretRightTask, SCROLL_PERIOD);
            }
        }
    };
    private ClipboardPanel mClipboardPanel;
    private ClipboardManager mClipboardManager;
    private float mZoomFactor = 1;
    private int mCaretX, mCaretY;
    private char mCharEmoji = '\0';
    private Pair mCaretSpan = new Pair(0, 0);
    private Typeface defTypeface = Typeface.DEFAULT;
    private Typeface boldTypeface = Typeface.DEFAULT_BOLD;
    private Typeface italicTypeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC);
    // Cursor blink animation
    private Runnable cursorAnimation = new Runnable() {

        @Override
        public void run() {
            // Switch the cursor visibility and set it
            //int newAlpha = (mTextPaint.getAlpha() == 0) ? 255 : 0;
            //mTextPaint.setAlpha(newAlpha);
            isCursorVisiable = !isCursorVisiable;
            // Call onDraw() to draw the cursor with the new Paint
            invalidate();
            // Wait 500 milliseconds before calling self again
            postDelayed(cursorAnimation, 500);
        }
    };

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */


    public FreeScrollingTextField(Context context) {
        super(context);
        initTextField(context);
    }


    public FreeScrollingTextField(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTextField(context);
    }

    public FreeScrollingTextField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initTextField(context);
    }

    public void setCaretListener(OnCaretScrollListener caretScrollListener) {
        mCaretListener = caretScrollListener;
    }

    public void startBlink() {
        removeCallbacks(cursorAnimation);
        postDelayed(cursorAnimation, 1000);
    }

    public void stopBlink() {
        removeCallbacks(cursorAnimation);
        isCursorVisiable = true;
    }

    protected void initTextField(Context context) {
        hDoc = new DocumentProvider(this);
        mNavMethod = new TouchNavigationMethod(this);
        mScroller = new Scroller(context);

        initView(context);
    }

    public int getTopOffset() {
        return mTopOffset;
    }

    public int getAutoIndentWidth() {
        return mAutoIndentWidth;
    }

    //换行空格数目
    public void setAutoIndentWidth(int autoIndentWidth) {
        mAutoIndentWidth = autoIndentWidth;
    }

    public int getCaretY() {
        return mCaretY;
    }

    public int getCaretX() {
        return mCaretX;
    }

    public void setCursorVisiable(boolean isCursorVidiable) {
        isCursorVisiable = isCursorVidiable;
    }

    public boolean isShowLineNumbers() {
        return isShowLineNumbers;
    }

    public void setShowLineNumbers(boolean showLineNumbers) {
        isShowLineNumbers = showLineNumbers;
    }

    public void setCaretScrolled(boolean scrolled) {
        isCaretScrolled = scrolled;
    }

    public boolean getTextChanged() {
        return isTextChanged;
    }

    public void setTextChanged(boolean changed) {
        isTextChanged = changed;
    }

    public int getLeftOffset() {
        return mLeftOffset;
    }

    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    public void setTextSize(int pix) {
        if (pix <= 20 || pix >= 80 || pix == mTextPaint.getTextSize()) {
            return;
        }
        double oldHeight = rowHeight();
        double oldWidth = getAdvance('a');
        mZoomFactor = pix / BASE_TEXT_SIZE_PIXELS;
        mTextPaint.setTextSize(pix);
        mLineNumPaint.setTextSize(pix);
        if (hDoc.isWordWrap())
            hDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        double x = getScrollX() * ((double) getAdvance('a') / oldWidth);
        double y = getScrollY() * ((double) rowHeight() / oldHeight);
        scrollTo((int) x, (int) y);
        mAlphaWidth = (int) mTextPaint.measureText("a");
        mSpaceWidth = (int) mTextPaint.measureText(" ");
        //int idx=coordToCharIndex(getScrollX(), getScrollY());
        //if (!makeCharVisible(idx))
        {
            invalidate();
        }
    }

    public void replaceText(int from, int charCount, String text) {
        hDoc.beginBatchEdit();
        mFieldController.replaceText(from, charCount, text);
        mFieldController.stopTextComposing();
        hDoc.endBatchEdit();
    }

    public int getLength() {
        return hDoc.docLength();
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean isSaveEnabled() {
        return true;
    }

    private void initView(Context context) {
        mFieldController = this.new TextFieldController();
        mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(BASE_TEXT_SIZE_PIXELS);
        mLineNumPaint = new Paint();
        mLineNumPaint.setAntiAlias(true);
        mLineNumPaint.setTextSize(BASE_TEXT_SIZE_PIXELS);
        //setBackgroundColor(mColorScheme.getColor(ColorScheme.Colorable.BACKGROUND));
        setLongClickable(true);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(true);

        mColorScheme = new ColorSchemeLight();

        startBlink();

        mTextListener = new OnTextChangeListener() {

            @Override
            public void onNewLine(String s, int caretPosition, int pos) {
                // TODO: Implement this method
                stopBlink();
                isTextChanged = true;
                mCaretSpan.setFirst(mCaretSpan.getFirst() + 1);
                mAutoCompletePanel.dismiss();
                startBlink();
            }

            @Override
            public void onDel(CharSequence text, int cursorPosition, int delCount) {
                // TODO: Implement this method
                stopBlink();
                isTextChanged = true;
                if (delCount <= mCaretSpan.getFirst()) {
                    mCaretSpan.setFirst(mCaretSpan.getFirst() - 1);
                }
                mAutoCompletePanel.dismiss();
                startBlink();
            }

            @Override
            public void onAdd(CharSequence text, int cursorPosition, int addCount) {
                // TODO: Implement this method
                stopBlink();
                isTextChanged = true;
                mCaretSpan.setFirst(mCaretSpan.getFirst() + addCount);
                int curr = cursorPosition;
                if (text.length() == 0) return;
                //找到空格或者其他
                for (; curr >= 0; curr--) {
                    char c = hDoc.charAt(curr - 1);
                    if (!(Character.isLetterOrDigit(c) || c == '_' || c == '.')) {
                        break;
                    }
                }
                char ch = text.charAt(0);

                if (cursorPosition - curr > 0 && Character.isLetterOrDigit(ch)) {
                    //是否开启代码提示
                    // log("subSequence:"+hDoc.subSequence(curr, caretPosition - curr));
                    if (isAutoCompeted) {
                        mAutoCompletePanel.update(hDoc.subSequence(curr, cursorPosition - curr));
                    }
                } else {
                    mAutoCompletePanel.dismiss();
                }
                startBlink();
            }
        };

        mRowListener = newRowIndex -> {
            // Do nothing
        };

        mSelModeListener = (active, selStart, selEnd) -> {
            // TODO: Implement this method
            if (active)
                mClipboardPanel.show();
            else
                mClipboardPanel.hide();
        };

        resetView();
        mClipboardPanel = new ClipboardPanel(this);
        mAutoCompletePanel = new AutoCompletePanel(this);
        //TODO find out if this function works
        //setScrollContainer(true);
        invalidate();
    }

    private void resetView() {
        mCaretPosition = mCaretRow = 0;
        xExtent = mLineMaxWidth = 0;
        mFieldController.setSelectText(false);
        mFieldController.stopTextComposing();
        hDoc.clearSpans();
        if (getContentWidth() > 0 || !hDoc.isWordWrap()) {
            hDoc.analyzeWordWrap();
        }
        mRowListener.onRowChanged(0);
        scrollTo(0, 0);
    }

    /**
     * Sets the text displayed to the document referenced by hDoc. The view
     * state is reset and the view is invalidated as a side-effect.
     */
    public void setDocumentProvider(DocumentProvider doc) {
        hDoc = doc;
        resetView();
        mFieldController.cancelSpanning(); //stop existing lex threads
        mFieldController.determineSpans();
        invalidate();
    }

    /**
     * Returns a DocumentProvider that references the same Document used by the
     * FreeScrollingTextField.
     */
    public DocumentProvider createDocumentProvider() {
        return new DocumentProvider(hDoc);
    }

    public void setRowListener(OnRowChangedListener rLis) {
        mRowListener = rLis;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener sLis) {
        mSelModeListener = sLis;
    }

    /**
     * Sets the caret navigation method used by this text field
     */
    public void setNavigationMethod(TouchNavigationMethod navMethod) {
        mNavMethod = navMethod;
    }

    //---------------------------------------------------------------------
    //-------------------------- Paint methods ----------------------------

    public void setChirality(boolean isRightHanded) {
        mNavMethod.onChiralityChanged(isRightHanded);
    }

    // this used to be isDirty(), but was renamed to avoid conflicts with Android API 11
    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean set) {
        isEdited = set;
    }

    @Override
    public InputConnection onCreateInputConnection(@NotNull EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                | EditorInfo.IME_ACTION_DONE
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        if (mInputConnection == null) {
            mInputConnection = this.new TextFieldInputConnection(this);
        } else {
            mInputConnection.resetComposingState();
        }
        return mInputConnection;
    }

    //---------------------------------------------------------------------
    //------------------------- Layout methods ----------------------------
    //TODO test with height less than 1 complete row
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                useAllDimensions(widthMeasureSpec),
                useAllDimensions(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // TODO: Implement this method
        if (changed) {
            Rect rect = new Rect();
            getWindowVisibleDisplayFrame(rect);
            mTopOffset = rect.top + rect.height() - getHeight();
            if (!isLayout)
                respan();
            isLayout = right > 0;
            invalidate();
            mAutoCompletePanel.setWidth(getWidth() / 2);
            mAutoCompletePanel.setHeight(getHeight() / 2);
            //mAutoCompletePanel.show();
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (hDoc.isWordWrap() && oldw != w)
            hDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        if (h < oldh)
            makeCharVisible(mCaretPosition);
    }

    private int useAllDimensions(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int result = MeasureSpec.getSize(measureSpec);

        if (specMode != MeasureSpec.EXACTLY && specMode != MeasureSpec.AT_MOST) {
            result = Integer.MAX_VALUE;
            TextWarriorException.fail("MeasureSpec cannot be UNSPECIFIED. Setting dimensions to max.");
        }

        return result;
    }

    protected int getNumVisibleRows() {
        return (int) Math.ceil((double) getContentHeight() / rowHeight());
    }

    protected int rowHeight() {
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        return (metrics.descent - metrics.ascent);
    }

    /*
     The only methods that have to worry about padding are invalidate, draw
	 and computeVerticalScrollRange() methods. Other methods can assume that
	 the text completely fills a rectangular viewport given by getContentWidth()
	 and getContentHeight()
	 */
    protected int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Determines if the View has been layout or is still being constructed
     */
    public boolean hasLayout() {
        return (getWidth() == 0); // simplistic implementation, but should work for most cases
    }

    /**
     * The first row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getBeginPaintRow(@NotNull Canvas canvas) {
        Rect bounds = canvas.getClipBounds();
        return bounds.top / rowHeight();
    }

    /**
     * The last row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getEndPaintRow(@NotNull Canvas canvas) {
        //clip top and left are inclusive; bottom and right are exclusive
        Rect bounds = canvas.getClipBounds();
        return (bounds.bottom - 1) / rowHeight();
    }

    /**
     * @return The x-value of the baseline for drawing text on the given row
     */
    public int getPaintBaseline(int row) {
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        return (row + 1) * rowHeight() - metrics.descent;
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {

        canvas.save();

        //translate clipping region to create padding around edges
        canvas.clipRect(getScrollX() + getPaddingLeft(),
                getScrollY() + getPaddingTop(),
                getScrollX() + getWidth() - getPaddingRight(),
                getScrollY() + getHeight() - getPaddingBottom());
        canvas.translate(getPaddingLeft(), getPaddingTop());
        realDraw(canvas);

        canvas.restore();
        mNavMethod.onTextDrawComplete(canvas);
    }

    private void realDraw(Canvas canvas) {
        //----------------------------------------------
        // initialize and set up boundaries
        //----------------------------------------------
        int currRowNum = getBeginPaintRow(canvas);

        // 每次重新进行绘制时, 对xExtent进行重新赋值
        xExtent = mLeftOffset;

        int currIndex = hDoc.getRowOffset(currRowNum);
        if (currIndex < 0) return;

        //int len = hDoc.length();
        int currLineNum = isWordWrap() ? hDoc.findLineNumber(currIndex) + 1 : currRowNum + 1;
        int lastLineNum = 0;
        if (isShowLineNumbers)
            mLeftOffset = (int) mLineNumPaint.measureText(String.valueOf(hDoc.getRowCount())) + mSpaceWidth * 3 / 2;
        int endRowNum = getEndPaintRow(canvas);


        int paintX = 0;
        int paintY = getPaintBaseline(currRowNum);

        //----------------------------------------------
        // set up initial span color
        //----------------------------------------------
        int spanIndex = 0;
        List<Pair> spans = hDoc.getSpans();

        if (spans.isEmpty()) return;
        // There must be at least one span to paint, even for an empty file,
        // where the span contains only the EOF character
        TextWarriorException.assertVerbose(!spans.isEmpty(),
                "No spans to paint in TextWarrior.paint()");

        //TODO use binary search
        Pair nextSpan = spans.get(spanIndex++);

        Pair currSpan;
        int spanOffset = 0;
        int spanSize = spans.size();
        do {
            currSpan = nextSpan;
            spanOffset += currSpan.first;
            if (spanIndex < spanSize) {
                nextSpan = spans.get(spanIndex++);
            } else {
                nextSpan = null;
            }
        } while (nextSpan != null && spanOffset <= currIndex);

        int currType = currSpan.second;
        int lastType = currType;

        switch (currSpan.second) {
            case Lexer.KEYWORD:
                mTextPaint.setTypeface(boldTypeface);
                break;
            case Lexer.DOUBLE_SYMBOL_LINE:
                mTextPaint.setTypeface(italicTypeface);
                break;
            default:
                mTextPaint.setTypeface(defTypeface);
        }
        int spanColor = mColorScheme.getTokenColor(currSpan.second);
        mTextPaint.setColor(spanColor);

        //----------------------------------------------
        // start painting!
        //----------------------------------------------
        // 获取总共的行数
        int rowCount = hDoc.getRowCount();
        if (isShowLineNumbers) {
            mLineNumPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
            canvas.drawLine(mLeftOffset - mSpaceWidth / 2, getScrollY(), mLeftOffset - mSpaceWidth / 2, getScrollY() + getHeight(), mLineNumPaint);
        }

        Typeface lastTypeface;
        switch (currType) {
            case Lexer.KEYWORD:
                lastTypeface = boldTypeface;
                break;
            case Lexer.DOUBLE_SYMBOL_LINE:
                lastTypeface = italicTypeface;
                break;
            default:
                lastTypeface = defTypeface;
        }

        mTextPaint.setTypeface(lastTypeface);
        while (currRowNum <= endRowNum) {
            int spanLen = spanOffset - currIndex;

            /*String row = hDoc.getRow(currRowNum);
            boolean charDraw = false;
            if (row.contains("\t")) {
                charDraw = true;
            } else if (currRowNum == rowCount - 1) {
                charDraw = true;
            } else if (currRowNum == mCaretRow) {
                charDraw = true;
            } else if (isSelectText()) {
                charDraw = true;
            }
*/
            int rowLen = hDoc.getRowSize(currRowNum);

            if (currRowNum >= rowCount) {
                break;
            }
            //int padx = (int) (mLeftOffset - mLineNumPaint.measureText(currLineNum + "") - mSpaceWidth / 2) / 2;
            int padx = (int) (mLeftOffset - mLineNumPaint.measureText(currLineNum + "") - mSpaceWidth);
            if (isShowLineNumbers && currLineNum != lastLineNum) {
                lastLineNum = currLineNum;
                String num = String.valueOf(currLineNum);
                drawLineNum(canvas, num, padx, paintY);
            }

            paintX = mLeftOffset;
            int i = 0;

            while (i < rowLen) {
                // check if formatting changes are needed
                if (nextSpan != null && currIndex >= spanOffset) {
                    currSpan = nextSpan;

                    spanLen = currSpan.first;
                    spanOffset += spanLen;
                    lastType = currType;
                    currType = currSpan.second;

                    if (lastType != currType) {
                        Typeface currTypeface;
                        switch (currType) {
                            case Lexer.KEYWORD:
                                currTypeface = boldTypeface;
                                break;
                            case Lexer.DOUBLE_SYMBOL_LINE:
                                currTypeface = italicTypeface;
                                break;
                            default:
                                currTypeface = defTypeface;
                        }

                        if (lastTypeface != currTypeface) {
                            mTextPaint.setTypeface(currTypeface);
                            lastTypeface = currTypeface;
                        }

                        spanColor = mColorScheme.getTokenColor(currType);
                        mTextPaint.setColor(spanColor);
                    }
                    if (spanIndex < spanSize) {
                        nextSpan = spans.get(spanIndex++);
                    } else {
                        nextSpan = null;
                    }
                }

                char c = hDoc.charAt(currIndex);
                int x = paintX;//解决光标被Selection挡住
                if (mFieldController.inSelectionRange(currIndex)) {
                    paintX += drawSelectedText(canvas, c, paintX, paintY);
                } else {
                    // 绘制文本
                    paintX += drawChar(canvas, c, paintX, paintY);
                }

                // 绘制光标
                if (currIndex == mCaretPosition && isCursorVisiable) {
                    drawCaret(canvas, x, paintY);
                }
                ++currIndex;
                ++i;
                spanLen--;
            }

            if (hDoc.charAt(currIndex - 1) == Language.NEWLINE)
                ++currLineNum;

            paintY += rowHeight();


            if (paintX > xExtent) {
                // record widest line seen so far
                xExtent = paintX;
            }
            if (paintX > mLineMaxWidth)
                mLineMaxWidth = paintX;

            ++currRowNum;
        } // end while
        doOptionHighlightRow(canvas);
//        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
//        int advanceWidth = 0;
//        if (Character.valueOf(DefFormatter.indentChar).equals('\t')) {
//            advanceWidth = getTabAdvance(0);
//        } else {
//            advanceWidth = mSpaceWidth;
//        }

        if (isShowRegion && !isWordWrap()) {
//            int originColor = mTextPaint.getColor();
//            mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.LINE_HIGHLIGHT));
            doBlockLine(canvas);
//            mTextPaint.setColor(originColor);
        }
    }

    private void doBlockLine(Canvas canvas) {
        ArrayList<Rect> lines = Lexer.mLines;
        if (lines == null || lines.isEmpty())
            return;
        Rect bounds = canvas.getClipBounds();
        int bt = bounds.top;
        int bb = bounds.bottom;
        for (Rect rect : lines) {
//            if(rect.top == mCaretRow){
//                doBlockRow(canvas, rect.bottom);
//            } else if(rect.bottom == mCaretRow){
//                doBlockRow(canvas, rect.top);
//            }
            int top = (rect.top + 1) * rowHeight();
            int bottom = rect.bottom * rowHeight();
            if (bottom < bt || top > bb)
                continue;
            int left = Math.min(getCharExtent(rect.left).first, getCharExtent(rect.right).first) + mCursorWidth;
            canvas.drawLine(left, top, left, bottom, mLineNumPaint);
        }
    }

    /**
     * Underline the caret row if the option for highlighting it is set
     * 高亮当前行
     */
    private void doOptionHighlightRow(Canvas canvas) {
        if (isHighlightRow) {
            int y = getPaintBaseline(mCaretRow);
            int originalColor = mTextPaint.getColor();
            mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.LINE_HIGHLIGHT));

            int lineLength = Math.max(getWidth(), mLineMaxWidth + mLeftOffset);
            //Math.max(xExtent, getContentWidth());
            // 绘制当前行的背景色
            drawTextBackground(canvas, mLeftOffset, y, lineLength);
            mTextPaint.setColor(originalColor);
        }
    }

    // 绘制文本
    private int drawChar(Canvas canvas, char c, int paintX, int paintY) {

        int originalColor = mTextPaint.getColor();
        int charWidth = getAdvance(c, paintX);

        if (paintX > getScrollX() || paintX < (getScrollX() + getContentWidth()))
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    mCharEmoji = c;
                    break;
                case ' ':
                    if (isShowNonPrinting) {
                        mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(Language.GLYPH_SPACE, 0, 1, paintX, paintY, mTextPaint);
                        mTextPaint.setColor(originalColor);
                    } else {
                        canvas.drawText(" ", 0, 1, paintX, paintY, mTextPaint);
                    }
                    break;

                case Language.EOF: //fall-through
                case Language.NEWLINE:
                    if (isShowNonPrinting) {
                        mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(Language.GLYPH_NEWLINE, 0, 1, paintX, paintY, mTextPaint);
                        mTextPaint.setColor(originalColor);
                    }
                    break;

                case Language.TAB:
                    if (isShowNonPrinting) {
                        mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(Language.GLYPH_TAB, 0, 1, paintX, paintY, mTextPaint);
                        mTextPaint.setColor(originalColor);
                    }
                    break;

                default:
                    if (mCharEmoji != 0) {
                        canvas.drawText(new char[]{mCharEmoji, c}, 0, 2, paintX, paintY, mTextPaint);
                        mCharEmoji = 0;
                    } else {
                        char[] ca = {c};
                        canvas.drawText(ca, 0, 1, paintX, paintY, mTextPaint);
                    }
                    break;
            }

        return charWidth;
    }

    // paintY is the baseline for text, NOT the top extent
    private void drawTextBackground(Canvas canvas, int paintX, int paintY, int advance) {
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        canvas.drawRect(paintX,
                paintY + metrics.ascent,
                paintX + advance,
                paintY + metrics.descent,
                mTextPaint);
    }

    private int drawSelectedText(Canvas canvas, char c, int paintX, int paintY) {
        int oldColor = mTextPaint.getColor();
        int advance = getAdvance(c);

        mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.SELECTION_BACKGROUND));
        drawTextBackground(canvas, paintX, paintY, advance);

        mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.SELECTION_FOREGROUND));
        drawChar(canvas, c, paintX, paintY);

        mTextPaint.setColor(oldColor);
        return advance;
    }

    //光标
    private void drawCaret(Canvas canvas, int paintX, int paintY) {
        int originalColor = mTextPaint.getColor();
        mCaretX = paintX - mCursorWidth / 2;
        mCaretY = paintY;
        int caretColor = mColorScheme.getColor(ColorScheme.Colorable.CARET_DISABLED);
        mTextPaint.setColor(caretColor);
        // draw full caret
        drawTextBackground(canvas, mCaretX, paintY, mCursorWidth);
        mTextPaint.setColor(originalColor);
    }

    // 绘制行号
    private int drawLineNum(@NotNull Canvas canvas, String s, int paintX, int paintY) {
        int originalColor = mTextPaint.getColor();
        mTextPaint.setColor(mColorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
        canvas.drawText(s, paintX, paintY, mLineNumPaint);
        mTextPaint.setColor(originalColor);
        return 0;
    }

    @Override
    final public int getRowWidth() {
        return getContentWidth() - mLeftOffset;
    }

    /**
     * Returns printed width of c.
     * <p>
     * Takes into account user-specified tab width and also handles
     * application-defined widths for NEWLINE and EOF
     *
     * @param c Character to measure
     * @return Advance of character, in pixels
     */
    @Override
    public int getAdvance(char c) {
        return getAdvance(c, 0);
    }

    public int getAdvance(char c, int x) {
        int advance;
        switch (c) {
            case 0xd83c:
            case 0xd83d:
                advance = 0;
                break;
            case ' ':
                advance = getSpaceAdvance();
                break;
            case Language.NEWLINE: // fall-through
            case Language.EOF:
                advance = getEOLAdvance();
                break;
            case Language.TAB:
                advance = getTabAdvance(x);
                break;
            default:
                if (mCharEmoji != 0) {
                    char[] ca = {mCharEmoji, c};
                    advance = (int) mTextPaint.measureText(ca, 0, 2);
                } else {
                    char[] ca = {c};
                    advance = (int) mTextPaint.measureText(ca, 0, 1);
                }
                break;
        }
        return advance;
    }

    //---------------------------------------------------------------------
    //------------------- Scrolling and touch -----------------------------

    public int getCharAdvance(char c) {
        int advance;
        char[] ca = {c};
        advance = (int) mTextPaint.measureText(ca, 0, 1);
        return advance;
    }

    protected int getSpaceAdvance() {
        if (isShowNonPrinting) {
            return (int) mTextPaint.measureText(Language.GLYPH_SPACE,
                    0, Language.GLYPH_SPACE.length());
        } else {
            return mSpaceWidth;
        }
    }

    protected int getEOLAdvance() {
        if (isShowNonPrinting) {
            return (int) mTextPaint.measureText(Language.GLYPH_NEWLINE,
                    0, Language.GLYPH_NEWLINE.length());
        } else {
            return (int) (EMPTY_CARET_WIDTH_SCALE * mTextPaint.measureText(" ", 0, 1));
        }
    }

    protected int getTabAdvance() {
        if (isShowNonPrinting) {
            return mTabLength * (int) mTextPaint.measureText(Language.GLYPH_SPACE,
                    0, Language.GLYPH_SPACE.length());
        } else {
            return mTabLength * mSpaceWidth;
        }
    }

    protected int getTabAdvance(int x) {
        if (isShowNonPrinting) {
            return mTabLength * (int) mTextPaint.measureText(Language.GLYPH_SPACE,
                    0, Language.GLYPH_SPACE.length());
        } else {
            int i = (x - mLeftOffset) / mSpaceWidth % mTabLength;
            return (mTabLength - i) * mSpaceWidth;
        }
    }

    /**
     * Invalidate rows from startRow (inclusive) to endRow (exclusive)
     */
    private void invalidateRows(int startRow, int endRow) {
        TextWarriorException.assertVerbose(startRow <= endRow && startRow >= 0,
                "Invalid startRow and/or endRow");

        Rect caretSpill = mNavMethod.getCaretBloat();
        //TODO The ascent of (startRow+1) may jut inside startRow, so part of
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                endRow * rowHeight() + getPaddingTop() + caretSpill.bottom);
    }

    /**
     * Invalidate rows from startRow (inclusive) to the end of the field
     */
    private void invalidateFromRow(int startRow) {
        TextWarriorException.assertVerbose(startRow >= 0,
                "Invalid startRow");

        Rect caretSpill = mNavMethod.getCaretBloat();
        //TODO The ascent of (startRow+1) may jut inside startRow, so part of
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
    }

    private void invalidateCaretRow() {
        invalidateRows(mCaretRow, mCaretRow + 1);
    }

    private void invalidateSelectionRows() {
        int startRow = hDoc.findRowNumber(mSelectionAnchor);
        int endRow = hDoc.findRowNumber(mSelectionEdge);

        invalidateRows(startRow, endRow + 1);
    }

    /**
     * Scrolls the text horizontally and/or vertically if the character
     * specified by charOffset is not in the visible text region.
     * The view is invalidated if it is scrolled.
     *
     * @param charOffset The index of the character to make visible
     * @return True if the drawing area was scrolled horizontally
     * and/or vertically
     */
    private boolean makeCharVisible(int charOffset) {
        TextWarriorException.assertVerbose(
                charOffset >= 0 && charOffset < hDoc.docLength(),
                "Invalid charOffset given");
        int scrollVerticalBy = makeCharRowVisible(charOffset);
        int scrollHorizontalBy = makeCharColumnVisible(charOffset);

        if (scrollVerticalBy == 0 && scrollHorizontalBy == 0) {
            return false;
        } else {
            scrollBy(scrollHorizontalBy, scrollVerticalBy);
            return true;
        }
    }

    /**
     * Calculates the amount to scroll vertically if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll vertically
     */
    private int makeCharRowVisible(int charOffset) {
        int scrollBy = 0;
        int currLine = hDoc.findRowNumber(charOffset);
        int charTop = currLine * rowHeight();
        int charBottom = charTop + rowHeight();

        if (isCaretScrolled) {
            // 拖动水滴滚动在距离SCROLL_EDGE_SLOP的时候就开始滚动
            if (charTop < getScrollY()) {
                scrollBy = charTop - getScrollY();
            } else if (charBottom + SCROLL_EDGE_SLOP > (getScrollY() + getContentHeight())) {
                scrollBy = charBottom + SCROLL_EDGE_SLOP - getScrollY() - getContentHeight();
            }
        } else {
            // 默认情况在水滴移动到屏幕上下边缘时才开始滚动
            if (charTop < getScrollY()) {
                scrollBy = charTop - getScrollY();
            } else if (charBottom > (getScrollY() + getContentHeight())) {
                scrollBy = charBottom - getScrollY() - getContentHeight();
            }
        }

        return scrollBy;
    }

    /**
     * Calculates the amount to scroll horizontally if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll horizontally
     */
    private int makeCharColumnVisible(int charOffset) {
        int scrollBy = 0;
        Pair visibleRange = getCharExtent(charOffset);

        int charLeft = visibleRange.first;
        int charRight = visibleRange.second;


        if (isCaretScrolled) {
            // 拖动水滴滚动在距离SCROLL_EDGE_SLOP / 3的时候就开始滚动
            if (charRight + SCROLL_EDGE_SLOP / 3 >= (getScrollX() + getContentWidth())) {
                scrollBy = charRight + SCROLL_EDGE_SLOP / 3 - getScrollX() - getContentWidth();

            } else if (charLeft - SCROLL_EDGE_SLOP / 3 <= getScrollX() + mAlphaWidth) {
                scrollBy = charLeft - SCROLL_EDGE_SLOP / 3 - getScrollX() - mAlphaWidth;
                if (charLeft <= mLeftOffset)
                    scrollBy = 0;
            }
        } else {
            // 默认情况在水滴移动到屏幕左右边缘时才开始滚动
            if (charRight > (getScrollX() + getContentWidth())) {
                scrollBy = charRight - getScrollX() - getContentWidth();
            } else if (charLeft < getScrollX() + mAlphaWidth) {
                scrollBy = charLeft - getScrollX() - mAlphaWidth;
            }
        }

        return scrollBy;
    }

    /**
     * Calculates the x-coordinate extent of charOffset.
     *
     * @return The x-values of left and right edges of charOffset. Pair.first
     * contains the left edge and Pair.second contains the right edge
     */
    protected Pair getCharExtent(int charOffset) {
        int row = hDoc.findRowNumber(charOffset);
        int rowOffset = hDoc.getRowOffset(row);
        int left = mLeftOffset;
        int right = mLeftOffset;
        boolean isEmoji = false;
        String rowText = hDoc.getRow(row);
        int i = 0;

        int len = rowText.length();
        while (rowOffset + i <= charOffset && i < len) {
            char c = rowText.charAt(i);
            left = right;
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    right += (int) mTextPaint.measureText(ca, 0, 2);
                    break;
                case Language.NEWLINE:
                case Language.EOF:
                    right += getEOLAdvance();
                    break;
                case ' ':
                    right += getSpaceAdvance();
                    break;
                case Language.TAB:
                    right += getTabAdvance(right);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        right += getCharAdvance(c);
                    break;
            }
            ++i;
        }
        return new Pair(left, right);
    }

    /**
     * Returns the bounding box of a character in the text field.
     * The coordinate system used is one where (0, 0) is the top left corner
     * of the text, before padding is added.
     *
     * @param charOffset The character offset of the character of interest
     * @return Rect(left, top, right, bottom) of the character bounds,
     * or Rect(-1, -1, -1, -1) if there is no character at that coordinate.
     */
    Rect getBoundingBox(int charOffset) {
        if (charOffset < 0 || charOffset >= hDoc.docLength()) {
            return new Rect(-1, -1, -1, -1);
        }

        int row = hDoc.findRowNumber(charOffset);
        int top = row * rowHeight();
        int bottom = top + rowHeight();

        Pair xExtent = getCharExtent(charOffset);
        int left = xExtent.first;
        int right = xExtent.second;

        return new Rect(left, top, right, bottom);
    }

    public ColorScheme getColorScheme() {
        return mColorScheme;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        mColorScheme = colorScheme;
        mNavMethod.onColorSchemeChanged(colorScheme);
        setBackgroundColor(colorScheme.getColor(ColorScheme.Colorable.BACKGROUND));
    }

    /**
     * Maps a coordinate to the character that it is on. If the coordinate is
     * on empty space, the nearest character on the corresponding row is returned.
     * If there is no character on the row, -1 is returned.
     * <p>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the closest character, or -1 if there is
     * no character or nearest character at that coordinate
     */
    int coordToCharIndex(int x, int y) {
        int row = y / rowHeight();
        if (row > hDoc.getRowCount())
            return hDoc.docLength() - 1;

        int charIndex = hDoc.getRowOffset(row);
        if (charIndex < 0) {
            //non-existent row
            return -1;
        }

        if (x < 0) {
            return charIndex; // coordinate is outside, to the left of view
        }

        String rowText = hDoc.getRow(row);

        int extent = mLeftOffset;
        int i = 0;
        boolean isEmoji = false;

        //x-=getAdvance('a')/2;
        int len = rowText.length();
        while (i < len) {
            char c = rowText.charAt(i);
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    extent += (int) mTextPaint.measureText(ca, 0, 2);
                    break;
                case Language.NEWLINE:
                case Language.EOF:
                    extent += getEOLAdvance();
                    break;
                case ' ':
                    extent += getSpaceAdvance();
                    break;
                case Language.TAB:
                    extent += getTabAdvance(extent);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        extent += getCharAdvance(c);

            }

            if (extent >= x) {
                break;
            }
            ++i;
        }

        if (i < rowText.length()) {
            return charIndex + i;
        }
        //nearest char is last char of line
        return charIndex + i - 1;
    }

    /**
     * Maps a coordinate to the character that it is on.
     * Returns -1 if there is no character on the coordinate.
     * <p>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the character that is on the coordinate,
     * or -1 if there is no character at that coordinate.
     */
    int coordToCharIndexStrict(int x, int y) {
        int row = y / rowHeight();
        int charIndex = hDoc.getRowOffset(row);

        if (charIndex < 0 || x < 0) {
            //non-existent row
            return -1;
        }

        String rowText = hDoc.getRow(row);

        int extent = 0;
        int i = 0;
        boolean isEmoji = false;

        //x-=getAdvance('a')/2;
        int len = rowText.length();
        while (i < len) {
            char c = rowText.charAt(i);
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    extent += (int) mTextPaint.measureText(ca, 0, 2);
                    break;
                case Language.NEWLINE:
                case Language.EOF:
                    extent += getEOLAdvance();
                    break;
                case ' ':
                    extent += getSpaceAdvance();
                    break;
                case Language.TAB:
                    extent += getTabAdvance(extent);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        extent += getCharAdvance(c);
            }

            if (extent >= x) {
                break;
            }
            ++i;
        }

        if (i < rowText.length()) {
            return charIndex + i;
        }

        //no char enclosing x
        return -1;
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum x-value that can be scrolled to for the current rows
     * of text in the viewport.
     */
    int getMaxScrollX() {
        if (isWordWrap())
            return mLeftOffset;
        else
            return Math.max(0, xExtent - getContentWidth() + mNavMethod.getCaretBloat().right + mAlphaWidth);
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum y-value that can be scrolled to.
     */
    int getMaxScrollY() {
        //return Math.max(0,hDoc.getRowCount() * rowHeight() - getContentHeight() / 2 + mNavMethod.getCaretBloat().bottom);
        //滚动时最后一行下面允许的空高度
        return Math.max(0, hDoc.getRowCount() * rowHeight() - getContentHeight() / 2 + mNavMethod.getCaretBloat().bottom);
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return hDoc.getRowCount() * rowHeight() + getPaddingTop() + getPaddingBottom();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    public final void smoothScrollBy(int dx, int dy) {
        if (getHeight() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > 250) {
            //final int maxY = getMaxScrollX();
            final int scrollY = getScrollY();
            final int scrollX = getScrollX();

            //dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;

            mScroller.startScroll(scrollX, scrollY, dx, dy);
            postInvalidate();
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }


    //---------------------------------------------------------------------
    //------------------------- Caret methods -----------------------------

    /**
     * Start fling scrolling
     */

    void flingScroll(int velocityX, int velocityY) {

        mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY,
                0, getMaxScrollX(), 0, getMaxScrollY());
        // Keep on drawing until the animation has finished.
        postInvalidate();
        //postInvalidateOnAnimation();
    }

    public boolean isFlingScrolling() {
        return !mScroller.isFinished();
    }

    public void stopFlingScrolling() {
        mScroller.forceFinished(true);
    }

    /**
     * Starting scrolling continuously in scrollDir.
     * Not private to allow access by TouchNavigationMethod.
     *
     * @return True if auto-scrolling started
     */
    boolean autoScrollCaret(int scrollDir) {
        boolean scrolled = false;
        switch (scrollDir) {
            case SCROLL_UP:
                removeCallbacks(mScrollCaretUpTask);
                if ((!caretOnFirstRowOfFile())) {
                    post(mScrollCaretUpTask);
                    scrolled = true;
                }
                break;
            case SCROLL_DOWN:
                removeCallbacks(mScrollCaretDownTask);
                if (!caretOnLastRowOfFile()) {
                    post(mScrollCaretDownTask);
                    scrolled = true;
                }
                break;
            case SCROLL_LEFT:
                removeCallbacks(mScrollCaretLeftTask);
                if (mCaretPosition > 0 &&
                        mCaretRow == hDoc.findRowNumber(mCaretPosition - 1)) {
                    post(mScrollCaretLeftTask);
                    scrolled = true;
                }
                break;
            case SCROLL_RIGHT:
                removeCallbacks(mScrollCaretRightTask);
                if (!caretOnEOF() &&
                        mCaretRow == hDoc.findRowNumber(mCaretPosition + 1)) {
                    post(mScrollCaretRightTask);
                    scrolled = true;
                }
                break;
            default:
                TextWarriorException.fail("Invalid scroll direction");
                break;
        }
        return scrolled;
    }

    /**
     * Stops automatic scrolling initiated by autoScrollCaret(int).
     * Not private to allow access by TouchNavigationMethod
     */
    void stopAutoScrollCaret() {
        removeCallbacks(mScrollCaretDownTask);
        removeCallbacks(mScrollCaretUpTask);
        removeCallbacks(mScrollCaretLeftTask);
        removeCallbacks(mScrollCaretRightTask);
    }

    /**
     * Stops automatic scrolling in scrollDir direction.
     * Not private to allow access by TouchNavigationMethod
     */
    void stopAutoScrollCaret(int scrollDir) {
        switch (scrollDir) {
            case SCROLL_UP:
                removeCallbacks(mScrollCaretUpTask);
                break;
            case SCROLL_DOWN:
                removeCallbacks(mScrollCaretDownTask);
                break;
            case SCROLL_LEFT:
                removeCallbacks(mScrollCaretLeftTask);
                break;
            case SCROLL_RIGHT:
                removeCallbacks(mScrollCaretRightTask);
                break;
            default:
                TextWarriorException.fail("Invalid scroll direction");
                break;
        }
    }

    public int getCaretRow() {
        return mCaretRow;
    }

    public int getCaretPosition() {
        return mCaretPosition;
    }

    /**
     * Sets the caret to position i, scrolls it to view and invalidates
     * the necessary areas for redrawing
     *
     * @param i The character index that the caret should be set to
     */
    public void moveCaret(int i) {
        mFieldController.moveCaret(i);
    }

    /**
     * Sets the caret one position back, scrolls it on screen, and invalidates
     * the necessary areas for redrawing.
     * <p>
     * If the caret is already on the first character, nothing will happen.
     */
    public void moveCaretLeft() {
        mFieldController.moveCaretLeft(false);
    }

    /**
     * Sets the caret one position forward, scrolls it on screen, and
     * invalidates the necessary areas for redrawing.
     * <p>
     * If the caret is already on the last character, nothing will happen.
     */
    public void moveCaretRight() {
        mFieldController.moveCaretRight(false);
    }

    /**
     * Sets the caret one row down, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p>
     * If the caret is already on the last row, nothing will happen.
     */
    public void moveCaretDown() {
        mFieldController.moveCaretDown();
    }


    //---------------------------------------------------------------------
    //------------------------- Text Selection ----------------------------

    /**
     * Sets the caret one row up, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p>
     * If the caret is already on the first row, nothing will happen.
     */
    public void moveCaretUp() {
        mFieldController.moveCaretUp();
    }

    /**
     * Scrolls the caret into view if it is not on screen
     */
    public void focusCaret() {
        makeCharVisible(mCaretPosition);
    }

    /**
     * @return The column number where charOffset appears on
     */
    protected int getColumn(int charOffset) {
        int row = hDoc.findRowNumber(charOffset);
        TextWarriorException.assertVerbose(row >= 0,
                "Invalid char offset given to getColumn");
        int firstCharOfRow = hDoc.getRowOffset(row);
        return charOffset - firstCharOfRow;
    }

    protected boolean caretOnFirstRowOfFile() {
        return (mCaretRow == 0);
    }

    protected boolean caretOnLastRowOfFile() {
        return (mCaretRow == (hDoc.getRowCount() - 1));
    }

    protected boolean caretOnEOF() {
        return (mCaretPosition == (hDoc.docLength() - 1));
    }

    @Contract(pure = true)
    public final boolean isSelectText() {
        return mFieldController.isSelectText();
    }

    @Contract(pure = true)
    public final boolean isSelectText2() {
        return mFieldController.isSelectText2();
    }

    /**
     * Enter or exit select mode.
     * Invalidates necessary areas for repainting.
     *
     * @param mode If true, enter select mode; else exit select mode
     */
    public void selectText(boolean mode) {
        if (mFieldController.isSelectText() && !mode) {
            invalidateSelectionRows();
            mFieldController.setSelectText(false);
        } else if (!mFieldController.isSelectText() && mode) {
            invalidateCaretRow();
            mFieldController.setSelectText(true);
        }
    }

    public void selectAll() {
        mFieldController.setSelectionRange(0, hDoc.docLength() - 1, false, true);
    }

    public void setSelection(int beginPosition, int numChars) {
        mFieldController.setSelectionRange(beginPosition, numChars, true, false);
    }

    public void setSelectionRange(int beginPosition, int numChars) {
        mFieldController.setSelectionRange(beginPosition, numChars, true, true);
    }

    public boolean inSelectionRange(int charOffset) {
        return mFieldController.inSelectionRange(charOffset);
    }

    public int getSelectionStart() {
        if (mSelectionAnchor < 0)
            return mCaretPosition;
        else
            return mSelectionAnchor;
    }

    public int getSelectionEnd() {
        if (mSelectionEdge < 0)
            return mCaretPosition;
        else
            return mSelectionEdge;
    }

    public void focusSelectionStart() {
        mFieldController.focusSelection(true);
    }

    public void focusSelectionEnd() {
        mFieldController.focusSelection(false);
    }

    //---------------------------------------------------------------------
    //------------------------- Formatting methods ------------------------

    public void cut() {
        if (mSelectionAnchor != mSelectionEdge)
            mFieldController.cut(mClipboardManager);
    }

    public void copy() {
        if (mSelectionAnchor != mSelectionEdge)
            mFieldController.copy(mClipboardManager);
        selectText(false);
    }

    public void paste() {
        CharSequence text = mClipboardManager.getText();
        if (text != null)
            mFieldController.paste(text.toString());
    }

    public void cut(ClipboardManager cb) {
        mFieldController.cut(cb);
    }

    public void copy(ClipboardManager cb) {
        mFieldController.copy(cb);
    }

    public void paste(String text) {
        mFieldController.paste(text);
    }

    private boolean reachedNextSpan(int charIndex, Pair span) {
        return span != null && (charIndex == span.first);
    }

    public void respan() {
        mFieldController.determineSpans();
    }

    public void cancelSpanning() {
        mFieldController.cancelSpanning();
    }

    /**
     * Sets the text to use the new typeface, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     */
    public void setTypeface(Typeface typeface) {
        defTypeface = typeface;
        boldTypeface = Typeface.create(typeface, Typeface.BOLD);
        italicTypeface = Typeface.create(typeface, Typeface.ITALIC);
        mTextPaint.setTypeface(typeface);
        mLineNumPaint.setTypeface(typeface);
        if (hDoc.isWordWrap())
            hDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        if (!makeCharVisible(mCaretPosition)) {
            invalidate();
        }
    }

    public void setItalicTypeface(Typeface typeface) {
        italicTypeface = typeface;
    }

    public void setBoldTypeface(Typeface typeface) {
        boldTypeface = typeface;
    }

    public boolean isWordWrap() {
        return hDoc.isWordWrap();
    }

    public void setWordWrap(boolean enable) {
        hDoc.setWordWrap(enable);
        if (enable) {
            xExtent = 0;
            scrollTo(0, 0);
        }
        mFieldController.updateCaretRow();
        if (!makeCharVisible(mCaretPosition)) {
            invalidate();
        }
    }

    public float getZoom() {
        return mZoomFactor;
    }

    /**
     * Sets the text size to be factor of the base text size, scrolls the view
     * to display the caret if needed, and invalidates the entire view
     */
    public void setZoom(float factor) {
        if (factor <= 0.5 || factor >= 5 || factor == mZoomFactor) {
            return;
        }
        mZoomFactor = factor;
        int newSize = (int) (factor * BASE_TEXT_SIZE_PIXELS);
        mTextPaint.setTextSize(newSize);
        mLineNumPaint.setTextSize(newSize);
        if (hDoc.isWordWrap())
            hDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        mAlphaWidth = (int) mTextPaint.measureText("a");
        //if(!makeCharVisible(mCaretPosition)){
        invalidate();
        //}
    }

    /**
     * Sets the length of a tab character, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     *
     * @param spaceCount The number of spaces a tab represents
     */
    public void setTabSpaces(int spaceCount) {
        if (spaceCount < 0) {
            return;
        }
        mTabLength = spaceCount;
        if (hDoc.isWordWrap())
            hDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        if (!makeCharVisible(mCaretPosition)) {
            invalidate();
        }
    }

    /**
     * Enable/disable auto-indent
     */
    public void setAutoIndent(boolean enable) {
        isAutoIndent = enable;
    }

    public void setAutoComplete(boolean enable) {
        isAutoComplete = enable;
    }

    /**
     * Enable/disable long-pressing capitalization.
     * When enabled, a long-press on a hardware key capitalizes that letter.
     * When disabled, a long-press on a hardware key bring up the
     * CharacterPickerDialog, if there are alternative characters to choose from.
     */
    public void setLongPressCaps(boolean enable) {
        isLongPressCaps = enable;
    }

    /**
     * Enable/disable highlighting of the current row. The current row is also
     * invalidated
     */
    public void setHighlightCurrentRow(boolean enable) {
        isHighlightRow = enable;
        invalidateCaretRow();
    }

    /**
     * Enable/disable display of visible representations of non-printing
     * characters like spaces, tabs and end of lines
     * Invalidates the view if the enable state changes
     */
    public void setNonPrintingCharVisibility(boolean enable) {
        if (enable ^ isShowNonPrinting) {
            isShowNonPrinting = enable;
            if (hDoc.isWordWrap())
                hDoc.analyzeWordWrap();
            mFieldController.updateCaretRow();
            if (!makeCharVisible(mCaretPosition)) {
                invalidate();
            }
        }
    }

    //---------------------------------------------------------------------
    //------------------------- Event handlers ----------------------------
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        //Intercept multiple key presses of printing characters to implement
        //long-press caps, because the IME may consume them and not pass the
        //event to onKeyDown() for long-press caps logic to work.
        //TODO Technically, long-press caps should be implemented in the IME,
        //but is put here for end-user's convenience. Unfortunately this may
        //cause some IMEs to break. Remove this feature in future.
        if (isLongPressCaps
                && event.getRepeatCount() == 1
                && event.getAction() == KeyEvent.ACTION_DOWN) {

            char c = KeysInterpreter.keyEventToPrintableChar(event);
            if (Character.isLowerCase(c)
                    && c == Character.toLowerCase(hDoc.charAt(mCaretPosition - 1))) {
                mFieldController.onPrintableChar(Language.BACKSPACE);
                mFieldController.onPrintableChar(Character.toUpperCase(c));
                return true;
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Let touch navigation method intercept key event first
        if (mNavMethod.onKeyDown(keyCode, event)) {
            return true;
        }

        //check if direction or symbol key
        if (KeysInterpreter.isNavigationKey(event)) {
            handleNavigationKey(keyCode, event);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SYM ||
                keyCode == KeyCharacterMap.PICKER_DIALOG_INPUT) {
            showCharacterPicker(
                    PICKER_SETS.get(KeyCharacterMap.PICKER_DIALOG_INPUT), false);
            return true;
        }

        //check if character is printable
        char c = KeysInterpreter.keyEventToPrintableChar(event);
        if (c == Language.NULL_CHAR) {
            return super.onKeyDown(keyCode, event);
        }

        int repeatCount = event.getRepeatCount();
        //handle multiple (held) key presses
        if (repeatCount == 1) {
            if (isLongPressCaps) {
                handleLongPressCaps(c);
            } else {
                handleLongPressDialogDisplay(c);
            }
        } else if (repeatCount == 0
                || isLongPressCaps && !Character.isLowerCase(c)
                || !isLongPressCaps && PICKER_SETS.get(c) == null) {
            mFieldController.onPrintableChar(c);
        }

        return true;
    }

    private void handleNavigationKey(int keyCode, @NotNull KeyEvent event) {
        if (event.isShiftPressed() && !isSelectText()) {
            invalidateCaretRow();
            mFieldController.setSelectText(true);
        } else if (!event.isShiftPressed() && isSelectText()) {
            invalidateSelectionRows();
            mFieldController.setSelectText(false);
        }
        // remove cursor blink callback
        stopBlink();
        // restart blink
        startBlink();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mFieldController.moveCaretRight(false);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mFieldController.moveCaretLeft(false);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mFieldController.moveCaretDown();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                mFieldController.moveCaretUp();
                break;
            default:
                break;
        }
    }

    private void handleLongPressCaps(char c) {
        if (Character.isLowerCase(c)
                && c == hDoc.charAt(mCaretPosition - 1)) {
            mFieldController.onPrintableChar(Language.BACKSPACE);
            mFieldController.onPrintableChar(Character.toUpperCase(c));
        } else {
            mFieldController.onPrintableChar(c);
        }
    }

    //Precondition: If c is alphabetical, the character before the caret is
    //also c, which can be lower- or upper-case
    private void handleLongPressDialogDisplay(char c) {
        //workaround to get the appropriate caps mode to use
        boolean isCaps = Character.isUpperCase(hDoc.charAt(mCaretPosition - 1));
        char base = (isCaps) ? Character.toUpperCase(c) : c;

        String candidates = PICKER_SETS.get(base);
        if (candidates != null) {
            mFieldController.stopTextComposing();
            showCharacterPicker(candidates, true);
        } else {
            mFieldController.onPrintableChar(c);
        }
    }

    /**
     * @param candidates A string of characters to for the user to choose from
     * @param replace    If true, the character before the caret will be replaced
     *                   with the user-selected char. If false, the user-selected char will
     *                   be inserted at the caret position.
     */
    private void showCharacterPicker(String candidates, boolean replace) {
        final boolean shouldReplace = replace;
        final SpannableStringBuilder dummyString = new SpannableStringBuilder();
        Selection.setSelection(dummyString, 0);

        CharacterPickerDialog dialog = new CharacterPickerDialog(getContext(),
                this, dummyString, candidates, true);

        dialog.setOnDismissListener(dialog1 -> {
            if (dummyString.length() > 0) {
                if (shouldReplace) {
                    mFieldController.onPrintableChar(Language.BACKSPACE);
                }
                mFieldController.onPrintableChar(dummyString.charAt(0));
            }
        });
        dialog.show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mNavMethod.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(@NotNull MotionEvent event) {
        // TODO Test on real device
        int deltaX = Math.round(event.getX());
        int deltaY = Math.round(event.getY());
        while (deltaX > 0) {
            mFieldController.moveCaretRight(false);
            --deltaX;
        }
        while (deltaX < 0) {
            mFieldController.moveCaretLeft(false);
            ++deltaX;
        }
        while (deltaY > 0) {
            mFieldController.moveCaretDown();
            --deltaY;
        }
        while (deltaY < 0) {
            mFieldController.moveCaretUp();
            ++deltaY;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isFocused()) {
            mNavMethod.onTouchEvent(event);
        } else {
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP
                    && isPointInView((int) event.getX(), (int) event.getY())) {
                // somehow, the framework does not automatically change the focus
                // to this view when it is touched
                requestFocus();
            }
        }
        return true;
    }

    final private boolean isPointInView(int x, int y) {
        return (x >= 0 && x < getWidth() &&
                y >= 0 && y < getHeight());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        invalidateCaretRow();
    }

    /**
     * Not public to allow access by {@link TouchNavigationMethod}
     */
    void showIME(boolean show) {

        InputMethodManager im = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (show) {
            im.showSoftInput(this, 0);
        } else {
            im.hideSoftInputFromWindow(this.getWindowToken(), 0);
        }
    }

    /**
     * Some navigation methods use sensors or have states for their widgets.
     * They should be notified of application lifecycle events so they can
     * start/stop sensing and load/store their GUI state.
     */
    void onPause() {
        mNavMethod.onPause();
    }

    void onResume() {
        mNavMethod.onResume();
    }

    void onDestroy() {
        mFieldController.cancelSpanning();
    }

    //*********************************************************************
    //************************ Controller logic ***************************
    //*********************************************************************
    private class TextFieldController
            implements Lexer.LexCallback {
        private final Lexer _lexer = new Lexer(this);
        private boolean _isInSelectionMode = false;
        private boolean _isInSelectionMode2;

        /**
         * Analyze the text for programming language keywords and redraws the
         * text view when done. The global programming language used is set with
         * the static method Lexer.setLanguage(Language)
         * <p>
         * Does nothing if the Lexer language is not a programming language
         */
        public void determineSpans() {
            isShowRegion = false;
            _lexer.tokenize(hDoc);
        }

        public void cancelSpanning() {
            isShowRegion = true;
            _lexer.cancelTokenize();
        }

        @Override
        //This is usually called from a non-UI thread
        public void lexDone(final List<Pair> results) {
            post(() -> {
                isShowRegion = true;
                hDoc.setSpans(results);
                invalidate();
            });
        }

        //- TextFieldController -----------------------------------------------
        //---------------------------- Key presses ----------------------------

        //TODO minimise invalidate calls from moveCaret(), insertion/deletion and word wrap
        public void onPrintableChar(char c) {
            isShowRegion = false;
            // delete currently selected text, if any
            boolean selectionDeleted = false;
            if (_isInSelectionMode) {
                selectionDelete();
                selectionDeleted = true;
            }

            int originalRow = mCaretRow;
            int originalOffset = hDoc.getRowOffset(originalRow);

            switch (c) {
                case Language.BACKSPACE:
                    if (selectionDeleted) {
                        break;
                    }

                    if (mCaretPosition > 0) {
                        hDoc.deleteAt(mCaretPosition - 1, System.nanoTime());
                        if (hDoc.charAt(mCaretPosition - 2) == 0xd83d || hDoc.charAt(mCaretPosition - 2) == 0xd83c) {
                            hDoc.deleteAt(mCaretPosition - 2, System.nanoTime());
                            moveCaretLeft(true);
                        }

                        mTextListener.onDel(c + "", mCaretPosition, 1);
                        moveCaretLeft(true);

                        if (mCaretRow < originalRow) {
                            // either a newline was deleted or the caret was on the
                            // first word and it became short enough to fit the prev
                            // row
                            invalidateFromRow(mCaretRow);
                        } else if (hDoc.isWordWrap()) {
                            if (originalOffset != hDoc.getRowOffset(originalRow)) {
                                //invalidate previous row too if its wrapping changed
                                --originalRow;
                            }
                            //TODO invalidate damaged rows only
                            invalidateFromRow(originalRow);
                        }
                    }
                    break;

                case Language.NEWLINE:
                    if (isAutoIndent) {
                        char[] indent = createAutoIndent();
                        hDoc.insertBefore(indent, mCaretPosition, System.nanoTime());
                        moveCaret(mCaretPosition + indent.length);
                    } else {
                        hDoc.insertBefore(c, mCaretPosition, System.nanoTime());
                        moveCaretRight(true);
                    }

                    if (hDoc.isWordWrap() && originalOffset != hDoc.getRowOffset(originalRow)) {
                        //invalidate previous row too if its wrapping changed
                        --originalRow;
                    }

                    mTextListener.onNewLine(c + "", mCaretPosition, 1);
                    invalidateFromRow(originalRow);
                    break;

                default:
                    hDoc.insertBefore(c, mCaretPosition, System.nanoTime());
                    moveCaretRight(true);
                    mTextListener.onAdd(c + "", mCaretPosition, 1);
                    if (hDoc.isWordWrap()) {
                        if (originalOffset != hDoc.getRowOffset(originalRow)) {
                            //invalidate previous row too if its wrapping changed
                            --originalRow;
                        }
                        //TODO invalidate damaged rows only
                        invalidateFromRow(originalRow);
                    }
                    break;
            }
            setEdited(true);
            determineSpans();
        }

        /**
         * Return a char[] with a newline as the 0th element followed by the
         * leading spaces and tabs of the line that the caret is on
         * 创建自动缩进
         */
        @NotNull
        private char[] createAutoIndent() {
            int lineNum = hDoc.findLineNumber(mCaretPosition);
            int startOfLine = hDoc.getLineOffset(lineNum);
            int whitespaceCount = 0;
            hDoc.seekChar(startOfLine);
            //查找上一行的空白符个数
            while (hDoc.hasNext()) {
                char c = hDoc.next();
                if ((c != ' ' && c != Language.TAB) || startOfLine + whitespaceCount >= mCaretPosition) {

                    break;
                }
                if (c == Language.TAB) {
                    whitespaceCount += getAutoIndentWidth();
                }
                if (c == ' ')
                    ++whitespaceCount;
            }
            //寻找最后字符
            hDoc.seekChar(startOfLine);
            int endChar = 0;
            while (hDoc.hasNext()) {
                char c = hDoc.next();
                if (c == Language.NEWLINE) {
                    break;
                }
                endChar = c;
            }
            //最后字符为'{',缩进
            if (endChar == '{')
                whitespaceCount += getAutoIndentWidth();
            if (whitespaceCount < 0)
                return new char[]{Language.NEWLINE};

            char[] indent = new char[1 + whitespaceCount];
            indent[0] = Language.NEWLINE;

            hDoc.seekChar(startOfLine);
            for (int i = 0; i < whitespaceCount; ++i) {
                indent[1 + i] = ' ';
            }
            return indent;
        }

        public void moveCaretDown() {
            if (!caretOnLastRowOfFile()) {
                int currCaret = mCaretPosition;
                int currRow = mCaretRow;
                int newRow = currRow + 1;
                int currColumn = getColumn(currCaret);
                int currRowLength = hDoc.getRowSize(currRow);
                int newRowLength = hDoc.getRowSize(newRow);

                if (currColumn < newRowLength) {
                    // Position at the same column as old row.
                    mCaretPosition += currRowLength;
                } else {
                    // Column does not exist in the new row (new row is too short).
                    // Position at end of new row instead.
                    mCaretPosition +=
                            currRowLength - currColumn + newRowLength - 1;
                }
                ++mCaretRow;

                updateSelectionRange(currCaret, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(currRow, newRow + 1);
                }
                // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
                mCaretListener.updateCaret(mCaretPosition);
                mRowListener.onRowChanged(newRow);
                stopTextComposing();
            }
        }

        public void moveCaretUp() {
            if (!caretOnFirstRowOfFile()) {
                int currCaret = mCaretPosition;
                int currRow = mCaretRow;
                int newRow = currRow - 1;
                int currColumn = getColumn(currCaret);
                int newRowLength = hDoc.getRowSize(newRow);

                if (currColumn < newRowLength) {
                    // Position at the same column as old row.
                    mCaretPosition -= newRowLength;
                } else {
                    // Column does not exist in the new row (new row is too short).
                    // Position at end of new row instead.
                    mCaretPosition -= (currColumn + 1);
                }
                --mCaretRow;

                updateSelectionRange(currCaret, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(newRow, currRow + 1);
                }
                // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
                mCaretListener.updateCaret(mCaretPosition);
                mRowListener.onRowChanged(newRow);
                stopTextComposing();
            }
        }

        /**
         * @param isTyping Whether caret is moved to a consecutive position as
         *                 a result of entering text
         */
        public void moveCaretRight(boolean isTyping) {
            if (!caretOnEOF()) {
                int originalRow = mCaretRow;
                ++mCaretPosition;
                updateCaretRow();
                updateSelectionRange(mCaretPosition - 1, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(originalRow, mCaretRow + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
                // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
                mCaretListener.updateCaret(mCaretPosition);
            }
        }

        /**
         * @param isTyping Whether caret is moved to a consecutive position as
         *                 a result of deleting text
         */
        public void moveCaretLeft(boolean isTyping) {
            if (mCaretPosition > 0) {
                int originalRow = mCaretRow;
                --mCaretPosition;
                updateCaretRow();
                updateSelectionRange(mCaretPosition + 1, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(mCaretRow, originalRow + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
                // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
                mCaretListener.updateCaret(mCaretPosition);
            }
        }

        public void moveCaret(int i) {
            if (i < 0 || i >= hDoc.docLength()) {
                TextWarriorException.fail("Invalid caret position");
                return;
            }
            updateSelectionRange(mCaretPosition, i);

            mCaretPosition = i;
            updateAfterCaretJump();
        }

        private void updateAfterCaretJump() {
            int oldRow = mCaretRow;
            updateCaretRow();
            if (!makeCharVisible(mCaretPosition)) {
                invalidateRows(oldRow, oldRow + 1); //old caret row
                invalidateCaretRow(); //new caret row
            }
            stopTextComposing();
        }


        /**
         * This helper method should only be used by internal methods after setting
         * mCaretPosition, in order to to recalculate the new row the caret is on.
         */
        void updateCaretRow() {
            int newRow = hDoc.findRowNumber(mCaretPosition);
            if (mCaretRow != newRow) {
                mCaretRow = newRow;
                mRowListener.onRowChanged(newRow);
            }
        }

        public void stopTextComposing() {
            InputMethodManager im = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            // This is an overkill way to inform the InputMethod that the caret
            // might have changed position and it should re-evaluate the
            // caps mode to use.
            im.restartInput(FreeScrollingTextField.this);

            if (mInputConnection != null && mInputConnection.isComposingStarted()) {
                mInputConnection.resetComposingState();
            }
        }

        //- TextFieldController -----------------------------------------------
        //-------------------------- Selection mode ---------------------------
        @Contract(pure = true)
        public final boolean isSelectText() {
            return _isInSelectionMode;
        }

        /**
         * Enter or exit select mode.
         * Does not invalidate view.
         *
         * @param mode If true, enter select mode; else exit select mode
         */
        public void setSelectText(boolean mode) {
            if (!(mode ^ _isInSelectionMode)) {
                return;
            }

            if (mode) {
                mSelectionAnchor = mCaretPosition;
                mSelectionEdge = mCaretPosition;
            } else {
                mSelectionAnchor = -1;
                mSelectionEdge = -1;
            }
            _isInSelectionMode = mode;
            _isInSelectionMode2 = mode;
            mSelModeListener.onSelectionChanged(mode, getSelectionStart(), getSelectionEnd());
        }

        @Contract(pure = true)
        public final boolean isSelectText2() {
            return _isInSelectionMode2;
        }

        public boolean inSelectionRange(int charOffset) {
            if (mSelectionAnchor < 0) {
                return false;
            }

            return (mSelectionAnchor <= charOffset &&
                    charOffset < mSelectionEdge);
        }

        /**
         * Selects numChars count of characters starting from beginPosition.
         * Invalidates necessary areas.
         *
         * @param beginPosition
         * @param numChars
         * @param scrollToStart If true, the start of the selection will be scrolled
         *                      into view. Otherwise, the end of the selection will be scrolled.
         */
        public void setSelectionRange(int beginPosition, int numChars,
                                      boolean scrollToStart, boolean mode) {
            TextWarriorException.assertVerbose(
                    (beginPosition >= 0) && numChars <= (hDoc.docLength() - 1) && numChars >= 0,
                    "Invalid range to select");

            if (_isInSelectionMode) {
                // unhighlight previous selection
                invalidateSelectionRows();
            } else {
                // unhighlight caret
                invalidateCaretRow();
                if (mode)
                    setSelectText(true);
                else
                    _isInSelectionMode = true;
            }

            mSelectionAnchor = beginPosition;
            mSelectionEdge = mSelectionAnchor + numChars;

            mCaretPosition = mSelectionEdge;
            stopTextComposing();
            updateCaretRow();
            if (mode)
                mSelModeListener.onSelectionChanged(isSelectText(), mSelectionAnchor, mSelectionEdge);
            boolean scrolled = makeCharVisible(mSelectionEdge);

            if (scrollToStart) {
                //TODO reduce unnecessary scrolling and write a method to scroll
                // the beginning of multi-line selections as far left as possible
                scrolled = makeCharVisible(mSelectionAnchor);
            }

            if (!scrolled) {
                invalidateSelectionRows();
            }
        }

        /**
         * Moves the caret to an edge of selected text and scrolls it to view.
         *
         * @param start If true, moves the caret to the beginning of
         *              the selection. Otherwise, moves the caret to the end of the selection.
         *              In all cases, the caret is scrolled to view if it is not visible.
         */
        public void focusSelection(boolean start) {
            if (_isInSelectionMode) {
                if (start && mCaretPosition != mSelectionAnchor) {
                    mCaretPosition = mSelectionAnchor;
                    updateAfterCaretJump();
                } else if (!start && mCaretPosition != mSelectionEdge) {
                    mCaretPosition = mSelectionEdge;
                    updateAfterCaretJump();
                }
            }
        }


        /**
         * Used by internal methods to update selection boundaries when a new
         * caret position is set.
         * Does nothing if not in selection mode.
         */
        private void updateSelectionRange(int oldCaretPosition, int newCaretPosition) {


            if (!_isInSelectionMode) {
                return;
            }

            if (oldCaretPosition < mSelectionEdge) {
                if (newCaretPosition > mSelectionEdge) {
                    mSelectionAnchor = mSelectionEdge;
                    mSelectionEdge = newCaretPosition;
                } else {
                    mSelectionAnchor = newCaretPosition;
                }

            } else {
                if (newCaretPosition < mSelectionAnchor) {
                    mSelectionEdge = mSelectionAnchor;
                    mSelectionAnchor = newCaretPosition;
                } else {
                    mSelectionEdge = newCaretPosition;
                }
            }
        }


        //- TextFieldController -----------------------------------------------
        //------------------------ Cut, copy, paste ---------------------------

        /**
         * Convenience method for consecutive copy and paste calls
         */
        public void cut(ClipboardManager cb) {
            copy(cb);
            selectionDelete();
        }

        /**
         * Copies the selected text to the clipboard.
         * <p>
         * Does nothing if not in select mode.
         */
        public void copy(ClipboardManager cb) {
            //TODO catch OutOfMemoryError
            if (_isInSelectionMode &&
                    mSelectionAnchor < mSelectionEdge) {
                CharSequence contents = hDoc.subSequence(mSelectionAnchor,
                        mSelectionEdge - mSelectionAnchor);
                cb.setText(contents);
            }
        }

        /**
         * Inserts text at the caret position.
         * Existing selected text will be deleted and select mode will end.
         * The deleted area will be invalidated.
         * <p>
         * After insertion, the inserted area will be invalidated.
         */
        public void paste(String text) {
            if (text == null) {
                return;
            }

            hDoc.beginBatchEdit();
            selectionDelete();

            int originalRow = mCaretRow;
            int originalOffset = hDoc.getRowOffset(originalRow);
            hDoc.insertBefore(text.toCharArray(), mCaretPosition, System.nanoTime());
            mTextListener.onAdd(text, mCaretPosition, text.length());
            //_textLis.onAdd(text, mCaretPosition, text.length());
            hDoc.endBatchEdit();

            mCaretPosition += text.length();
            updateCaretRow();

            setEdited(true);
            determineSpans();
            stopTextComposing();

            if (!makeCharVisible(mCaretPosition)) {
                int invalidateStartRow = originalRow;
                //invalidate previous row too if its wrapping changed
                if (hDoc.isWordWrap() &&
                        originalOffset != hDoc.getRowOffset(originalRow)) {
                    --invalidateStartRow;
                }

                if (originalRow == mCaretRow && !hDoc.isWordWrap()) {
                    //pasted text only affects caret row
                    invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                } else {
                    //TODO invalidate damaged rows only
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        /**
         * Deletes selected text, exits select mode and invalidates deleted area.
         * If the selected range is empty, this method exits select mode and
         * invalidates the caret.
         * <p>
         * Does nothing if not in select mode.
         */
        public void selectionDelete() {
            if (!_isInSelectionMode) {
                return;
            }

            int totalChars = mSelectionEdge - mSelectionAnchor;

            if (totalChars > 0) {
                int originalRow = hDoc.findRowNumber(mSelectionAnchor);
                int originalOffset = hDoc.getRowOffset(originalRow);
                boolean isSingleRowSel = hDoc.findRowNumber(mSelectionEdge) == originalRow;
                hDoc.deleteAt(mSelectionAnchor, totalChars, System.nanoTime());
                mTextListener.onDel("", mCaretPosition, totalChars);
                mCaretPosition = mSelectionAnchor;
                updateCaretRow();
                setEdited(true);
                determineSpans();
                setSelectText(false);
                stopTextComposing();

                if (!makeCharVisible(mCaretPosition)) {
                    int invalidateStartRow = originalRow;
                    //invalidate previous row too if its wrapping changed
                    if (hDoc.isWordWrap() &&
                            originalOffset != hDoc.getRowOffset(originalRow)) {
                        --invalidateStartRow;
                    }

                    if (isSingleRowSel && !hDoc.isWordWrap()) {
                        //pasted text only affects current row
                        invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                    } else {
                        //TODO invalidate damaged rows only
                        invalidateFromRow(invalidateStartRow);
                    }
                }
            } else {
                setSelectText(false);
                invalidateCaretRow();
            }
        }

        void replaceText(int from, int charCount, String text) {
            int invalidateStartRow, originalOffset;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;
            //delete selection
            if (_isInSelectionMode) {
                invalidateStartRow = hDoc.findRowNumber(mSelectionAnchor);
                originalOffset = hDoc.getRowOffset(invalidateStartRow);

                int totalChars = mSelectionEdge - mSelectionAnchor;

                if (totalChars > 0) {
                    mCaretPosition = mSelectionAnchor;
                    hDoc.deleteAt(mSelectionAnchor, totalChars, System.nanoTime());

                    if (invalidateStartRow != mCaretRow) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }

                setSelectText(false);
            } else {
                invalidateStartRow = mCaretRow;
                originalOffset = hDoc.getRowOffset(mCaretRow);
            }

            //delete requested chars
            if (charCount > 0) {
                int delFromRow = hDoc.findRowNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                    originalOffset = hDoc.getRowOffset(delFromRow);
                }

                if (invalidateStartRow != mCaretRow) {
                    isInvalidateSingleRow = false;
                }

                mCaretPosition = from;
                hDoc.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }

            //insert
            if (text != null && text.length() > 0) {
                int insFromRow = hDoc.findRowNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                    originalOffset = hDoc.getRowOffset(insFromRow);
                }

                hDoc.insertBefore(text.toCharArray(), mCaretPosition, System.nanoTime());
                mCaretPosition += text.length();
                dirty = true;
            }

            if (dirty) {
                setEdited(true);
                determineSpans();
            }

            int originalRow = mCaretRow;
            updateCaretRow();
            if (originalRow != mCaretRow) {
                isInvalidateSingleRow = false;
            }

            if (!makeCharVisible(mCaretPosition)) {
                //invalidate previous row too if its wrapping changed
                if (hDoc.isWordWrap() &&
                        originalOffset != hDoc.getRowOffset(invalidateStartRow)) {
                    --invalidateStartRow;
                }

                if (isInvalidateSingleRow && !hDoc.isWordWrap()) {
                    //replaced text only affects current row
                    invalidateRows(mCaretRow, mCaretRow + 1);
                } else {
                    //TODO invalidate damaged rows only
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        //- TextFieldController -----------------------------------------------
        //----------------- Helper methods for InputConnection ----------------

        /**
         * Deletes existing selected text, then deletes charCount number of
         * characters starting at from, and inserts text in its place.
         * <p>
         * Unlike paste or selectionDelete, does not signal the end of
         * text composing to the IME.
         */
        void replaceComposingText(int from, int charCount, String text) {
            int invalidateStartRow, originalOffset;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;

            //delete selection
            if (_isInSelectionMode) {
                invalidateStartRow = hDoc.findRowNumber(mSelectionAnchor);
                originalOffset = hDoc.getRowOffset(invalidateStartRow);

                int totalChars = mSelectionEdge - mSelectionAnchor;

                if (totalChars > 0) {
                    mCaretPosition = mSelectionAnchor;
                    hDoc.deleteAt(mSelectionAnchor, totalChars, System.nanoTime());

                    if (invalidateStartRow != mCaretRow) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }

                setSelectText(false);
            } else {
                invalidateStartRow = mCaretRow;
                originalOffset = hDoc.getRowOffset(mCaretRow);
            }

            //delete requested chars
            if (charCount > 0) {
                int delFromRow = hDoc.findRowNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                    originalOffset = hDoc.getRowOffset(delFromRow);
                }

                if (invalidateStartRow != mCaretRow) {
                    isInvalidateSingleRow = false;
                }

                mCaretPosition = from;
                hDoc.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }

            //insert
            if (text != null && text.length() > 0) {
                int insFromRow = hDoc.findRowNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                    originalOffset = hDoc.getRowOffset(insFromRow);
                }

                hDoc.insertBefore(text.toCharArray(), mCaretPosition, System.nanoTime());
                mCaretPosition += text.length();
                dirty = true;
            }

            mTextListener.onAdd(text, mCaretPosition, text.length() - charCount);
            if (dirty) {
                setEdited(true);
                determineSpans();
            }

            int originalRow = mCaretRow;
            updateCaretRow();
            if (originalRow != mCaretRow) {
                isInvalidateSingleRow = false;
            }

            if (!makeCharVisible(mCaretPosition)) {
                //invalidate previous row too if its wrapping changed
                if (hDoc.isWordWrap() &&
                        originalOffset != hDoc.getRowOffset(invalidateStartRow)) {
                    --invalidateStartRow;
                }

                if (isInvalidateSingleRow && !hDoc.isWordWrap()) {
                    //replaced text only affects current row
                    invalidateRows(mCaretRow, mCaretRow + 1);
                } else {
                    //TODO invalidate damaged rows only
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        /**
         * Delete leftLength characters of text before the current caret
         * position, and delete rightLength characters of text after the current
         * cursor position.
         * <p>
         * Unlike paste or selectionDelete, does not signal the end of
         * text composing to the IME.
         */
        void deleteAroundComposingText(int left, int right) {
            int start = mCaretPosition - left;
            if (start < 0) {
                start = 0;
            }
            int end = mCaretPosition + right;
            int docLength = hDoc.docLength();
            if (end > (docLength - 1)) { //exclude the terminal EOF
                end = docLength - 1;
            }
            replaceComposingText(start, end - start, "");
        }

        String getTextAfterCursor(int maxLen) {
            int docLength = hDoc.docLength();
            if ((mCaretPosition + maxLen) > (docLength - 1)) {
                //exclude the terminal EOF
                return hDoc.subSequence(mCaretPosition, docLength - mCaretPosition - 1).toString();
            }

            return hDoc.subSequence(mCaretPosition, maxLen).toString();
        }

        String getTextBeforeCursor(int maxLen) {
            int start = mCaretPosition - maxLen;
            if (start < 0) {
                start = 0;
            }
            return hDoc.subSequence(start, mCaretPosition - start).toString();
        }
    }//end inner controller class

    //*********************************************************************
    //************************** InputConnection **************************
    //*********************************************************************
    /*
     * Does not provide ExtractedText related methods
     */
    private class TextFieldInputConnection extends BaseInputConnection {
        private boolean isComposing = false;
        private int composingCharCount = 0;

        public TextFieldInputConnection(FreeScrollingTextField v) {
            super(v, true);
        }

        public void resetComposingState() {
            composingCharCount = 0;
            isComposing = false;
            hDoc.endBatchEdit();
        }

        @Override
        public boolean performContextMenuAction(int id) {
            switch (id) {
                case android.R.id.copy:
                    copy();
                    break;
                case android.R.id.cut:
                    cut();
                    break;
                case android.R.id.paste:
                    paste();
                    break;
                case android.R.id.startSelectingText:
                case android.R.id.stopSelectingText:
                case android.R.id.selectAll:
                    selectAll();
                    break;
            }

            return false;
        }

        @Override
        public boolean sendKeyEvent(@NotNull KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                    if (isSelectText())
                        selectText(false);
                    else
                        selectText(true);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    moveCaretLeft();
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveCaretUp();
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    moveCaretRight();
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveCaretDown();
                    break;
                case KeyEvent.KEYCODE_MOVE_HOME:
                    moveCaret(0);
                    break;
                case KeyEvent.KEYCODE_MOVE_END:
                    moveCaret(hDoc.length() - 1);
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
//                    if(mAutoCompletePanel.isShow()) {
//                        mAutoCompletePanel.selectFirst();
//                    } else {
//                        return super.sendKeyEvent(event);
//                    }
//                    scrollTo(0, getScrollY());
//                    super.sendKeyEvent(event);
//                    break;
                default:
                    return super.sendKeyEvent(event);
            }
            return true;
        }

        /**
         * Only true when the InputConnection has not been used by the IME yet.
         * Can be programatically cleared by resetComposingState()
         */
        public boolean isComposingStarted() {
            return isComposing;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            isComposing = true;
            if (!hDoc.isBatchEdit()) {
                hDoc.beginBatchEdit();
            }

            mFieldController.replaceComposingText(
                    getCaretPosition() - composingCharCount,
                    composingCharCount,
                    text.toString());
            composingCharCount = text.length();

            //TODO reduce invalidate calls
            if (newCursorPosition > 1) {
                mFieldController.moveCaret(mCaretPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                mFieldController.moveCaret(mCaretPosition - text.length() - newCursorPosition);
            }
            // log("setComposingText:"+text+","+newCursorPosition);
            return true;
        }

        /**
         * 输入法传递过来的字符串
         *
         * @param text
         * @param newCursorPosition
         * @return
         */
        @Override
        public boolean commitText(@NotNull CharSequence text, int newCursorPosition) {
            // log("commitText:"+text+","+newCursorPosition+","+_composingCharCount);
            mFieldController.replaceComposingText(
                    getCaretPosition() - composingCharCount,
                    composingCharCount,
                    text.toString());
            composingCharCount = 0;
            hDoc.endBatchEdit();
            //TODO reduce invalidate calls
            if (newCursorPosition > 1) {
                mFieldController.moveCaret(mCaretPosition + newCursorPosition - 1);
            }
//          else if(newCursorPosition==1){
//              mFieldController.moveCaret(getCaretPosition() + newCursorPosition);
//          }
            else if (newCursorPosition <= 0) {
                mFieldController.moveCaret(mCaretPosition - text.length() - newCursorPosition);
            }
            isComposing = false;
            return true;
        }


        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            if (composingCharCount != 0) {
//                Log.d("lua","Warning: Implmentation of InputConnection.deleteSurroundingText" +
//                      " will not skip composing text");
            }

            mFieldController.deleteAroundComposingText(leftLength, rightLength);
            return true;
        }

        @Override
        public boolean finishComposingText() {
            resetComposingState();
            return true;
        }

        @Override
        public int getCursorCapsMode(int reqModes) {
            int capsMode = 0;

            // Ignore InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS; not used in TextWarrior

            if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                    == InputType.TYPE_TEXT_FLAG_CAP_WORDS) {
                int prevChar = mCaretPosition - 1;
                if (prevChar < 0 || Lexer.getLanguage().isWhitespace(hDoc.charAt(prevChar))) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;

                    //set CAP_SENTENCES if client is interested in it
                    if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                            == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) {
                        capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                    }
                }
            }

            // Strangely, Android soft keyboard does not set TYPE_TEXT_FLAG_CAP_SENTENCES
            // in reqModes even if it is interested in doing auto-capitalization.
            // Android bug? Therefore, we assume TYPE_TEXT_FLAG_CAP_SENTENCES
            // is always set to be on the safe side.
            else {
                Language lang = Lexer.getLanguage();

                int prevChar = mCaretPosition - 1;
                int whitespaceCount = 0;
                boolean capsOn = true;

                // Turn on caps mode only for the first char of a sentence.
                // A fresh line is also considered to start a new sentence.
                // The position immediately after a period is considered lower-case.
                // Examples: "abc.com" but "abc. Com"
                while (prevChar >= 0) {
                    char c = hDoc.charAt(prevChar);
                    if (c == Language.NEWLINE) {
                        break;
                    }

                    if (!lang.isWhitespace(c)) {
                        if (whitespaceCount == 0 || !lang.isSentenceTerminator(c)) {
                            capsOn = false;
                        }
                        break;
                    }

                    ++whitespaceCount;
                    --prevChar;
                }

                if (capsOn) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                }
            }

            return capsMode;
        }

        @Override
        public CharSequence getTextAfterCursor(int maxLen, int flags) {
            return mFieldController.getTextAfterCursor(maxLen); //ignore flags
        }

        @Override
        public CharSequence getTextBeforeCursor(int maxLen, int flags) {
            return mFieldController.getTextBeforeCursor(maxLen); //ignore flags
        }

        @Override
        public boolean setSelection(int start, int end) {
            // log("setSelection:"+start+","+end);

            if (start == end) {
                if (start == 0) {
                    //适配搜狗输入法
                    if (getCaretPosition() > 0) {
                        mFieldController.moveCaret(getCaretPosition() - 1);
                    }
                } else {
                    mFieldController.moveCaret(start);
                }
            } else {
                mFieldController.setSelectionRange(start, end - start, false, true);
            }
            return true;
        }
    }// end inner class
}