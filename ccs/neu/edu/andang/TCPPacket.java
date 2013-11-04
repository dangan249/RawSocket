package ccs.neu.edu.andang ;

// TODO
// TCPPacket: represent a TCP  packet
public class TCPPacket{

	TCPHeader header ;
	private byte[] data ;

	public TCPPacket( TCPHeader header ){
		this.header = header ; 
	}



}