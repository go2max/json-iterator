package com.jsoniter;

import com.jsoniter.any.*;

import java.io.IOException;

class IterImplSkip {

    static final boolean[] breaks = new boolean[256];

    static {
        breaks[' '] = true;
        breaks['\t'] = true;
        breaks['\n'] = true;
        breaks['\r'] = true;
        breaks[','] = true;
        breaks['}'] = true;
        breaks[']'] = true;
    }

    public static final Any readAny(JsonIterator iter) throws IOException {
        int start = iter.head;
        byte c = IterImpl.nextToken(iter);
        switch (c) {
            case '"':
                IterImpl.skipString(iter);
                return Any.lazyString(iter.buf, start, iter.head);
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                IterImpl.skipUntilBreak(iter);
                return Any.lazyNumber(iter.buf, start, iter.head);
            case 't':
            case 'f':
                IterImpl.skipUntilBreak(iter);
                return Any.lazyBoolean(iter.buf, start, iter.head);
            case 'n':
                IterImpl.skipUntilBreak(iter);
                return Any.lazyNull(iter.buf, start, iter.head);
            case '[':
                IterImpl.skipArray(iter);
                return Any.lazyArray(iter.buf, start, iter.head);
            case '{':
                IterImpl.skipObject(iter);
                return Any.lazyObject(iter.buf, start, iter.head);
            default:
                throw iter.reportError("IterImplSkip", "do not know how to skip: " + c);
        }
    }

    public static final void skip(JsonIterator iter) throws IOException {
        byte c = IterImpl.nextToken(iter);
        switch (c) {
            case '"':
                IterImpl.skipString(iter);
                return;
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 't':
            case 'f':
            case 'n':
                IterImpl.skipUntilBreak(iter);
                return;
            case '[':
                IterImpl.skipArray(iter);
                return;
            case '{':
                IterImpl.skipObject(iter);
                return;
            default:
                throw iter.reportError("IterImplSkip", "do not know how to skip: " + c);
        }
    }

    // adapted from: https://github.com/buger/jsonparser/blob/master/parser.go
    // Tries to find the end of string
    // Support if string contains escaped quote symbols.
    final static int findStringEnd(JsonIterator iter) {
        boolean escaped = false;
        for (int i = iter.head; i < iter.tail; i++) {
            byte c = iter.buf[i];
            if (c == '"') {
                if (!escaped) {
                    return i + 1;
                } else {
                    int j = i - 1;
                    for (; ; ) {
                        if (j < iter.head || iter.buf[j] != '\\') {
                            // even number of backslashes
                            // either end of buffer, or " found
                            return i + 1;
                        }
                        j--;
                        if (j < iter.head || iter.buf[j] != '\\') {
                            // odd number of backslashes
                            // it is \" or \\\"
                            break;
                        }
                        j--;
                    }
                }
            } else if (c == '\\') {
                escaped = true;
            }
        }
        return -1;
    }
}
