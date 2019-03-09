package com.github.maciejkaznowski.library;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

class ViewUtils {

    @NonNull
    static String getLayoutHexString(@LayoutRes int layout) {
        return String.format("0x%8x", layout);
    }
}
