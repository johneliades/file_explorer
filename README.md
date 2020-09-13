# File Explorer

A simple file explorer written in java using Swing and DefaultMutableTreeNode aiming to achieve cross-platform functionality in a visually appealing way.
					
![Image of website](https://github.com/johneliades/file_explorer/blob/master/preview.png)

## Clone

Clone the repository locally by entering the following command:
```
git clone https://github.com/johneliades/file_explorer.git
```
Or by clicking on the green "Clone or download" button on top and then decompressing the zip.

## Compile and Run

Just double clicking the FileExplorer.jar should work. In case it doesn't, make sure java is installed.

If a different version of java is installed then a compilation will be necessary:

Open a terminal in the cloned folder and enter:

```
ant
```
ant compiles and then executes the application. 

(Note: ant must be installed)
```
sudo apt-get update
sudo apt-get install ant
```

OR

compilation:
```
javac java/*.java -d classes
```

creation of jar:
```
jar -cvmf MANIFEST.MF FileExplorer.jar -C classes .
```

Now double clicking the FileExplorer.jar should finally open the File Explorer.

## Notes

Fully working on Windows and Unix, not tested on Mac.

## Author

**Eliades John** - *Developer* - [Github](https://github.com/johneliades)
