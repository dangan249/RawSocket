package ccs.neu.edu.andang ;

// Programmer: An Dang

// Util 
import java.io.InputStream;
import java.net.*;
import java.util.Random ;
import java.util.Collections ;
import java.util.Enumeration;
import java.io.ByteArrayOutputStream ;
import java.nio.ByteBuffer ;
import java.util.Arrays ;

// Exceptions:
import java.io.IOException ;
import java.lang.RuntimeException ;

// Networking:
import com.savarese.rocksaw.net.RawSocket;
import static com.savarese.rocksaw.net.RawSocket.PF_INET;

public class RawSocketClient{

	private RawSocket rSock ;
	private final byte URG_FLAG = (byte) 32;
	private final byte FIN_FLAG = (byte) 1;
	private final byte SYN_FLAG = (byte) 2;
	private final byte ACK_SYN_FLAG = (byte) 18;
	private final byte ACK_FLAG = (byte) 16;
	private final byte ACK_FIN_FLAG = (byte) 17;
	private final int AD_WINDOW_SIZE = 1 ;
    private final int DATA_BUFFER_SIZE = 1460 ;
    private final int IP_HEADER_SIZE = 20 ;
    private final int TCP_IP_HEADERS_MIN_SIZE = 40 ;
    private final long INITIAL_SEQUENCE_NUM = 0l ;
	private final long INITIAL_ACK_NUM = 0l ;

/* TCP functionalities supported:

Packet = IP Header + TCP Header + Data

-- Verify the checksums of incoming TCP packets
-- Generate correct checksums for outgoing packets. (done)
-- Select a valid local port to send traffic on (done)
-- Perform the three-way handshake (done)
-- Handle connection tear down.
-- Handle sequence and acknowledgement numbers. 
-- Manage the advertised window as you see fit. 
-- Include basic timeout functionality: 
==> if a packet is not ACKed within 1 minute, assume the packet is lost and retransmit it. 
-- Able to receive out-of-order incoming packets and put them back into the correct order
-- Identify and discard duplicate packets. 
-- Implement a basic congestion window: 
==> start with cwnd=1, 
==> increment the cwnd after each successful ACK, up to a fixed maximum of 1000
-- If your program observes a packet drop or a timeout, reset the cwnd to 1.

*/
    private String remoteHost ;
    private int destPort;
    private int sourcePort ;
    private InetAddress destAddress;
    private InetAddress sourceAddress ;
    private long currentSeqNum ; // the number of bytes have been sent
    private long currentACKNum ; // the number of bytes have received

    // TODO: set up the sender and receiver raw socks
    public RawSocketClient( String remoteHost, int destPort ){

        this.remoteHost = remoteHost ;
		this.destPort = destPort;
		this.sourceAddress =  Util.getSourceExternalIPAddress() ;
		this.sourcePort = Util.getAvailablePort() ;
        this.currentACKNum = INITIAL_ACK_NUM ;
        this.currentSeqNum = INITIAL_SEQUENCE_NUM ;

    }

    // TODO: handling TCP teardown process
    public void disconnect(){
		try{
			this.rSock.close() ;
		}
		catch(IOException ex){
			System.out.println( "Unable to disconnect: " + ex.toString() ) ;
		}
    }

    // TODO: connect to the remote server + doing the handshake
    public boolean connect() throws UnknownHostException, SocketException, IOException{
        this.destAddress = Util.getIPAddress( this.remoteHost ) ;

  	    if( this.destAddress.isAnyLocalAddress() || this.destAddress.isLoopbackAddress() ){
  	    	throw new RuntimeException("Internal Error") ;
  	    }
    	this.rSock = new RawSocket () ;
        this.rSock.open( PF_INET, RawSocket.getProtocolByName("tcp")) ;
        // binding a raw socket to an address causes only packets with a destination
        // matching the address to be delivered to the socket.
        // Also, the kernel will set the source address of outbound packets to the bound address
        // (unless setIPHeaderInclude(true) has been called).
        this.rSock.bind(this.sourceAddress);

        handShake() ;
  	    return true ;
    }


