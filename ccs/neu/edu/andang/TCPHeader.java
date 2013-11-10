package ccs.neu.edu.andang ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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


    byte[] options ;

    public TCPHeader( byte[] baseHeader ){

        if (baseHeader.length == BASE_HEADER_SIZE) {
            source_port = ( ( ( baseHeader[0] << 8 ) & 65280 ) | ( baseHeader[1 ] & 255 ));

            destination_port = (int)(((baseHeader[2]<<8)&65280)|(baseHeader[3]&255));

            seq_num = (long)(((baseHeader[4]<<24)&4278190080l)
                    |((baseHeader[5]<<16)&16711680l)
                    |((baseHeader[6]<<8)&65280)
                    |(baseHeader[7]&255));

            ack_num = (long)(((baseHeader[8]<<24)&4278190080l)
                    |((baseHeader[9]<<16)&16711680l)
                    |((baseHeader[10]<<8)&65280)
                    |(baseHeader[11]&255));

            data_offset = (byte)((baseHeader[12]>>4)&15);
            flags = (byte)(baseHeader[13]&63);
            win_size = (int)(((baseHeader[14]<<8)&65280)|(baseHeader[15]&255));
            checksum = (int)(((baseHeader[16]<<8)&65280)|(baseHeader[17]&255));
            urg_point = (int)(((baseHeader[18]<<8)&65280)|(baseHeader[19]&255));
        }
        else{
            throw new RuntimeException("Error in creating bash header for TCPPacket") ;
        }
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
		this.data_offset = BASE_HEADER_SIZE / 4 ;
		this.flags = flags;
		this.win_size = win_size;
		this.checksum = 0;
		this.urg_point = 0;
	}

	public byte[] toByteArray(){

        ByteArrayOutputStream out = new ByteArrayOutputStream( );
        try {
            out.write( getHeader() );
            if ( options != null ){
                out.write( this.options );
            }
        } catch (IOException e) {
            System.out.println( e.toString() ) ;
        }

		return out.toByteArray()  ;
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

    public byte[] getOptions() {
        return options;
    }

    public void setOptions(byte[] options) {
        this.options = options;
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

	public void setCheckSum(int checksum){this.checksum = checksum;}

	public int getUrgentPointer(){return urg_point;}

	public boolean isURGFlagOn(){return (boolean)((flags&32) == 32);}

	public boolean isACKFlagOn(){return (boolean)((flags&16) == 16);}

	public boolean isPSHFlagOn(){return (boolean)((flags&8) == 8);}

	public boolean isRSTFlagOn(){return (boolean)((flags&4) == 4);}	

	public boolean isSYNFlagOn(){return (boolean)((flags&2) == 2);}

	public boolean isFINFlagOn(){return (boolean)((flags&1) == 1);}

	public void print() {
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
