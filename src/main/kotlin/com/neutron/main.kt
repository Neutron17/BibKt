import com.neutron.BookName
import com.neutron.Chapter
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader
import java.lang.NumberFormatException
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
val metadata: MutableList<Pair<String, Int>> = mutableListOf()
const val progName = "GetVerse"
const val VERSION = 1.3

// metadata job
lateinit var md_job: Job
fun loadMetadata() {
	var i = 0
	Json.decodeFromString<MutableList<BookName>>(read("books.json")).forEach {
		i++
		metadata.add(Pair(it.name, i))
	}
}

object Book {
	fun chapters(bookId: Int): Int {
		val reader = FileReader("new.csv")
		val records:Iterable<CSVRecord> = CSVFormat.DEFAULT.withHeader().parse(reader)
		var isIn = false;
		var i = 0
		for(record in records) {
			if(record["BookID"].toInt() == bookId)
				isIn = true
			if(isIn) {
				if(record["BookID"].toInt() != bookId)
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
}
fun chapterByBook() {
	val reader = FileReader("new.csv")
	val json = Json { prettyPrint = true }
	val records: Iterable<CSVRecord> = CSVFormat.DEFAULT.withHeader().parse(reader)
	for (record in records) {
		println("${record["BookID"]} ${record["Chapter"]}")
		if (record["BookID"].toInt() == 3) break
		//record["BookID"][metadata.find { it -> it.first ==  }]

		//for(i in metadata) { File("karoli/${i.first}/").mkdirs() }
	}
}

fun decodeChapter(translation: String, book: String, chapter: Int): Chapter? {
	return try {
		Json.decodeFromString(read("bibles/$translation/$book/$chapter.json"))
	} catch (e: Exception) {
		if (isDebug) e.printStackTrace()
		if (!File("bibles/$translation").exists())
			err("No such translation")
		if (!File("bibles/$translation/$book").exists())
			err("No such book")
		if (!File("bibles/$translation/$book/$chapter.json").exists())
			err("No such chapter")
		else
			err("Unexpected reading/parsing error")
		// bc compiler complaining
		null;
	}
}
suspend fun main(args: Array<String>) {
	coroutineScope {
		md_job = launch {
			loadMetadata()
		}
	}
	val arg = parse(args)
	with(arg) {
		if (isInteractive) {
			if (isBook) {
				println("${arg.book}, enter chapter(1-${Book.chapters(arg.book)})")
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
					System.err.println("Invalid input");
				}
				val ch = decodeChapter(arg.translation.toStr(), arg.book, arg.chapt)!!
				if (!arg.isChapt) {
					if (arg.isNum) print("${arg.vers}. ")
					println(ch.verses[arg.vers - 1].text)
				} else {
					if (arg.isNum)
						ch.verses.forEach { println("${it.verse}. ${it.text}") }
					else
						ch.verses.forEach { println(it.text) }
				}
			}
			exitProcess(0);
		}
	}

	val translation = arg.translation.toStr()
	val book = arg.book
	val chapter = arg.chapt

	val ch = decodeChapter(translation, book, chapter)!!
	if (!arg.isChapt) {
		if (arg.isNum) print("${arg.vers}. ")
		println(ch.verses[arg.vers - 1].text)
	} else {
		if (arg.isNum)
			ch.verses.forEach { println("${it.verse}. ${it.text}") }
		else
			ch.verses.forEach { println(it.text) }
	}
	/*val reader = FileReader("new.csv")
	val json = Json { prettyPrint = true }
	val records:Iterable<CSVRecord> = CSVFormat.DEFAULT.withHeader().parse(reader)
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
		Option("v", "verse", true, "-v \"<Book> <Chapter(number)> <Verse(number, not required)>\""),
		Option(
			"t", "translation", true,
			"kjv / en (King James Version)\nkaroli / hu (Károli - Hungarian)\nvulgate / vul / lat (Vulgate - Latin)",
		),
		Option("d", "debug", false, "Set debug flag on"),
		Option("n", "number", false, "Print verse number"),
		Option("h", "help", false, "Print out help"),
		Option("i", "interactive", false, "Run in interactive mode"),
		Option("l", "list", false, "List books")
	);
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
		metadata.forEach { it ->
			println("${it.second}: ${it.first}");
		}
		exitProcess(0)
	}
	if (cmd.hasOption("number")) arg.isNum = true
	// TRANSLATION
	val value = cmd.getOptionValue("translation")
	arg.translation = when (value) {
		"en", "kjv" ->
			Translation.KJV

		"lat", "val", "vulgate" ->
			Translation.Vulgate

		"karoli", "hu" ->
			Translation.Karoli

		null -> // Default
			Translation.Karoli

		else -> {
			System.err.println("Invalid translation setting")
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
			with(parsed) {
				arg.book = book
				arg.isChapt = isChapt
				arg.isBook = isBook
				if (!isBook)
					arg.chapt = chapt
				if (!isChapt)
					arg.vers = vers
			}
		}
		return arg
	} else {
		if (!cmd.hasOption("verse"))
			err("-v / --verse option must be present")
	}

	verse = cmd.getOptionValue("verse").replace(",", " ").replace(":", " ")

	val verseSlices = verse.split(" ")
	if (verseSlices.size < 2) {
		if (isDebug)
			println("debug: argument count: ${verseSlices.size}")
		err("Not enough arguments (Provide at least a book and a chapter)")
	}
	arg.applyParsed(parseVerse(verse))
	if (isDebug)
		println("debug: ${arg.book} ${arg.chapt}")
	return arg
}

fun parseVerse(raw: String): Args {
	/*if(!raw.matches("(\\d*)\\s*([a-zA-Z]+)\\s*(\\d+)(?:(:|,)(\\d+))?".toRegex())) {
	System.err.println("Invalid verse")
	exitProcess(1)
}*/
	var stringBuilder = ""
	val args = Args()
	try {
		val parts = raw.split(' ').onEach(String::trim) // "Psalms 9 1"
		if(isDebug)
			println("debug: $parts")
		try {
			stringBuilder += "${parts[0]} "
			args.book = parts[0]
			stringBuilder += parts[1]
			args.chapt = parts[2].toInt()
			try {
				args.vers = parts[3].toInt()
			} catch (e: IndexOutOfBoundsException) {
				args.isChapt = true
			}
		} catch (e: Exception) {
			when (e) {
				is NumberFormatException, is IndexOutOfBoundsException -> {}
				else -> throw e
			}
			if (isDebug)
				e.printStackTrace()
			stringBuilder = "${parts[0]} "
			args.book = parts[0]
			try {
				stringBuilder += parts[1]
				args.chapt = parts[1].toInt()
				try {
					stringBuilder += parts[2]
					args.vers = parts[2].toInt()
				} catch (e: IndexOutOfBoundsException) {
					args.isChapt = true
				}
			} catch (e: IndexOutOfBoundsException) {
				args.isBook = true
			}
		}
	} catch (e: Exception) {
		if (isDebug)
			e.printStackTrace()
		System.err.println("Invalid verse/chapter")
	}
	if (isDebug)
		println("debug: ${args}, $stringBuilder")
	return args
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
