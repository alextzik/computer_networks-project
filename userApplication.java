/* Alexandros Tzikas (Αλέξανδρος Τζήκας)
 * alextzik@ece.auth.gr
 * AEM:8978
 * 7ο Εξάμηνο - Τομέας Τηλεπικοινωνιών
 * Δίκτυα Υπολογιστών Ι

Ο κώδικας της εργασίας βρίσκεται παρακάτω. Στις γραμμές 24-32 βρίσκονται οι τελευταίοι χρησμιποιημένοι κωδικοί, από το session στις 13/11/2018 @ 22:51
Το session αυτό έγινε ως επαλήθευση και δεν συγκαταλέγεται στα session της αναφοράς, εκτός από ένα σχήμα για το GPS που έχει συμπεριληφθεί ως επιπλέον υλικό στο session2.pdf
*/
import java.io.*;
import java.util.*;
import java.lang.Integer;
import java.util.*;
import java.lang.System;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.jws.WebParam.Mode;
import ithakimodem.Modem;
import java.nio.file.Files;

public class userApplication {
	
	//The necessary variables-codes given with each session
	//The codes request_code must terminate with the characters \r
	private static String echo_request_code="E3633\r";
	private static String image_request_code_ErrorFree="M6010\r"; //When sent, follow with the code CAM=FIX or CAM=PTZ to specify camera
											  			//Add DIR=X to CAM=PTZ (that camera), to specify direction of motion of the camera (X=L, R, U, D)
	private static String image_request_code_withErrors="G9323\r";
	private static String gps_request_code="P3386";
	private static String gps_request_code_withCourse="P3386R=1062099\r";
	private static String burst_request_code;
	private static String ACK_code="Q0316\r";
	private static String NACK_code="R1493\r";
	
	private static int speed=80000;
	private static int timeout=900;
	private static Modem modem;
	
	
	
	
	private static Modem SetUp() {
		modem=new Modem();
		modem.setSpeed(speed);
		modem.setTimeout(timeout);
		modem.open("ithaki");
		int k;
		for (;;) {
			k=modem.read();
			if (k!=-1) {
				System.out.print((char) k);
			}
			else {
				break;
			}
		}
		
		return modem;
	}
	
	private static void CloseModem(Modem mymodem) {
		mymodem.close();
	}
	
	private static void ReceiveEchoPackets(Modem mymodem) throws IOException {
		long timeLimit=5*60000;
		long start=System.currentTimeMillis();
		File file=new File("/Users/AlexandrosTzikas/Desktop/Computer-Networks-I/Session-1/echoPackets.txt");
		file.createNewFile();
		FileWriter EchoPacketWriter = new FileWriter(file);
		int numOfEchoPackets=0;
		int count=0;
		long timeOfPacket=0;
		ArrayList<Long> responseTime=new ArrayList<Long>();
		int incoming;
		while (System.currentTimeMillis()-start<timeLimit) {
			mymodem.write(echo_request_code.getBytes());
			timeOfPacket=System.currentTimeMillis();
			for(;;) {
				count+=1;
				//If incoming=mymodem.read() was placed here, after the reading of the 35 characters of each package
				//we would read for one more time (36th). Since the packages don't have a 36th character the read function would have to wait for the duration of the timeout for a character that would never arrive
				//,before reading the next package. This speeds up the process.
				if(count<=35) {
					incoming=mymodem.read();
					EchoPacketWriter.write((char) incoming);
					System.out.print((char) incoming);
				}
				else {
					timeOfPacket=System.currentTimeMillis()-timeOfPacket;
					responseTime.add(timeOfPacket);
					timeOfPacket=0;
					count=0;
					break;
				}
			}
			
			numOfEchoPackets+=1;
			System.out.println(numOfEchoPackets);
			EchoPacketWriter.write("\r\n");
		}
		System.out.println(responseTime);
		EchoPacketWriter.flush();
		EchoPacketWriter.close();
	}
	
