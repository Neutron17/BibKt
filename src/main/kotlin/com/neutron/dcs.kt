package com.neutron

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/*@Serializable
data class Verse(val book_id: String, val book_name: String, val chapter: Int, val verse: Int, val text: String)*/
/*@Serializable
data class Bible(val books: List<Book>)*/
/*data class Bible(val reference: String, val verses: Array<Verse>, val text: String, val translation_id: String,
                 val translation_name: String, val translation_note: String)*/
@Serializable
data class Verse(val chapter: Int, val verse: Int, val name: String, val text: String)
@Serializable
data class Chapter(val translation: String, val abbreviation: String, val lang: String,
                val language: String, val direction: String, val encoding: String,
                val book_nr: Int, val book_name: String, val chapter: Int,
                val name: String, @Contextual val verses: Array<Verse>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chapter

        if (translation != other.translation) return false
        if (abbreviation != other.abbreviation) return false
        if (lang != other.lang) return false
        if (language != other.language) return false
        if (direction != other.direction) return false
        if (encoding != other.encoding) return false
        if (book_nr != other.book_nr) return false
        if (book_name != other.book_name) return false
        if (chapter != other.chapter) return false
        if (name != other.name) return false
        if (!verses.contentEquals(other.verses)) return false

        return true
    }

    fun toChapter() = verses.toIt()
    override fun hashCode(): Int {
        var result = translation.hashCode()
        result = 31 * result + abbreviation.hashCode()
        result = 31 * result + lang.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + encoding.hashCode()
        result = 31 * result + book_nr
        result = 31 * result + book_name.hashCode()
        result = 31 * result + chapter
        result = 31 * result + name.hashCode()
        result = 31 * result + verses.contentHashCode()
        return result
    }
}

private fun Array<Verse>.toIt(): MutableList<String> {
    val ret = mutableListOf<String>()
    this.forEach { it ->
        ret.add(it.text)
    }
    return ret
}

/*@Serializable
data class Chapter(val verseCount: Int, var verses: MutableList<String>)
@Serializable
data class Book(val name: String, val chapters: MutableList<Pair<Chapter, Int>>?) {
    var id: Int = 0
    init {
        statId++
        id = statId
    }
    companion object {
        @JvmStatic
        var statId: Int = 0
    }
}*/
/*@Serializable
data class Bqr(val translation: String, val abbreviation: String, val lang: String,
               val language: String, val direction: String, val encoding: String,
               val nr: Int, val name: String, val url: String, val sha: String
)*/
@Serializable
data class BookName(val name: String, val id: Int)