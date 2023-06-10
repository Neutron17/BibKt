package com.neutron

import com.neutron.I18n.translation
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import java.io.File
import java.util.*
import kotlin.system.exitProcess

fun err(msg: String) {
	System.err.println("ERROR: $msg")
	exitProcess(-1)
}

enum class Translation { KJV, Vulgate, Karoli }
fun Translation.toStr() = when(this) {
	Translation.KJV ->
		"kjv"
	Translation.Vulgate ->
		"vulgate"
	Translation.Karoli ->
		"karoli"
}

data class Args(
	var book: String = "John",
	var translation: Translation = Translation.KJV,
	var chapt: Int = 3,
	var vers: Int  = 16,
	var isBook: Boolean = false,
	var isChapt: Boolean = false,
	var isNum: Boolean = false,
	var isInteractive: Boolean = false
) {
	fun applyParsed(arg: Args) {
		with(this) {
			book = arg.book
			isChapt = arg.isChapt
			isBook = arg.isBook
			if (!arg.isBook)
				chapt = arg.chapt
			if (!arg.isChapt)
				vers = arg.vers
		}
	}
}

var isDebug: Boolean = false
const val progName = "GetVerse"
const val VERSION = 1.5

// metadata job
lateinit var md_job: Job

fun decodeChapter(translation: String, book: String, chapter: Int): Chapter? {
	return try {
		Json.decodeFromString(read("bibles/$translation/$book/$chapter.json"))
	} catch (e: Exception) {
		if (isDebug) e.printStackTrace()
		if (!File("bibles/$translation").exists())
			err(translation("NO_TRANS"))
		if (!File("bibles/$translation/$book").exists())
			err(translation("NO_BOOK"))
		if (!File("bibles/$translation/$book/$chapter.json").exists())
			err(translation("NO_CHAP"))
		else
			err(translation("READ_ERR"))
		// bc compiler complaining
		null
	}
}

fun printChapter(arg: Args, ch: Chapter) {
	if (!arg.isChapt) {
		if (arg.isNum) print("${arg.vers}. ")
		println(ch.verses[arg.vers - 1].text)
	} else {
		if (arg.isNum)
			ch.verses.forEach { println("${it.verse}. ${it.text.dropLastWhile { itt -> itt == '\n' }}") }
		else
			ch.verses.forEach { println(it.text.dropLastWhile { itt -> itt == '\n' }) }
	}
}

fun printBooks(rows: Int = 6) {
	Book.metadata.forEachIndexed { id, it ->
		if((id % rows) == (rows - 1))
			println("${it.first}, ")
		else
			print("${it.first}, ")
	}
}

fun readTillNotNull(): String {
	var inp: String? = null
	while(inp == null)
		inp = readlnOrNull()
	return inp
}

suspend fun main(args: Array<String>) {
	coroutineScope {
		md_job = launch {
			Book.loadMetadata()
		}
	}
	val arg = parse(args)
	with(arg) {
		if (isInteractive) {
			if (isBook) {
				println("${arg.book}, ${translation("ENTER_CHAP")}(1-${Book.chapters(arg.book)})")
				print("> ")
				try {
					val inp = readln()
					val str = "${arg.book} " +
							inp.replace(":", " ").replace(", "," ").replace(",", " ").split(" ").apply {
								this[0].toInt()
								if(this.size == 2)
									this[1].toInt()
							}.joinToString(" ")
					arg.applyParsed(parseVerse(str))
				} catch (e: Exception) {
					System.err.println(translation("INV_INP"))
				}
				val ch = decodeChapter(arg.translation.toStr(), arg.book, arg.chapt)!! // TODO handle error
				printChapter(arg, ch)
			} else {
				println("---${translation("BOOKS")}---")
				printBooks()
				print("${translation("ENT_BOOK")}: ")
				System.out.flush()
				var book = readTillNotNull()
				while(!Book.doesExist(book)) {
					print("${translation("ENT_BOOK")}: ")
					book = readTillNotNull()
				}

				print("${translation("ENT_CHAP")}: ")
				System.out.flush()

				val ch_v = readTillNotNull().replace(", ", " ").replace(",", " ").replace(":", " ")

				arg.applyParsed(parseVerse("$book $ch_v"))
				val ch = decodeChapter(arg.translation.toStr(), arg.book, arg.chapt)!! // TODO handle error
				printChapter(arg, ch)
			}
			exitProcess(0)
		}
	}

	val translation = arg.translation.toStr()
	val book = arg.book
	val chapter = arg.chapt

	val ch = decodeChapter(translation, book, chapter)!! // TODO: handle error
	printChapter(arg, ch)
	/*val reader = FileReader("new.csv")
	val json = Json { prettyPrint = true }
	val records:Iterable<CSVRecord> = CSVFormat.DEFAULT.withHeader().com.neutron.parse(reader)
	for(record in records) {
		println("${record["BookID"]} ${record["Chapter"]}")
		//if(record["BookID"].toInt() == 3) break
		//for(i in metadata) { File("karoli/${i.first}/").mkdirs() }
		val obj = Json.decodeFromString<NewV>(
			URL("https://getbible.net/v2/karoli/${record["BookID"]}/${record["Chapter"]}.json")
				.readText()
		)
		Writer.bufferedWriter(json.encodeToString(obj), "karoli/${obj.book_name}/${obj.chapter}.json")
	}*/
}

