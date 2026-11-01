package com.example.data.document

sealed class SlideElement {
    data class Text(val text: String, val isTitle: Boolean) : SlideElement()
    data class Image(val localPath: String) : SlideElement()
}
