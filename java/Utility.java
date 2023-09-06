import java.io.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.lang.management.ManagementFactory;

public class Utility {
	private static final String ICONPATH = FileExplorer.getIconPath();

	public static ImageIcon chooseIcon(File file, int size) {
		ImageIcon img=null;
		Set<String> set = new HashSet<>();
		String name = file.getName(), path="";
		String extension = getExtension(file.getName());

		if(name.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(file);
			
			path = ICONPATH + "other/harddiskfolder.png";

			if(description!=null) {
				if(description.equals("CD Drive")) {
					path = ICONPATH + "other/cdfolder.png";
				}
				else if(description.equals("DVD Drive")) {
					path = ICONPATH + "other/dvd.png";
				}
				else if(description.equals("USB Drive")) {
					path = ICONPATH + "other/usbfolder.png";			
				}
			}

			img = ImageHandler.getImageFast(path, size, size, true);
			return img;
		}

		// Bad check for images
		set.add("jpeg");
		set.add("jpg");
		set.add("png");
		set.add("gif");
		if(set.contains(extension)) {
			img = ImageHandler.getImageFast(file.getPath(), size, size, false);
		}
		else if(file.isDirectory()) {
			if(file.list()!=null && file.list().length==0) {
				img = ImageHandler.getImageFast(ICONPATH 
					+ "other/folderempty.png", size, size, true);
			}
			else {
				img = ImageHandler.getImageFast(ICONPATH 
					+ "other/folder.png", size, size, true);
			}
		}
		else if(file.isFile()) {
			img = ImageHandler.getImageFast(ICONPATH + "extensions/" + 
				getExtension(file.getName()) + ".png", size, size, true);
			
			if(img==null) {
				img = ImageHandler.getImageFast(ICONPATH 
					+ "other/question.png", size, size, true);
			}
		}

		//Image folderImg = img.getImage().getScaledInstance(150, 60, 
		//		Image.SCALE_DEFAULT);

		/* You get small resolution system icons. Waiting for official better 
		way	Icon icon;

		if(!iconSet.contains(extension) && iconName!="folder.png") {
			icon = FileSystemView.getFileSystemView().getSystemIcon(file);
			folderImg = iconToImage(icon).getScaledInstance(60, 60, 
				Image.SCALE_DEFAULT);
		}
		*/

		return img;
	}

	public static String getExtension(String fileName) {
		String extension = "";

		int i = fileName.lastIndexOf('.');
		int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

		if (i > p) {
			extension = fileName.substring(i+1);
		}

		return extension;
	}

	public static String convertBytes(long bytes, boolean show_bytes) {
		long fileSizeInKB=0, fileSizeInMB=0, fileSizeInGB=0;
		if(bytes!=0) {
			fileSizeInKB = bytes / 1024;
			fileSizeInMB = fileSizeInKB / 1024;
			fileSizeInGB = fileSizeInMB / 1024;
		}

		String size="";
		if(bytes!=0) {
			size = bytes + " B";
		}

		if(fileSizeInKB!=0) {
			double tempSize = (double) bytes/1024;
			size = String.format("%.2f", tempSize) + " KB";
			if(show_bytes)
				size += "  ( " + bytes + " B )";
		}
		
		if(fileSizeInMB!=0) {
			double tempSize = (double) bytes/1024/1024;
			size = String.format("%.2f", tempSize) + " MB ";
			if(show_bytes)
				size += "  ( " + bytes + " B )";
		}

		if(fileSizeInGB!=0) {
			double tempSize = (double) bytes/1024/1024/1024;
			size = String.format("%.2f", tempSize) + " GB ";
			if(show_bytes)
				size += "  ( " + bytes + " B )";
		}

		return size;
	}

	public static long[] folderSize(File directory) {
		long[] total = new long[3];
		total[0] = 0; //length
		total[1] = 0; //files
		total[2] = 0; //folders

		try {
			for (File file : directory.listFiles()) {
				if(!Files.isSymbolicLink(file.toPath())) {
					if(file.isFile()) {
						total[0] += file.length();
						total[1] += 1;
					}
					else {
						long[] temp = folderSize(file);
	
						total[0] += temp[0];
						total[1] += temp[1];
						total[2] += temp[2] + 1;
					}
				}
			}
		}
		catch(Exception e) {
			return total;
		}

		return total;
	}

	public static String hashStream(BufferedInputStream bis, String type) {
		byte[] buffer= new byte[4096];
		int count;

		try {
			MessageDigest digest = MessageDigest.getInstance(type);
			
			while ((count = bis.read(buffer)) > 0) {
				digest.update(buffer, 0, count);
			}
			bis.close();

			byte[] hash = digest.digest();
	
			// Conver hash to hex string
			char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

			char[] hexChars = new char[hash.length * 2];
			for (int j = 0; j < hash.length; j++) {
				int v = hash[j] & 0xFF;
				hexChars[j * 2] = HEX_ARRAY[v >>> 4];
				hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
			}
			return new String(hexChars);
		}
		catch(Exception exc) {

		}

		return null;
	}

	public static String hashFile(File file, String type) {

		if(file.isDirectory()) {
			File[] files = file.listFiles();
			if(files == null)
				return "";
			
			String total = "";

			Arrays.sort(files);

			for(File current : files)
				total += hashFile(current, type);

			InputStream stream = new ByteArrayInputStream(
				total.getBytes(StandardCharsets.UTF_8));

			BufferedInputStream bis = new BufferedInputStream(stream);
			return hashStream(bis, type);
		}
		else {
			try {
				BufferedInputStream bis = new BufferedInputStream(new 
					FileInputStream(file));

				return hashStream(bis, type);
			}
			catch(Exception e) {

			}
		}
		return "";
	}
}