import com.neutron.Args
import com.neutron.Book
import com.neutron.parse
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.random.Random.Default.nextInt
import kotlin.test.junit.JUnitAsserter.assertEquals

class Test {
    @Test
    fun argTest() {
		Book.loadMetadata()
		var args: Args
		for(i in 1..66) {
			val book = Book.nameFromID(i)
			val chapt = nextInt(1, 2)
			val verse = nextInt(1, 10)
			val arr = arrayOf("getverse", "-t", "karoli", "-v", "\"$book $chapt:$verse\"", "-n")
			runBlocking {
				args = parse(arr)
			}
			assertEquals("Book", args.book, book)
			assertEquals("$book Chapter $chapt, ${arr.joinToString()}", args.chapt, chapt)
			assertEquals("Verse", args.vers, verse)
		}
    }
}