	private static void ReceiveFrameErrorFree(Modem mymodem) throws IOException{
		File image_without_errors=new File("/Users/AlexandrosTzikas/Desktop/Computer-Networks-I/Session-1/image_ErrorFree.jpg");
		image_without_errors.createNewFile();
		FileOutputStream streamWithoutErrors = new FileOutputStream(image_without_errors);
		int firstSent, secondSent;
		boolean start=false; //transmittance not started yet
		String image_request_code="";
		image_request_code=image_request_code_ErrorFree.substring(0, 5)+"CAM=03SIZE=L\r";//or +"CAM=PTZDIR=U\r"
		//System.out.println(image_request_code);
		try {
			mymodem.write(image_request_code_ErrorFree.getBytes());
			for(;;) {
				firstSent=mymodem.read();
				secondSent=mymodem.read();
				System.out.print(firstSent+" "+secondSent+", ");
				if (firstSent==-1) {
					break;
				}
				if (secondSent==-1){
					streamWithoutErrors.write(firstSent);
					break;
				}
				if(firstSent==255 && secondSent==216 || start==true) {
					streamWithoutErrors.write(firstSent);
					streamWithoutErrors.write(secondSent);
					if(start==false) {
						start=true;
					}
				}
				if(firstSent==255 && secondSent==217) {
					streamWithoutErrors.write(firstSent);
					streamWithoutErrors.write(secondSent);
					start=false;
					break;
				}
			}
			streamWithoutErrors.flush();
			streamWithoutErrors.close();
			System.out.println("Error-Free Image Printed");
			
		}
		catch (Exception e) {
			System.out.println("Exception occcured");
		}
	}
	
	private static void ReceiveFramewithErrors(Modem mymodem) throws IOException{
		File imageWithErrors=new File("/Users/AlexandrosTzikas/Desktop/Computer-Networks-I/Session-1/image_withErrors.jpg");
		imageWithErrors.createNewFile();
		FileOutputStream streamWithErrors = new FileOutputStream(imageWithErrors);
		int firstSent, secondSent;
		boolean start=false; //transmittance not started yet
		try {
			mymodem.write(image_request_code_withErrors.getBytes());
			for(;;) {
				firstSent=mymodem.read();
				secondSent=mymodem.read();
				System.out.print(firstSent+" "+secondSent+", ");
				if (firstSent==-1) {
					break;
				}
				if (secondSent==-1){
					streamWithErrors.write(firstSent);
					break;
				}
				if(firstSent==255 && secondSent==216 || start==true) {
					streamWithErrors.write(firstSent);
					streamWithErrors.write(secondSent);
					if(start==false) {
						start=true;
					}
				}
				if(firstSent==255 && secondSent==217) {
					streamWithErrors.write(firstSent);
					streamWithErrors.write(secondSent);
					start=false;
					break;
				}
			}
			streamWithErrors.flush();
			streamWithErrors.close();
			System.out.println("Error-Half Image Printed");
			
		}
		catch (Exception e) {
			System.out.println("Exception occcured");
		}
	}
	
	private static void ReceiveGPSLocation(Modem mymodem) throws IOException{
		File GPSImage=new File("/Users/AlexandrosTzikas/Desktop/Computer-Networks-I/Session-1/GPSImage3.jpg");
		GPSImage.createNewFile();
		FileOutputStream GPSstream = new FileOutputStream(GPSImage);
		mymodem.write(gps_request_code_withCourse.getBytes());
		int incoming;
		int secondsApart=12;
		int numOfSamples=9;
		String packet="";
		String[] latitudes = new String[numOfSamples];
		String[] longtitudes = new String[numOfSamples];
		
		for (;;){
			incoming=mymodem.read();
			if (incoming==-1){
				break;
			}
			System.out.print((char) incoming);
			packet=packet+(char)incoming;
		}
		
		String[] splitParts=packet.split("\n");
		String[][] splitSections=new String[splitParts.length][splitParts[1].split(",").length];
		splitSections[0]=splitParts[0].split(",");
		int index=0;
		for (int i=1; i<1+secondsApart*numOfSamples; i+=secondsApart){
			splitSections[i]=splitParts[i].split(",");
			latitudes[index]=splitSections[i][2];
			longtitudes[index]=splitSections[i][4];
			System.out.println(latitudes[index]+" "+longtitudes[index]);
			index+=1;
		}
		String T=gps_request_code;
		for (int i=0; i<latitudes.length; i++){
			int LongSecs=(int)(0.006*Integer.parseInt(longtitudes[i].substring(6,  10)));
			int LatSecs=(int) (0.006*Integer.parseInt(latitudes[i].substring(5,  9)));
			String LongSecsS=Integer.toString(LongSecs);
			String LatSecsS=Integer.toString(LatSecs);
			System.out.println(LatSecs+" "+LongSecs);
			T=T+"T="+longtitudes[i].substring(1,  5)+LongSecsS+latitudes[i].substring(0,  4)+LatSecsS;
		}
		T=T+"\r";
		System.out.print(T);
		
		int firstSent, secondSent;
		boolean start=false; //transmittance not started yet
		try {
			mymodem.write(T.getBytes());
			for(;;) {
				firstSent=mymodem.read();
				secondSent=mymodem.read();
				//System.out.print(firstSent+" "+secondSent+", ");
				if (firstSent==-1) {
					break;
				}
				if (secondSent==-1){
					GPSstream.write(firstSent);
					break;
				}
				if(firstSent==255 && secondSent==216 || start==true) {
					GPSstream.write(firstSent);
					GPSstream.write(secondSent);
					if(start==false) {
						start=true;
					}
				}
				if(firstSent==255 && secondSent==217) {
					GPSstream.write(firstSent);
					GPSstream.write(secondSent);
					start=false;
					break;
				}
			}
			GPSstream.flush();
			GPSstream.close();
			System.out.println("GPS Image Printed");
		}
		catch (Exception e) {
			System.out.println("Exception occcured");
		}
	}
	
