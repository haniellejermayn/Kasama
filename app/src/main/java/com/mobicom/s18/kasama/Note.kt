package com.mobicom.s18.kasama

class Note(title : String, content : String) {
    var title = title
        private set

    var content = content
        private set

    fun update(newTitle: String, newContent: String) {
        title = newTitle
        content = newContent
    }
}