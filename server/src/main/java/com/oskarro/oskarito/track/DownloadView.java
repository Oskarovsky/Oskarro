package com.oskarro.oskarito.track;

/** Where to fetch the file, and how many credits are left afterwards. */
public record DownloadView(String url, int creditsLeft) {
}
