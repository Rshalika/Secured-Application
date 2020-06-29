package com.strawhat.securedapplication.utils

import android.view.View


fun Boolean?.toVisibility(): Int {
    return if (this == true) {
        View.VISIBLE
    } else {
        View.GONE
    }
}