### Simple Indexer ###

Simple implementation of concurrent [inverted indexer](http://en.wikipedia.org/wiki/Inverted_index).

### Getting started ###

* Requarements:
    * JDK 1.7 or higher
    * maven 3.1.1 or higher

* Dependencies
    * trove4j
    * slf4j-log4j12
    * apache commons-lang
    * apache commons-io

* How to build
    * `git clone https://github.com/smolcoder/simpleindexer.git`
    * `cd simpleindexer`
    *  `mvn clean package`
    * in **build** directory will appear **simpleindexer.jar**

### Example ###
```java
WordToPathIndex index = new WordToPathIndex(FileSystems.getDefault());
String pathToIndexerSrc = System.getProperty("user.dir");
index.startWatch(Paths.get(pathToIndexerSrc, "src"));
out(index.getPathsByWord("public"));
index.stopWatch(Paths.get(pathToIndexerSrc, "src/main/java/simpleindexer/fs"));
out(index.getPathsByWord("public"));
index.shutdown();
```
