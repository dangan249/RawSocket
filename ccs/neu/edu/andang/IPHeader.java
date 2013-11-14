package ccs.neu.edu.andang ;

// IPHeader: represent the header of an IP packet
public class IPHeader{

	private final int BASE_HEADER_SIZE = 20 ;

	byte version;
	byte ihl;
	short tos;
	int length;
	int id;
	byte flags;
	short offset;
	short ttl;
	short protocol;
	int checksum;
	byte[] source_address = new byte[4];
	byte[] destination_address = new byte[4];
	
    byte[] options ;

    public IPHeader( byte[] baseHeader ){
        if (baseHeader.length == BASE_HEADER_SIZE) {
			version = (byte)((baseHeader[0]>>4)&15);
			ihl = (byte)(baseHeader[0]&15);
			tos = (short)(baseHeader[1]&63);
			length = (int)(((baseHeader[2]<<8)&65280)|(baseHeader[3]&255));
			id = (int)(((baseHeader[4]<<8)&65280)|(baseHeader[5]&255));
			flags = (byte)((baseHeader[6]>>5)&7);
			offset = (short)(baseHeader[6]&31);
			ttl = (short)(baseHeader[8]&63);
			protocol = (short)(baseHeader[9]&63);
			checksum = (int)(((baseHeader[10]<<8)&65280)|(baseHeader[11]&255));
			source_address[0] = baseHeader[12];
			source_address[1] = baseHeader[13];
			source_address[2] = baseHeader[14];
			source_address[3] = baseHeader[15];
			destination_address[0] = baseHeader[16];
			destination_address[1] = baseHeader[17];
			destination_address[2] = baseHeader[18];
			destination_address[3] = baseHeader[19];
			}	
        else{
            throw new RuntimeException("Error in creating bash header for TCPPacket") ;
        }
    }

	// Create an IP Header for an outgoing IP packet
	public IPHeader(int length, byte[] source_address, byte[] destination_address) {
		this.version = 4;
		this.ihl = 5;
		this.tos = 0;
		this.length = length;
		this.id = 0;
		this.flags = 2;
		this.offset = 0;
		this.ttl = 64;
		this.protocol = 6;
		this.checksum = 0;
		this.source_address[0] = source_address[0];
		this.source_address[1] = source_address[1];
		this.source_address[2] = source_address[2];
		this.source_address[3] = source_address[3];
		this.destination_address[0] = destination_address[0];
		this.destination_address[1] = destination_address[1];
		this.destination_address[2] = destination_address[2];
		this.destination_address[3] = destination_address[3];
	}

	public byte[] toByteArray(){
		return getHeader() ;	
	}
	
	// Generate the TCP Header in a byte array format
	public byte[] getHeader() {
		byte[] header = new byte[BASE_HEADER_SIZE];
		header[0] = (byte)(((version&15)<<4)|(ihl&15));
		header[1] = (byte)(tos&255);
		header[2] = (byte)((length>>8)&255);
		header[3] = (byte)(length&255);
		header[4] = (byte)((id>>8)&255);
		header[5] = (byte)(id&255);
		header[6] = (byte)(((flags&7)<<5)|((offset>>8)&31));
		header[7] = (byte)(offset&255);
		header[8] = (byte)(ttl&255);
		header[9] = (byte)(protocol&255);
		header[10] = (byte)((checksum>>8)&255);
		header[11] = (byte)(checksum&255);
		header[12] = source_address[0];
		header[13] = source_address[1];
		header[14] = source_address[2];
		header[15] = source_address[3];
		header[16] = destination_address[0];
		header[17] = destination_address[1];
		header[18] = destination_address[2];
		header[19] = destination_address[3];
		return header;
	}
	
	public byte[] getOptions() {
        return options;
    }

    public void setOptions(byte[] options) {
        this.options = options;
    }

	public int getEntireLength(){return length;}
	public int getChecksum(){return checksum;}
	public void setCheckSum(int checksum){this.checksum = checksum;}
	public byte getVersion(){return version;}
	public byte getIHL(){return ihl;}
	public short getToS(){return tos;}
	public int getID(){return id;}
	public short getOffset(){return offset;}
	public short getTTL(){return ttl;}
	public short getProtocol(){return protocol;}
	
	public byte[] getSourceAddress(){return source_address;}
	public byte[] getDestinationAddress(){return destination_address;}
	
	public boolean isFragmentOn(){return (boolean)((flags&2) == 2);}
	public boolean isMoreFragmentsOn(){return (boolean)((flags&1) == 1);}

	public void print() {
		byte[] head = getHeader();
		for (int j=0; j<head.length; j++) {
			System.out.format("%02X ", head[j]);
		}
		System.out.println();
	}
}
