package com.neutron

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.FileReader

object Book {
	// first: book name, second: book id
	var metadata: MutableList<Pair<String, Int>> = mutableListOf()
	fun chapters(bookId: Int): Int {
		val reader = FileReader("new.csv")
		val records: Iterable<CSVRecord> = CSVFormat.DEFAULT.withHeader().parse(reader)
		var isIn = false;
		var i = 0
		for (record in records) {
			if (record["BookID"].toInt() == bookId)
				isIn = true
			if (isIn) {
				if (record["BookID"].toInt() != bookId)
					break
				i++
			}
		}
		return i
	}

	fun chapters(book: String) = chapters(IDFromName(book)!!)
	fun IDFromName(name: String): Int? {
		return metadata.find {
			it.first == name
		}?.second
	}

	fun nameFromID(id: Int): String? {
		return metadata.find {
			it.second == id
		}?.first
	}
	fun loadMetadata() {
		var i = 0
		Json.decodeFromString<MutableList<BookName>>(read("books.json")).forEach {
			i++
			Book.metadata.add(Pair(it.name, i))
		}
	}

	fun doesExist(name: String): Boolean = metadata.find { it.first == name } != null
}