suspend fun parse(args: Array<String>): Args {
	if ("-V" in args || "--version" in args) {
		println("Version: $VERSION")
		exitProcess(0)
	}
	val options = Options()
	val opts = arrayOf(
		Option("v", "verse", true, "-v \"${translation("VERSE")}\""),
		Option("t", "translation", true, translation("TRANS")),
		Option("d", "debug", false, translation("DEBUG")),
		Option("n", "number", false, translation("NUMBER")),
		Option("h", "help", false, translation("HELP")),
		Option("i", "interactive", false, translation("INTER")),
		Option("a", "list", false, translation("LIST")),
		Option("l", "lang", true, translation("LANG"))
	)
	with(options) {
		opts.forEach(::addOption)
	}

	val parser: CommandLineParser = DefaultParser()
	val formatter = HelpFormatter()
	val cmd: CommandLine
	try {
		cmd = parser.parse(options, args)
	} catch (e: ParseException) {
		System.err.println(e.localizedMessage)
		formatter.printHelp(progName, options)
		exitProcess(1)
	}
	if (cmd.hasOption("help")) {
		formatter.printHelp(progName, options)
		exitProcess(0)
	}
	val arg = Args()
	if (cmd.hasOption("debug")) isDebug = true
	if (cmd.hasOption("list")) {
		md_job.join()
		printBooks()
		exitProcess(0)
	}
	if(cmd.hasOption("lang")) {
		try {
			I18n.locale = Locale(cmd.getOptionValue("lang"))
		} catch (e: Exception) {
			System.err.println(e.localizedMessage)
			System.err.println(translation("INV_LANG"))
			exitProcess(-1)
		}
		exitProcess(0)
	}
	if (cmd.hasOption("number")) arg.isNum = true
	// TRANSLATION
	val value = cmd.getOptionValue("translation")
	arg.translation = when (value) {
		"en", "kjv" ->
			Translation.KJV

		"lat", "vul", "vulgate" ->
			Translation.Vulgate

		"karoli", "hu","hun" ->
			Translation.Karoli

		null -> { // Default
			if(I18n.locale.country.lowercase().contains("hun"))
				Translation.Karoli
			else
				Translation.KJV
		}
		else -> {
			System.err.println(translation("INV_TRAN"))
			exitProcess(1)
		}
	}
	if (isDebug)
		println("debug: ${arg.translation.toStr()} $value")
	// INTERACTIVE
	val verse: String
	if (cmd.hasOption("interactive")) {
		arg.isInteractive = true
		if (cmd.hasOption("verse")) {
			verse = cmd.getOptionValue("verse").replace(",", " ").replace(":", " ")
			val parsed = parseVerse(verse)
			arg.applyParsed(parsed)
		}
		return arg
	}

	if (!cmd.hasOption("verse"))
		err("-v / --verse ${translation("V_MUST_PRES")}")

	verse = cmd.getOptionValue("verse").replace(", ", " ").replace(",", " ").replace(":", " ")

	val verseSlices = verse.split(" ")
	if (verseSlices.size < 2)
		err(translation("NOT_ENOUGH_ARG") + " (${verseSlices.size}")
	arg.applyParsed(parseVerse(verse))
	if (isDebug)
		println("debug: $arg")
	return arg
}

/** Parse a string into an com.neutron.Args object
 * Like: "Psalms 51:2" -> com.neutron.Args
 */
fun parseVerse(raw: String): Args {
	val args = Args()
	try {
		val parts = raw.split(' ').onEach(String::trim) // "Psalms 9 1 " -> ["Psalms", "9", "1"]
		if(isDebug)
			println("debug: $parts")
		args.book = parts[0]
		val offs: Int = if(parts[0].toIntOrNull() != null) { // staring with number, 2 segments, like 1 Corinthians
			args.book += " ${parts[1]}"
			1
		} else {
			try {
				if (("${parts[0]} ${parts[1]} ${parts[2]}").lowercase() == "song of songs") { // 3 segments
					args.book += " ${parts[1]} ${parts[2]}"
					2
				} else { // 1 segment
					0
				}
			}catch (e: IndexOutOfBoundsException) {
				0
			}
		}
		try {
			args.chapt = parts[offs+1].toInt()
			try {
				args.vers = parts[offs+2].toInt()
			} catch (e: IndexOutOfBoundsException) {
				args.isChapt = true
			}
		} catch (e: IndexOutOfBoundsException) {
			args.isBook = true
		}
	} catch (e: Exception) {
		if (isDebug)
			e.printStackTrace()
		System.err.println(translation("INV_VERSE_OR_CHAPT"))
	}
	if (isDebug)
		println("debug: $args")
	return args
}
