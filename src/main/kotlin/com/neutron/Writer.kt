package com.neutron

import java.io.*
import java.util.*

internal object Writer {
	fun bufferedWriter(text: String, path: String) {
		try {
			val fos = FileOutputStream(path)
			val bout = BufferedOutputStream(fos)
			val a = text.toByteArray()
			bout.write(a)
			bout.flush()
			bout.close()
			fos.flush()
			fos.close()
		} catch (ex: IOException) {
			System.err.println("Error in bufferedWriter")
			ex.printStackTrace()
		}
	}

	fun withoutOverwrite(text: String?, path: String) {
		if(text.isNullOrEmpty()) return
		val log = File(this::class.java.getResource(path)!!.file)
		try {
			if (!log.exists()) {
				println("We had to make a new file.")
				log.createNewFile()
			}
			val fileWriter = FileWriter(log, true)
			val bufferedWriter = BufferedWriter(fileWriter)
			bufferedWriter.write(text)
			bufferedWriter.close()
			println("Done")
		} catch (e: IOException) {
			println("COULD NOT LOG!!")
		}
	}
}

fun read(name: String): String {
	val file = File(name)
	val sc = Scanner(file)
	var buff = ""
	while (sc.hasNextLine()) {
		val data = sc.nextLine()
		buff += data
	}
	sc.close()
	return buff
}
