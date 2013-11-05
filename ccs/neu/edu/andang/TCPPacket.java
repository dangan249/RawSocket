package ccs.neu.edu.andang ;

// TODO
// TCPPacket: represent a TCP  packet
public class TCPPacket{

	TCPHeader header ;
	private byte[] data ;

	public TCPPacket( TCPHeader header ){
		this.header = header ; 
	}

	// convert the entire packet (data + header) to a byte array
	public byte[] toByteArray(){
		return header.toByteArray() ;
	}

  	public long calculateChecksum(byte[] buf) {
	    int length = buf.length;
	    int i = 0;

	    long sum = 0;
	    long data;

	    // Handle all pairs
	    while (length > 1) {
	      // Corrected to include @Andy's edits and various comments on Stack Overflow
	      data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
	      sum += data;
	      // 1's complement carry bit correction in 16-bits (detecting sign extension)
	      if ((sum & 0xFFFF0000) > 0) {
	        sum = sum & 0xFFFF;
	        sum += 1;
	      }

	      i += 2;
	      length -= 2;
	    }

	    // Handle remaining byte in odd length buffers
	    if (length > 0) {
	      // Corrected to include @Andy's edits and various comments on Stack Overflow
	      sum += (buf[i] << 8 & 0xFF00);
	      // 1's complement carry bit correction in 16-bits (detecting sign extension)
	      if ((sum & 0xFFFF0000) > 0) {
	        sum = sum & 0xFFFF;
	        sum += 1;
	      }
	    }

	    // Final 1's complement value correction to 16-bits
	    sum = ~sum;
	    sum = sum & 0xFFFF;
	    return sum;

  	}

}


