package ccs.neu.edu.temp ;

import java.nio.ByteBuffer ;
import java.util.Arrays ;
// TODO
// TCPHeader: represent the header of a TCP packet
// it also allow packets with optional fields
public class TCPHeader{

	private final int BASE_HEADER_SIZE = 20 ;

	int source_port;
	int destination_port;
	long seq_num;
	long ack_num;
	byte data_offset;
	byte flags;
	int win_size;
	int checksum;
	int urg_point;
	
	// Checksum is calculated on pseudo header + TCP header + TCP data}
	byte [] tcpPacket = { (byte)0x45, (byte)0x00, (byte)0x00, (byte)0x3c,  (byte)0x1c, (byte)0x46, (byte)0x40, (byte)0x00, (byte)0x40, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0xac, (byte)0x10, (byte)0x0a, (byte)0x63, (byte)0xac, (byte)0x10, (byte)0x0a, (byte)0x0c };
	byte [] pseudoHeader = { (byte)0xFF, (byte)0xFF, 46, (byte)0xFF, (byte)0xFF, 45, 00, (byte)0x3c, 06 };
	
	// TODO: Parse a TCP Header for an incoming TCP packet
	public TCPHeader(){
		
	}

	public int length(){
		return BASE_HEADER_SIZE ;
	}

	// Create a TCP Header for an outgoing TCP packet
	public TCPHeader(int source_port, int destination_port, long seq_num, long ack_num, byte flags, int win_size) {
		this.source_port = source_port;
		this.destination_port = destination_port;
		this.seq_num = seq_num;
		this.ack_num = ack_num;
		this.data_offset = 5;
		this.flags = flags;
		this.win_size = win_size;
		this.checksum = 0;
		this.urg_point = 0;
		this.generateChecksum(tcpPacket, pseudoHeader);
	}

	private int generateChecksum(byte[] byteArray, byte[] psedoHeader) {
		long sum = 0; 
		
		//add TCP pseudo header containing src and dest IP addresses as 16 bit words
		int j = 0;
		for (j=0; j<4; j=j+2) {
			int srcHalfIP = (pseudoHeader[j]&255)<<8 + (pseudoHeader[j+1]);
			sum = sum + srcHalfIP;
		}
		
		for (j=4; j<8; j=j+2) {
			int destHalfIP = (pseudoHeader[j]&255)<<8 + (pseudoHeader[j+1]);
			sum = sum + destHalfIP;
		}
		
		//add protocol number (penultimate byte) and the length of TCP packet (last byte)
		int protocolNum = psedoHeader[j]; // it's protocol number from IP packet (IPV4)
		System.out.println("Protocol num-->"+protocolNum);
		int lenTCP = byteArray.length; // TCP header + data ( assuming data to be zero and header (byteArray) will be added below
		System.out.println("TCP length-->"+lenTCP);
		
		// if the byte array (TCP header) has odd number of octets, padding" a zero byte
		byte[] stream ;
		if (lenTCP % 2 != 0) {
			stream = new byte[lenTCP+1];
			for (int i=0; i< lenTCP; i++) {
				stream[i] = byteArray[i];
			}
			stream[lenTCP] = 0;
		} else {
			stream = new byte[lenTCP];
			for (int i=0; i< lenTCP; i++)
				stream[i] = byteArray[i];
		}
		//sum = sum + protocolNum + 0; (not sure if this is TCP packet size or 0)
		sum = sum + protocolNum + lenTCP;		

		// adjacent 8 bit words are stored as a short, 
		// sum up the 16 bit shorts
		for (int c=0; c < stream.length; c=c+2 ) {
			int firstByte = Byte.valueOf(stream[c]).intValue();
			
			// to convert it to unsigned value
			firstByte = firstByte&255;
			int shifted = (firstByte<<8);
			System.out.println("The shifted-->"+Integer.toHexString(shifted));
			int nextbyte = stream[c+1]&255;
			System.out.println("The next byte-->"+Integer.toHexString(nextbyte));
			int twoBytesGrouping = (shifted + (stream[c+1]&255));
			System.out.println("The exor result-->"+Integer.toHexString(twoBytesGrouping));
			sum = sum + twoBytesGrouping;
		}
		
		//adding the 17th odd bit to the checksum to keep it 16 bit word
		while (sum > 65535)
			sum = sum - 65536 + 1;		
		System.out.println("The sum in hex--> "+Long.toHexString(sum));

		//compute one's complement of sum
		sum = (~sum&0xFFFF);
		checksum = (int) sum;
		System.out.println("The Checksum after one's complement-->"+Integer.toHexString(checksum));
		return checksum;
	}

	// Generate the TCP Header in a byte array format
	public byte[] getHeader() {
		byte[] header = new byte[BASE_HEADER_SIZE];
		header[0] = (byte)((source_port>>8)&255);
		header[1] = (byte)(source_port&255);
		header[2] = (byte)((destination_port>>8)&255);
		header[3] = (byte)(destination_port&255);
		header[4] = (byte)((seq_num>>24)&255);
		header[5] = (byte)((seq_num>>16)&255);
		header[6] = (byte)((seq_num>>8)&255);
		header[7] = (byte)(seq_num&255);
		header[8] = (byte)((ack_num>>24)&255);
		header[9] = (byte)((ack_num>>16)&255);
		header[10] = (byte)((ack_num>>8)&255);
		header[11] = (byte)(ack_num&255);
		header[12] = (byte)((data_offset&15)<<4);
		header[13] = (byte)(flags&63);
		header[14] = (byte)((win_size>>8)&255);
		header[15] = (byte)(win_size&255);
		header[16] = (byte)((checksum>>8)&255);
		header[17] = (byte)(checksum&255);
		header[18] = (byte)((urg_point>>8)&255);
		header[19] = (byte)(urg_point&255);
		return header;
	}

	public int getSourcePort(){return this.source_port;}

	public int getDestinationPort(){return this.destination_port;}

	public long getSequenceNumber(){return seq_num;}

	public long getACKNumber(){return ack_num;}

	// return the number of words in the TCP header 
	// including any 'options' fields.
	public byte getHeaderLength(){return data_offset;}

	public int getWindowSize(){return win_size;}

	public int getChecksum(){return checksum;}

	public int getUrgentPointer(){return urg_point;}

	public boolean isURGFlagOn(){return (boolean)((flags&32) == 32);}

	public boolean isACKFlagOn(){return (boolean)((flags&16) == 16);}

	public boolean isPSHFlagOn(){return (boolean)((flags&8) == 8);}

	public boolean isRSTFlagOn(){return (boolean)((flags&4) == 4);}	

	public boolean isSYNFlagOn(){return (boolean)((flags&2) == 2);}

	public boolean isFINFlagOn(){return (boolean)((flags&1) == 1);}
	
	private void print() {
		byte[] head = getHeader();
		for (int j=0; j<head.length; j++) {
			System.out.format("%02X ", head[j]);
		}
		System.out.println();
	}
	
	public static void main(String args[]){
		TCPHeader test = new TCPHeader(32769, 32768, 3758096384l, 3758096385l, (byte)18, 32769);
		test.print();
		System.out.println(test.getSourcePort());
	}
}
