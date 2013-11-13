package ccs.neu.edu.andang;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;

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


    // return the index where the data portion of a packet start
    public  static int getDataIndex(TCPHeader header , int tcpHeaderStartIndex ){
        return tcpHeaderStartIndex + header.getHeaderLength() * 4 ;
    }

    // parsing a URL string and give back the corresponding InetAddress object
    public static InetAddress getIPAddress( String host ) throws UnknownHostException, MalformedURLException {
        return InetAddress.getByName( host );
    }

    // Strategy: pick a random port from [49152,65535]
    // use ServerSocket to verify that it iss open
    public static int getAvailablePort() {
        int port = 0;
        try {
            do {
                port = (new Random()).nextInt(65535 - 49152 + 1) + 49152;
            } while (!isPortAvailable(port));
        } catch (IOException e) {
            System.out.println( e.toString() ) ;
        }

        return port;
    }

    // return the pseudo header needed to calculate  checksum
    public static byte[] getChecksumData( TCPPacket packet , InetAddress srcAddress,
                                   InetAddress dstAddress ){

        byte[] sourceAddress = srcAddress.getAddress() ;
        byte[] destAddres = dstAddress.getAddress() ;
        byte[] reserved  = new byte[]{ (byte) 0 };
        byte[] protocol  = new byte[]{ (byte) 6 };

        short tcpSegmentLength = (short) packet.length() ;

        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort(tcpSegmentLength);

        ByteArrayOutputStream out = new ByteArrayOutputStream( );

        try{
            out.write( sourceAddress );
            out.write( destAddres );
            out.write( reserved ) ;
            out.write( protocol );
            out.write( b.array() );
            out.write( packet.toByteArray() ) ;
        }
        catch(IOException ex){
            System.out.println( ex.toString() ) ;
        }
        return out.toByteArray();
    }

    // return true if the checksum in the receiving header
    public static boolean verifyChecksum( TCPPacket packet , InetAddress srcAddress,
                                   InetAddress dstAddress ){
        return packet.getHeader().getChecksum()
                == Util.generateChecksum(
                          getChecksumData( packet , srcAddress, dstAddress) ) ;
    }

    // return external IP address of this machine, which hosts the program
    public static InetAddress getSourceExternalIPAddress(){
        InetAddress result = null ;
        try{

            Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface anInterface : Collections.list(allInterfaces)) {

                Enumeration addresses = anInterface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress ia= (InetAddress) addresses.nextElement();

                    if(! ia.isLoopbackAddress() )
                        if( ia instanceof Inet4Address){
                            result = ia ;
                        }
                }

            }
        }

        catch ( SocketException e) {
            System.out.println( e.toString() ) ;
        }

        return result;
    }

    // return true if the given port is available
    private static boolean isPortAvailable( int port ) throws IOException {

        ServerSocket sock = null;
        try {
            sock = new ServerSocket(port);
            sock.setReuseAddress(true);
            return true;
        } catch ( IOException e) {
            System.out.println( e.toString() ) ;
        } finally {
            if (sock != null) {
                sock.close();
            }
        }

        return false;
    }

}
