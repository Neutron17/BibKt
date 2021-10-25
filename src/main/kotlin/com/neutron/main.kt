import com.neutron.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.math.BigInteger
import java.net.URL
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess


/*
{"reference":"Genesis 1:1","verses":
[{"book_id":"GEN","book_name":"Genesis","chapter":1,"verse":1,"text":"In principio creavit Deus cælum et terram."}],
"text":"In principio creavit Deus cælum et terram.",
"translation_id":"clementine","translation_name":"Clementine Latin Vulgate","translation_note":"Public Domain"}
* */



val metadata: MutableList<Pair<String, Int>> = mutableListOf()

@Serializable
data class Third(val value: MutableList<String>)

@Serializable
data class Inner(val value: Third)

@Serializable
data class Outer(@Contextual val inner: Inner)

lateinit var book: String
var chapt by Delegates.notNull<Int>()
var vers by Delegates.notNull<Int>()

class Main { companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            parse(args)
            run {
                var i = 0
                Json.decodeFromString<MutableList<BookName>>(read("books.json")).forEach { it ->
                    i++
                    metadata.add(Pair(it.name, i))
                }
            }
            println(Json.decodeFromString<Chapter>(read("bibles/karoli/$book/$chapt.json")).verses[vers+1].text)
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
}}

fun parse(args: Array<String>) {
    run {
        val verse: String
        val options = Options()
        val verseopt = Option("v", "verse", true, "")
        verseopt.isRequired = true
        options.addOption(verseopt)
        val parser: CommandLineParser = DefaultParser()
        val formatter = HelpFormatter()
        val cmd: CommandLine //not a good practice, it serves it purpose
        try {
            cmd = parser.parse(options, args)
        }catch(e: ParseException){
            e.printStackTrace()
            formatter.printHelp("foo", options)
            exitProcess(1)
        }
        verse = cmd.getOptionValue("verse").replace(",", " ").replace(":", " ")
        val foo = verse.split(" ")
        if(foo.size < 3)
            error("Not enough arguments")
        book = foo[0]
        chapt = foo[1].toInt()
        vers = foo[2].toInt()
    }
}

fun read(name: String): String {
    try {
        val file = File(name)
        val sc = Scanner(file)
        var buff = ""
        while (sc.hasNextLine()) {
            val data = sc.nextLine()
            buff += data
        }
        sc.close()
        return buff
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }
    return ""
}