	private static void ARQMechanism(Modem mymodem) throws IOException{
		long timeLimit=5*60000;
		long start=System.currentTimeMillis();
		File file=new File("/Users/AlexandrosTzikas/Desktop/Computer-Networks-I/Session-1/ARQPackets.txt");
		file.createNewFile();
		FileWriter ARQWriter = new FileWriter(file);
		int incoming;
		int counter=0;
		int numOfARQPackets=0;
		long timeOfPacket=0;
		int numOfResendings=0;
		ArrayList<Long> responseTime=new ArrayList<Long>();
		ArrayList<Integer> Resendings=new ArrayList<Integer>();
		String packet="";
		String seq="";
		int fcs=0;
		int k=2;
		int codexor;
		while (System.currentTimeMillis()-start<timeLimit) {
			mymodem.write(ACK_code.getBytes());
			timeOfPacket=System.currentTimeMillis();
			for(;;) {
				incoming=mymodem.read();
				counter+=1;
				if(counter<=58) {
					packet=packet+(char)incoming;
					System.out.print((char) incoming);
					if (counter>=32 && counter<=47){
						seq=seq+(char)incoming;
					}
					if (counter>=50 && counter<=52){
						String s=(char) incoming+"";
						fcs=fcs+Integer.parseInt(s)*((int)Math.pow(10, k));
						k--;
					}
					
					if(counter==58){
						codexor=seq.charAt(0)^seq.charAt(1);
						for (int i=2; i<seq.length(); i++){
							codexor=codexor^seq.charAt(i);
						}
						System.out.println(fcs);
						if (codexor==fcs){
							ARQWriter.write("The following package was received sucessfully\r\n");
							ARQWriter.write(packet);
							ARQWriter.write("\r\n");
							timeOfPacket=System.currentTimeMillis()-timeOfPacket;
							ARQWriter.write(timeOfPacket+"\r\n");
							responseTime.add(timeOfPacket);
							Resendings.add(numOfResendings);
							packet="";
							fcs=0;
							seq="";
							counter=0;
							timeOfPacket=0;
							numOfResendings=0;
							k=2;
							numOfARQPackets+=1;
							break;
						}
						else{
							packet="";
							fcs=0;
							seq="";
							counter=0;
							numOfResendings+=1;
							k=2;
							ARQWriter.write("The following package was not received sucessfully at first\r\n");
							mymodem.write(NACK_code.getBytes());
						}
						System.out.print("\r\n");
					}
				}
				else {
					counter=0;
					break;
				}
			}
		}
		ARQWriter.write("Number of Packets: "+numOfARQPackets+" Total Time (msec): "+(System.currentTimeMillis()-start)+"\r\n");
		ARQWriter.flush();
		ARQWriter.close();
		System.out.println(Resendings);
		System.out.println(responseTime);
	}
	
	public static void main(String[] args) throws IOException{
		System.out.println("In progress...");
		Modem modem=SetUp();
		ReceiveEchoPackets(modem);
		ReceiveFrameErrorFree(modem);
		ReceiveFramewithErrors(modem);
		ReceiveGPSLocation(modem);
		ARQMechanism(modem);
		CloseModem(modem);
	}
	
	
	
}
