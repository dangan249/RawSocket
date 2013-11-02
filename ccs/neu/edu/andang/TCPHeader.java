package ccs.neu.edu.andang ;

import java.nio.ByteBuffer ;
import java.util.Arrays ;
// TODO
// TCPHeader: represent the header of a TCP packet
// it also allow packets with optional fields
public class TCPHeader{

	private byte[] baseHeader ;
	private byte[] options ;
	private	byte[] byte12And13 ;

	private final int BASE_HEADER_SIZE = 20 ;


	public TCPHeader(){
		baseHeader = new byte[20] ;
	}

	public TCPHeader( byte[] baseHeader ){
		this.setHeader( baseHeader );
		byte12And13 = getBytes( 12 , 14 ) ;
	}

	// set the headers of a TCP packet ( not including optional fields )
	public void setHeader( byte[] baseHeader ){
		
		if( baseHeader.length != BASE_HEADER_SIZE ){
			throw new IllegalArgumentException("Header size should be 20 bytes.") ;
		}

		this.baseHeader = baseHeader ;
		byte12And13 = getBytes( 12 , 14 ) ;

	}

	public void setOptions( byte[] options ){
		this.options = options ;
	}

	public short getSourcePort(){
		return (ByteBuffer.wrap( getBytes( 0 , 2 ) )).getShort() ;
	}

	// side-effect: change this.baseHeader
	public void setSourcePort( short sourcePort ){
		
		byte[] sourcePortBytes = ByteBuffer.allocate(2).putShort(sourcePort).array();
		copyBytes( sourcePortBytes , 0 , 2 ) ;

	}

	public short getDestinationPort(){
		return (ByteBuffer.wrap( getBytes( 2 , 4 ) )).getShort() ;
	}

	// side-effect: change this.baseHeader
	public void setDestinationPort( short destPort ){
		
		byte[] sourcePortBytes = ByteBuffer.allocate(2).putShort(destPort).array();
		copyBytes( sourcePortBytes , 2 , 4 ) ;

	}

	public int getSequenceNumber(){
		return (ByteBuffer.wrap( getBytes( 4 , 8 ) )).getInt() ;
	}

	// side-effect: change this.baseHeader
	public void setSequenceNumber( int sequenceNumber ){

		byte[] sourcePortBytes = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
		copyBytes( sourcePortBytes , 4 , 8 ) ;

	}

	public int getACKNumber(){
		return (ByteBuffer.wrap( getBytes( 8 , 12 ) )).getInt() ;
	}

	// side-effect: change this.baseHeader
	public void setACKNumber( int aCKNumber ){

		byte[] sourcePortBytes = ByteBuffer.allocate(4).putInt(aCKNumber).array();
		copyBytes( sourcePortBytes , 8 , 12 ) ;

	}

	// return the number of bytes in the TCP header 
	// including any 'options' fields.
	public int getHeaderLength(){

		byte length = byte12And13[0] ;
		length = (byte) (length >> 4) ;
		return (new Byte(length)).intValue() * 4 ; 

	}

	// side-effect: change this.baseHeader
	public void setHeaderLength( int length ){
		
		int numOf4ByteChunks = length / 4 ;
		copyBytes( new byte[]{ (byte) numOf4ByteChunks } ,12 , 13 ) ;
	}

	public short getWindowSize(){
		return (ByteBuffer.wrap( getBytes( 14 , 16 ) )).getShort() ;
	}

	public short getChecksum(){
		return (ByteBuffer.wrap( getBytes( 16 , 18 ) )).getShort() ;
	}

	public short getUrgentPointer(){
		return (ByteBuffer.wrap( getBytes( 18 , 20 ) )).getShort() ;
	}

	public boolean isURGFlagOn(){
		return isBitOn( byte12And13[1] , 5 ) ;
	}

	public boolean isACKFlagOn(){
		return isBitOn( byte12And13[1] , 4 ) ;		
	}


	public boolean isPSHFlagOn(){
		return isBitOn( byte12And13[1] , 3 ) ;				
	}

	public boolean isRSTFlagOn(){
		return isBitOn( byte12And13[1] , 2 ) ;				
	}	

	public boolean isSYNFlagOn(){
		return isBitOn( byte12And13[1] , 1 ) ;				
	}

	public boolean isFINFlagOn(){
		return isBitOn( byte12And13[1] , 0 ) ;				
	}

	// side-effect: change this.baseHeader
	public void setACK(){

		int newByte = (new Byte( byte12And13[1] ) ).intValue() | ( 1 << 4 );

		copyBytes( new byte[]{ (byte) newByte } ,13 , 14 ) ;		
	}

	// side-effect: change this.baseHeader
	public void setSYN(){

		int newByte = (new Byte( byte12And13[1] ) ).intValue() | ( 1 << 1 );

		copyBytes( new byte[]{ (byte) newByte } ,13 , 14 ) ;		
	}

	// side-effect: change this.baseHeader
	public void setFYN(){

		int newByte = (new Byte( byte12And13[1] ) ).intValue() | 1;

		copyBytes( new byte[]{ (byte) newByte } ,13 , 14 ) ;		
	}

	private boolean isBitOn( byte b, int position ){
		int num = ( new Byte(b) ).intValue() ;
		return (num & ( 1 << position )) != 0 ;
	}

	// get all byte of baseHeader in range [from, to)
	private byte[] getBytes( int from, int to ){
		return Arrays.copyOfRange( baseHeader, from , to )  ;
	}

	// copy all bytes of the given byte array to our base header byte array
	// in range [from, to)
	// side-effect: change this.baseHeader
	private void copyBytes( byte[] source, int from, int to ){

		copyBytes( source , this.baseHeader, from, to ) ;

	}

	// copy all bytes of the source byte array to the dest byte array 
	// in range [from, to)
	// side-effect: change "dest" 
	private void copyBytes( byte[] source, byte[] dest, int from, int to ){

		int range = to - from ;
		if( range != source.length || range > dest.length 
		 || from < 0 || to < 0 || to > dest.length ){
			throw new IllegalArgumentException("Invalid [from,to) range") ;			
		}

		for( int i = from ; i < to ; i++ ){
			dest[i] = source[i] ;
		}

	}

}