    // create the TCP packet and use RockSaw to send it
    // side-effect: none
    private void sendMessage(String message, long sequenceNum, long ackNum,
                             byte flags) throws IOException{

        TCPHeader header = new TCPHeader( this.sourcePort, this.destPort, sequenceNum ,
                ackNum, flags , AD_WINDOW_SIZE ) ;

        TCPPacket packet = new TCPPacket( header );

        if( message != null && ! message.isEmpty() ){
            packet.setData( message.getBytes() ) ;
        }

        int checksum = Util.generateChecksum(
                Util.getChecksumData( packet, this.sourceAddress, this.destAddress) );

        packet.getHeader().setCheckSum(checksum) ;

        this.rSock.write( this.destAddress, packet.toByteArray() ) ;

    }


    // TODO: fix the check for correct check-summed packet
    // read one incoming packet (It can be any packet, we need to filter out the correct TCP packet
    // that this program is using)
    // and verify the checksum
    private TCPPacket read(){

        byte[] responseData = new byte[DATA_BUFFER_SIZE] ;
        TCPPacket packet = null ;
        TCPHeader header = null ;
        int packetSize = 0 ;
        do{
            try {
                packetSize = this.rSock.read( responseData , sourceAddress.getAddress()) ;
            } catch (IOException e) {
                System.out.println( "Failed to read data from remote server: " + e.toString() ) ;
            }
            // get the TCP header and ignore all optional fields
            header =  new TCPHeader( Arrays.copyOfRange( responseData, IP_HEADER_SIZE, TCP_IP_HEADERS_MIN_SIZE ) );
        }
        while ( header.getDestinationPort() != this.sourcePort );  // de-multiplexing

        int dataIndex = Util.getDataIndex( header , IP_HEADER_SIZE ) ;

        // check if there are options in TCP header
        if( dataIndex > TCP_IP_HEADERS_MIN_SIZE - 1 ){
            header.setOptions( Arrays.copyOfRange( responseData, TCP_IP_HEADERS_MIN_SIZE, dataIndex ) );
        }

        packet = new TCPPacket( header );

        if( packetSize - 1 >  dataIndex ){
            //System.out.println("setting data") ;
            packet.setData( Arrays.copyOfRange( responseData, dataIndex , responseData.length ));
            this.currentACKNum += packet.getData().length + 1 ;
        }

        packet.getHeader().setCheckSum(0) ;
        // TODO: fix this
        if( Util.verifyChecksum( packet , this.sourceAddress, this.destAddress ) ){
            System.out.println( "check sum correctlly") ;
            return packet ;
        }

        return packet ;
    }

    // TODO: implement the behavior
    // send the message to the remote server that we connect with
    // return: the InputStream from the server
    public InputStream sendMessage( String message ) throws IOException{

        sendMessage( message, this.currentSeqNum, this.currentACKNum, (byte) 0);

        readAll(); ;

        return null ;
    }

    // handle the TCP's 3 way handshake
    private  void handShake(){
        System.out.println( "HANDSHAKING");
        try {

            // send the SYN packet and then update currentSeqNum
            sendMessage( null, this.currentSeqNum++ , this.currentACKNum, SYN_FLAG);

            TCPPacket returnedPacket = null ;
            while(true){
                returnedPacket = read() ;
                if( returnedPacket.getHeader().isACKFlagOn()
                 && returnedPacket.getHeader().isSYNFlagOn()
                 && returnedPacket.getHeader().getACKNumber() == this.currentSeqNum ){
                    break ;
                }
            }
            // ACK-ing the SYN/ACK from server
            sendMessage( null, ++this.currentSeqNum,
                         this.currentACKNum = returnedPacket.getHeader().getSequenceNumber() + 1l,
                         ACK_FLAG);

            System.out.println( "FINISHING HANDSHAKE") ;

        } catch (IOException e) {
            System.out.println( "Failed to connect to remote server: " + e.toString() ) ;
        }

    }

