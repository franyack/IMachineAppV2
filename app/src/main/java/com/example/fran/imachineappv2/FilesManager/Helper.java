package com.example.fran.imachineappv2.FilesManager;

import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * Based on: https://github.com/sromku/android-storage
 */

public class Helper {

    public static void showSnackbar(String message, View root) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    public static String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }

}
