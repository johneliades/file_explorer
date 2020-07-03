# file_explorer

A simple file explorer written in java using Swing and DefaultMutableTreeNode aiming to achieve cross-platform functionality in a visually appealing way.
					
![Image of website](https://github.com/johneliades/file_explorer/blob/master/preview.png)

## Clone

Clone the repository locally by entering the following command:
```
git clone https://github.com/johneliades/file_explorer.git
```
Or by clicking on the green "Clone or download" button on top and then decompressing the zip.

## Compile (unnecessary)
Open a cmd in the cloned folder and enter:

```
javac java/*.java -d classes
```
to compile the java files in classes and

```
jar -cvmf MANIFEST.MF FileExplorer.jar -C classes .
```
to make a jar executable.

## Run
Double click the FileExplorer.jar file (Note: icons folder must be in the same folder as the jar).

OR

Open a cmd in the cloned folder and enter:

```
java -cp classes FileExplorer

OR

java -jar FileExplorer.jar
```

## Compile and Run

Open a cmd in the cloned folder and enter:

```
ant (Note: Must be installed).
```

## Notes

Fully working on Windows and Unix, not tested on Mac.

## Author

**Eliades John** - *Developer* - [Github](https://github.com/johneliades)
