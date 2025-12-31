package com.example.numidiapath;

import android.net.Uri;

public interface OnProfileUpdateListener {
    void onUpdate(String newName, String newBio, Uri newImageUri);
}