package ccs.neu.edu.andang;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class IPPacket {
	private IPHeader IPheader ;
	private TCPPacket TCPpacket ;

	
	public IPPacket( IPHeader IPheader, TCPPacket TCPpacket ){
		this.IPheader = IPheader;
		this.TCPpacket = TCPpacket ; 
	}
	// convert the entire IP packet (data + header) to a byte array
	public byte[] toByteArray(){

		byte[] IPheaderBytes = IPheader.toByteArray() ;
		byte[] TCPpacketBytes = TCPpacket.toByteArray();
		ByteArrayOutputStream out = new ByteArrayOutputStream( );

		try{
			out.write( IPheaderBytes ) ;
			out.write(TCPpacketBytes);		
		}
		catch(IOException ex){
			System.out.println( ex.toString() ) ;
		}
		return out.toByteArray() ;
	}
	
	

	public int length(){
		return ( IPheader.length + TCPpacket.length() );
	}
	public IPHeader getIPheader() {
		return IPheader;
	}
	public void setIPheader(IPHeader iPheader) {
		IPheader = iPheader;
	}
	public TCPPacket getTCPpacket() {
		return TCPpacket;
	}
	public void setTCPpacket(TCPPacket tCPpacket) {
		TCPpacket = tCPpacket;
	}

}