    // read all incoming packets until it get a complete HTML request for the server
    // the method will send back an ACK to the server if there is no error
    // Assumption: we have finished the handshake and send out the request message
    private void readAll(){

        ByteArrayOutputStream out = new ByteArrayOutputStream( DATA_BUFFER_SIZE * 2 ) ;
        while(true){
            byte[] responseData = new byte[DATA_BUFFER_SIZE] ;
            try {

                int packetSize = this.rSock.read( responseData ) ;

                // TODO: parse IPHeader , verify this packet is intended for this program (matching src addr, dest,adrr)

                // parse TCPHeader, , verify this packet is intended for this program (matching src port, dest port)
                // get the TCP header and ignore all optional fields
                TCPHeader header =  new TCPHeader( Arrays.copyOfRange( responseData, IP_HEADER_SIZE,
                                                                       TCP_IP_HEADERS_MIN_SIZE ) );

                if( header.getSourcePort() != this.destPort || header.getDestinationPort() != this.sourcePort ){
                    continue;
                }

                int dataIndex = Util.getDataIndex( header , IP_HEADER_SIZE) ;

                // check if there are options in TCP header
                if( dataIndex > TCP_IP_HEADERS_MIN_SIZE ){
                    header.setOptions( Arrays.copyOfRange( responseData, TCP_IP_HEADERS_MIN_SIZE, dataIndex ) );
                }

                TCPPacket packet = new TCPPacket( header );

                // setting data
                if( packetSize - 1 >  dataIndex ){
                    packet.setData( Arrays.copyOfRange( responseData, dataIndex , responseData.length ));
                    this.currentACKNum += packet.getData().length + 1 ;
                }

                // verifying checksum
                // TODO: fix this
                /*
                if( verifyChecksum( packet ) ){
                    System.out.println( "check sum correctlly") ;
                    return packet ;
                }
                */

                // check if server stop sending data
                if( header.isFINFlagOn() ){

                    // if server ACK-ing something we have sent
                    if( header.isACKFlagOn() ){
                        this.currentSeqNum = header.getACKNumber() - 1 ;
                    }

                    //this.currentACKNum
                    //

                    sendMessage( null, this.currentSeqNum, header.getACKNumber() + 1l, ACK_FIN_FLAG);
                }
                else if ( header.isACKFlagOn() ){

                }
                // when ??
                out.write( packet.getData() );

            } catch (IOException e) {
                System.out.println( "Failed to get data from remote server: " + e.toString() ) ;
            }
        }

    }


    boolean isIPSupported() throws SocketException {
    	return this.rSock.getIPHeaderInclude() ;
    }


	public static void main( String args[] ){
				
		try{
			RawSocketClient client = new RawSocketClient( 
				new URL("http://www.ccs.neu.edu/home/cbw/4700/project4.html").getHost() ,80 ) ;
            System.out.println("CONNECTING");
			client.connect() ;
            System.out.println("DISCONNECTING");
            client.disconnect() ;

        }
		catch (SocketException ex){
			System.out.println( ex.toString() ) ;
		}
		catch( IOException ex ){
			System.out.println( ex.toString() ) ;
		}
	}
}

/*
            System.out.println("PACKET: ") ;
            System.out.println( "Source port: " + header.getSourcePort() ) ;
            System.out.println( "Destination port: " + header.getDestinationPort() ) ;
            System.out.println(  "ACK num: " + header.getACKNumber() ) ;
            System.out.println(  "Sequence num: " + header.getSequenceNumber() ) ;
            System.out.println(  "Window size: " + header.getWindowSize() ) ;
            System.out.println(  "Checksum: " + header.getChecksum() ) ;
            System.out.println(  "Header length: " + header.getHeaderLength() ) ;
            System.out.println( "ACK FLAG on: " + header.isACKFlagOn() ) ;
            System.out.println( "SYN FLAG on: " + header.isSYNFlagOn() ) ;
            System.out.println( "FYN FLAG on: " + header.isFINFlagOn() ) ;
            System.out.println("*******************************************************") ;
*/