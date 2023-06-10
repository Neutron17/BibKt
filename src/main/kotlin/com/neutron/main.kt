import com.neutron.Book
import com.neutron.Chapter
import com.neutron.I18n
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
const val progName = "GetVerse"
const val VERSION = 1.4

// metadata job
lateinit var md_job: Job

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
			err(I18n.getMessage("NO_TRANS"))
		if (!File("bibles/$translation/$book").exists())
			err(I18n.getMessage("NO_BOOK"))
		if (!File("bibles/$translation/$book/$chapter.json").exists())
			err(I18n.getMessage("NO_CHAP"))
		else
			err(I18n.getMessage("READ_ERR"))
		// bc compiler complaining
		null;
	}
}

fun printChapter(arg: Args, ch: Chapter) {
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

suspend fun main(args: Array<String>) {
	Locale.setDefault(Locale.CHINA)
	coroutineScope {
		md_job = launch {
			Book.loadMetadata()
		}
	}
	val arg = parse(args)
	with(arg) {
		if (isInteractive) {
			if (isBook) {
				println("${arg.book}, ${I18n.getMessage("ENTER_CHAP")}(1-${Book.chapters(arg.book)})")
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
					System.err.println(I18n.getMessage("INV_INP"));
				}
				val ch = decodeChapter(arg.translation.toStr(), arg.book, arg.chapt)!! // TODO handle error
				printChapter(arg, ch)
			} else {
				val row = 6
				println("---${I18n.getMessage("BOOKS")}---")
				Book.metadata.forEachIndexed { id, it ->
					if((id % row) == (row - 1))
						println("${it.first}, ")
					else
						print("${it.first}, ")
				}
				print("Enter a book: ")
				val inp = readln().toUIntOrNull() // TODO: handle error
				TODO("Not yet implemented")

			}
			exitProcess(0);
		}
	}

	val translation = arg.translation.toStr()
	val book = arg.book
	val chapter = arg.chapt

	val ch = decodeChapter(translation, book, chapter)!! // TODO: handle error
	printChapter(arg, ch)
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
		Option("v", "verse", true, "-v \"${I18n.getMessage("VERSE")}\""),
		Option(
			"t", "translation", true,
			I18n.getMessage("TRANS")
		),
		Option("d", "debug", false, I18n.getMessage("DEBUG")),
		Option("n", "number", false, I18n.getMessage("NUMBER")),
		Option("h", "help", false, I18n.getMessage("HELP")),
		Option("i", "interactive", false, I18n.getMessage("INTER")),
		Option("l", "list", false, I18n.getMessage("LIST"))
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
		Book.metadata.forEach { it ->
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
			System.err.println(I18n.getMessage("INV_TRAN"))
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
			err("-v / --verse ${I18n.getMessage("V_MUST_PRES")}")
	}

	verse = cmd.getOptionValue("verse").replace(",", " ").replace(":", " ")

	val verseSlices = verse.split(" ")
	if (verseSlices.size < 2) {
		if (isDebug)
			println("debug: argument count: ${verseSlices.size}")
		err(I18n.getMessage("NOT_ENOUGH_ARG"))
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
		System.err.println(I18n.getMessage("INV_VERSE_OR_CHAPT"))
	}
	if (isDebug)
		println("debug: $args, $stringBuilder")
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
