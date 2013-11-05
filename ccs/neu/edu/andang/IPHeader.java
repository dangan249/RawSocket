package ccs.neu.edu.andang;

public class IPHeader {
	private static final byte[] byteArray = new byte[] { (byte)0x45, (byte)0x00, (byte)0x00, (byte)0x3c,  (byte)0x1c, (byte)0x46, (byte)0x40, (byte)0x00, (byte)0x40, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0xac, (byte)0x10, (byte)0x0a, (byte)0x63, (byte)0xac, (byte)0x10, (byte)0x0a, (byte)0x0c };
	private int checksum;
	

	public int getChecksum() {
		return checksum;
	}

	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}

	
	public static void main(String args[]){
		IPHeader ipHeader = new IPHeader();
		ipHeader.generateChecksum(byteArray);
	}
	
	private void generateChecksum(byte[] byteArray) {
		int sum = 0; 
		
		// if the byte array has odd number of octets, padding a zero byte
		byte[] stream ;
		if (byteArray.length % 2 != 0) {
			stream = new byte[byteArray.length+1];
			for (int i=0; i< byteArray.length; i++) {
				stream[i] = byteArray[i];
			}
			stream[byteArray.length] = 0;
			System.out.println(stream.length);
		} else {
			stream = new byte[byteArray.length];
			for (int i=0; i< byteArray.length; i++)
				stream[i] = byteArray[i];
		}		
		

		// adjacent 8 bit words are stored as a short, 
		// sum up the 16 bit shorts and compute 1's complement for checksum
		for (int c=0; c < stream.length; c=c+2 ) {
			int firstByte = Byte.valueOf(stream[c]).intValue();
			
			// to convert it to unsigned value
			firstByte = firstByte&255;
			int shifted = (firstByte<<8);
			System.out.println("The shifted-->"+shifted +" "+Integer.toHexString(shifted));
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
		setChecksum(sum);
		System.out.println("The Checksum after one's complement-->"+Integer.toHexString(checksum));		


		
	}
}
