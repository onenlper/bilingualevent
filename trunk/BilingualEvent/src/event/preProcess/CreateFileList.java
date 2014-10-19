package event.preProcess;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import util.Common;
import util.Util;

public class CreateFileList {
	
//	public static void main(String args[]) throws Exception{
//		if(args.length!=1) {
//			System.out.println("java ~ [folder]");
//		}
//		Util.part = args[0];
//		
//		int folder = Integer.valueOf(args[0]);
//		System.out.println("Creat File List...\n====================");
//		String baseFolderStr = "/users/yzcchen/chen3/coling2012/LDC2006T06/data/";
//		File baseFolder = new File(baseFolderStr);
//		for(File languageFolder : baseFolder.listFiles()) {
//			String language = languageFolder.getName();
//			ArrayList<String> filenames_train = new ArrayList<String>();
//			ArrayList<String> filenames_development = new ArrayList<String>();
//			ArrayList<String> filenames_test = new ArrayList<String>();
//			
//			ArrayList<String> allFilenames = new ArrayList<String>();
//			for(File subFolder : languageFolder.listFiles()) {
//				String finalFolder = subFolder.getAbsolutePath() + File.separator;
//				if(language.equals("English")) {
//					finalFolder += "timex2norm" + File.separator;
//				} else {
//					finalFolder += "adj" + File.separator;
//				}
//				for(File file : new File(finalFolder).listFiles()) {
//					String fn = file.getAbsolutePath();
//					if(fn.endsWith(".sgm")) {
//						allFilenames.add(fn.substring(0, fn.length()-4));
//					}
//				}
//			}
//			int filesize = allFilenames.size();
//			for(int i=0;i<filesize;i++) {
//				String filename = allFilenames.get(i);
//				if(i>=filesize*0.1*(folder-1) && i<=filesize*0.1*folder) {
//					filenames_test.add(filename);
//				}
////				else if(i>filesize*0.1*(folder%10) && i<=filesize*0.15*(folder%10)){
////					filenames_development.add(filename);
////				}
//				else {
//					filenames_train.add(filename);
//				}
//			}
//			Common.outputLines(filenames_train, "ACE_" + language+"_train"+Util.part);
//			Common.outputLines(filenames_development, "ACE_" + language+"_development"+Util.part);
//			Common.outputLines(filenames_test, "ACE_" + language+"_test"+Util.part);
//		}
//	}
	
	
	public static void main(String args[]) throws Exception{
		if(args.length!=1) {
			System.out.println("java ~ [folder]");
		}
		Util.part = args[0];
		
		int folder = Integer.valueOf(args[0]);
		System.out.println("Creat File List...\n====================");
		String baseFolderStr = "/users/yzcchen/chen3/coling2012/LDC2006T06/data/";
		File baseFolder = new File(baseFolderStr);
		for(File languageFolder : baseFolder.listFiles()) {
			String language = languageFolder.getName();
			ArrayList<String> filenames_train = new ArrayList<String>();
			ArrayList<String> filenames_development = new ArrayList<String>();
			ArrayList<String> filenames_test = new ArrayList<String>();
			for(File subFolder : languageFolder.listFiles()) {
				String finalFolder = subFolder.getAbsolutePath() + File.separator;
				if(language.equals("English")) {
					finalFolder += "timex2norm" + File.separator;
				} else {
					finalFolder += "adj" + File.separator;
				}
				FilenameFilter filter = new OnlyExt("sgm");
				int filesize = new File(finalFolder).list(filter).length;
				int count = 0;
				for(File file : new File(finalFolder).listFiles()) {
					String fn = file.getAbsolutePath();
					if(fn.endsWith(".sgm")) {
						count++;
//						if(count>=filesize*0.1*(folder-1) && count<=filesize*0.1*folder) {
//							filenames_test.add(fn.substring(0, fn.length()-4));
//						}
////						else if(count>=filesize*0.40 && count<filesize*0.45){
////							filenames_development.add(fn.substring(0, fn.length()-4));
////						}
//						else {
//							filenames_train.add(fn.substring(0, fn.length()-4));
//						}
						if(count>=filesize*0.1*(folder-1) && count<=filesize*0.1*folder) {
							filenames_test.add(fn.substring(0, fn.length()-4));
						}
						else {
//							int low = (int)(filesize*0.1*folder);
//							int up = (int)(filesize*0.1*(folder+1));
//							if(folder==10) {
//								low = 0;
//								up = (int)(filesize*0.1);
//							}
//							if(count>=low && count<up) {
//								filenames_test.add(fn.substring(0, fn.length()-4));
//							} else {
								filenames_train.add(fn.substring(0, fn.length()-4));
//							}
						}
					}
				}
			}
			Common.outputLines(filenames_train, "ACE_" + language+"_train"+Util.part);
			Common.outputLines(filenames_development, "ACE_" + language+"_development"+Util.part);
			Common.outputLines(filenames_test, "ACE_" + language+"_test"+Util.part);
		}
	}
	
	
	public static class OnlyExt implements FilenameFilter {
		
		String ext;
		
		public OnlyExt(String ext) {
			this.ext = "." + ext;
		}
		
		@Override
		public boolean accept(File arg0, String arg1) {
			return arg1.endsWith(ext);
		}
		
	}
}


