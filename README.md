# GetVerse

Get bible chapter/verse right from the comfort of the command line

### Example:
```
java -jar getverse.jar -v "Genesis 1,1" -t kjv
```
### Options
 - ```-v```/```--verse```: verse: ```<Book> <Chapter(number)> <Verse(number, not required)>```
 - ```-t```/```--translation```: bible translation possibilities: 
   - kjv(english - King James Version), 
   - karoli(hungarian - Károli), 
   - vulgate(latin)
 - ```-l```/```--lang```: language for the program, possibilities:
   - english
   - hungarian
 - ```-a```/```--list```: list books
 - ```-d```/```--debug```: debug/verbose
 - ```-n```/```--number```: print number next to verses
 - ```-h```/```--help```: print help