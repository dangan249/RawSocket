package ccs.neu.edu.andang;

public class Util{

	public static int generateChecksum(byte[] byteArray) {

		int sum = 0; 
		
		// if the byte array has odd number of octets, padding a zero byte
		byte[] stream ;
		if (byteArray.length % 2 != 0) {
			stream = new byte[byteArray.length+1];
			for (int i=0; i< byteArray.length; i++) {
				stream[i] = byteArray[i];
			}
			stream[byteArray.length] = 0;
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
			int nextbyte = stream[c+1]&255;
			int twoBytesGrouping = (shifted + (stream[c+1]&255));
			sum = sum + twoBytesGrouping;
		}
		
		//adding the 17th odd bit to the checksum to keep it 16 bit word
		while (sum > 65535)
			sum = sum - 65536 + 1;
		
		//compute one's complement of sum
		sum = (~sum&0xFFFF);		
		
		return sum ;
	}
}
