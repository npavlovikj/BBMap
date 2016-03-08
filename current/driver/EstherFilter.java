package driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import align2.ListNum;

import stream.ConcurrentGenericReadInputStream;
import stream.ConcurrentReadStreamInterface;
import stream.FastaReadInputStream;
import stream.Read;

import fileIO.FileFormat;
import fileIO.ReadWrite;

/**
 * @author Brian Bushnell
 * @date Jul 29, 2014
 *
 */
public class EstherFilter {
	
	public static void main(String[] args){
		String query=args[0];
		String ref=args[1];
		float cutoff=Float.parseFloat(args[2]);
		boolean outputFasta=false;
		if(args.length>3 && args[3].equalsIgnoreCase("fasta")){
			outputFasta=true;
		}
		String command="blastall -p blastn -i "+query+" -d "+ref+" -e 0.00001 -m 8";
		
		FastaReadInputStream.SPLIT_READS=false;
		
		ReadWrite.FORCE_KILL=true;

//		InputStream is=ReadWrite.getInputStreamFromProcess("stdin", command, false);
//		InputStream is=ReadWrite.getInputStreamFromProcess("", command, false);
		InputStream is=ReadWrite.getInputStreamFromProcess(null, command, false);
		
		InputStreamReader isr=new InputStreamReader(is);
		BufferedReader b=new BufferedReader(isr, 32768);
		
//		System.out.println("Before");
		
		if(outputFasta){
//			System.out.println("A");
			processToFasta(b, cutoff, query);
		}else{
//			System.out.println("B");
			processToNames(b, cutoff);
		}
		
//		System.out.println("Finished");

//		ReadWrite.finishReading(is, "stdin", true, b, isr);
//		ReadWrite.finishReading(is, "", true, b, isr);
		ReadWrite.finishReading(is, null, true, b, isr);
		
	}
	
	public static void processToFasta(BufferedReader b, float cutoff, String query){
		String s=null;
		
		ArrayList<String> names=new ArrayList<String>();
//		System.out.println("Reading line 0");
		try {
			s=b.readLine();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		System.out.println("Starting");
		String prev="";
		
		while(s!=null){
			String[] split=s.split("\t");
			float value=0;
			try {
				value=Float.parseFloat(split[11].trim());
			} catch (NumberFormatException e) {
				e.printStackTrace();
//				System.err.println("Bad line:\n"+s);
			}
			if(value>=cutoff){
				if(!prev.equals(split[0])){
					prev=split[0];
					names.add(split[0]);
				}
			}
//			System.out.println("Reading line");
			try {
				s=b.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		outputFasta(query, names);
	}
	
	public static void processToNames(BufferedReader b, float cutoff){
		String s=null;
//		System.out.println("Reading line 0");
		try {
			s=b.readLine();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		System.out.println("Starting");
		String prev="";
		while(s!=null){
			String[] split=s.split("\t");
			float value=0;
			try {
				value=Float.parseFloat(split[11].trim());
			} catch (NumberFormatException e) {
				e.printStackTrace();
//				System.err.println("Bad line:\n"+s);
			}
			if(value>=cutoff){
				System.out.println(split[0]);
			}
//			System.out.println("Reading line");
			try {
				s=b.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void outputFasta(String fname, ArrayList<String> names){
		
		Collections.sort(names);
		
		FileFormat ff=FileFormat.testInput(fname, FileFormat.FASTA, null, false, true);
		ConcurrentReadStreamInterface cris=ConcurrentGenericReadInputStream.getReadInputStream(-1L, false, false, ff, null);
		Thread cristhread=new Thread(cris);
		cristhread.start();
		ListNum<Read> ln=cris.nextList();
		ArrayList<Read> reads=(ln!=null ? ln.list : null);
		
		/* Iterate through read lists from the input stream */
		while(reads!=null && reads.size()>0){
			
			for(Read r : reads){
				if(Collections.binarySearch(names, r.id)>=0){
					System.out.println(r.toFasta(70));
				}
			}
			
			/* Dispose of the old list and fetch a new one */
			cris.returnList(ln, ln.list.isEmpty());
			ln=cris.nextList();
			reads=(ln!=null ? ln.list : null);
		}
		/* Cleanup */
		cris.returnList(ln, ln.list.isEmpty());
	}
	
	
}