### Simple Indexer ###

Simple implementation of concurrent [inverted indexer](http://en.wikipedia.org/wiki/Inverted_index).

### Getting started ###

* Requarements:
    * JDK 1.7 or higher
    * maven 3.1.1 or higher

* Dependencies:
    * trove4j
    * slf4j-log4j12
    * apache commons-lang
    * apache commons-io

* How to build:
    * `git clone https://github.com/smolcoder/simpleindexer.git`
    * `cd simpleindexer`
    *  `mvn clean package`
    * in **build** directory will appear executable **simpleindexer.jar**

### API example ###
Create index instance:
```java
WordToPathIndex index = new WordToPathIndex(FileSystems.getDefault());
```
Specify path to some directory with text files. In this example it's index project folder:
```java
String pathToIndexerSrc = System.getProperty("user.dir");
```
Start watch src/ path:
```java
index.startWatch(Paths.get(pathToIndexerSrc, "src"));
```
Getting paths with specified word:
```java
List<String> paths = index.getPathsByWord("public"));
```
Stop watching sub-directory:
```java
index.stopWatch(Paths.get(pathToIndexerSrc, "src/main/java/simpleindexer/fs"));
```
Shutdown index:
```java
index.shutdown();
```
### Comand-line usage exapmle ####
Run indexer (from relative to simpleindexer/):
```bash
java -jar build/simpleindexer.jar
```
Run indexer with indexer options:
```bash
java -jar -Dindexer.threads.count=2 build/simpleindexer.jar
```
Interactive prompt will appear as indexer started.

Print help:
```bash
>> h
```

Add directory:
```bash
>> add ../somePathRelativeToSimpleindexer
```

Remove directory:
```bash
>> rm ../somePathRelativeToSimpleindexer
```

List all files than contain word "example":
```bash
>> find example
```

Note: do not add src/ path to indexer when loggin is switched-on.
