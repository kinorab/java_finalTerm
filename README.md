# Final term

Work with [vscode](https://code.visualstudio.com/)

## Compile

>javac -encoding UTF-8 *.java

#### Compile with bat

[compileAndRun](https://github.com/kinorab/java_finalTerm/blob/master/compileAndRun.bat)(compile file and wrap to jar, and run jar)

## Wrap to jar

**Enter class**: [FinalTerm](https://github.com/kinorab/java_finalTerm/blob/master/src/FinalTerm.java)
>jar cvfe (targetName).jar [FinalTerm](https://github.com/kinorab/java_finalTerm/blob/master/src/FinalTerm.java) bin/allFiles [lang.xml](https://github.com/kinorab/java_finalTerm/blob/master/src/lang.xml) [template.xml](https://github.com/kinorab/java_finalTerm/blob/master/src/template.xml)

## Execute

>java -jar (targetName).jar

#### Execute with bat

[run](https://github.com/kinorab/java_finalTerm/blob/master/run.bat)(run the jar file)

## Config of setting.ini

### Can rewrite directly

* **lang**(*language*) = "**en\_US**"(default), "zh\_TW", "ja\_JP"

* **userFileDir**(*user file's directory*)

* **userFileName**(*user file name*)

* **backupDir**(*backup's directory*)

* **autoBackup**(*auto backup*) = "true", "**false**"(default)

* **backupKeepNumber**(*maximum of backup keeps*) = (0 ~ 30), **10**(default)

* **autoBackupIntervalTimes**(*interval times of auto backup, auto backup is false will invalid*) = (1 ~ 10), **3**(default)

* **userInterface**(*current only have text mode*) = "**text**"(default), "graphic"

* **contactDataLimit**(*limit on 128 datas*) = "**true**"(default), "false"

* **bookDataLimit**(*limit on 128 datas*) = "**true**"(default), "false"

* **workDataLimit**(*limit on 128 datas*) = "**true**"(default), "false"

## Data of user file

Encoding with UTF-8

### Tag

* **contact**(*personal contact book*)

* **book**(*personal book*)

* **work**(*personal work*)

## Backups system

* Endec with [Base64](https://en.wikipedia.org/wiki/Base64)

* Naming with [timestamp](https://en.wikipedia.org/wiki/Timestamp)
