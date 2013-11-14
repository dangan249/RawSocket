package ccs.neu.edu.andang ;

// Programmer: An Dang

// Util 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.*;
import java.io.ByteArrayOutputStream ;
import java.util.Arrays ;

// Exceptions:
import java.io.IOException ;
import java.lang.RuntimeException ;

// Networking:
import com.savarese.rocksaw.net.RawSocket;
import static com.savarese.rocksaw.net.RawSocket.PF_INET;

public class RawSocketClient{

	private final byte SYN_FLAG = (byte) 2;
	private final byte ACK_FLAG = (byte) 16;
	private final byte ACK_FIN_FLAG = (byte) 17;
	private final int AD_WINDOW_SIZE = 1460000;
    private final int DATA_BUFFER_SIZE = 2000 ;
    private final int IP_HEADER_SIZE = 20 ;
    private final int TCP_IP_HEADERS_MIN_SIZE = 40 ;
    private final long INITIAL_SEQUENCE_NUM = 1l ;
	private final long INITIAL_ACK_NUM = 0l ;

    private RawSocket rSock ;
    private String remoteHost ;
    private int destPort;
    private int sourcePort ;
    private InetAddress destAddress;
    private InetAddress sourceAddress ;
    private long currentSeqNum ; // the number of bytes have been sent
    private long currentACKNum ; // the number of bytes have received
    private int numBytesReceived = 0 ;
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
            //header.setFlags( (byte)(flags + 8)); // set PSH on, since we sending small request
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
        if( dataIndex > TCP_IP_HEADERS_MIN_SIZE ){
            header.setOptions( Arrays.copyOfRange( responseData, TCP_IP_HEADERS_MIN_SIZE, dataIndex ) );
        }

        packet = new TCPPacket( header );
        if( packetSize - 1 >  dataIndex ){
            numBytesReceived = packetSize - dataIndex ;
            packet.setData( Arrays.copyOfRange( responseData, dataIndex , responseData.length ));
        }

        // TODO: fix this,when packet has data, this check failed
        if( Util.verifyChecksum( packet , this.sourceAddress, this.destAddress ) ){
            return packet ;
        }

        return packet ;
    }

    // TODO: implement the behavior
    // send the message to the remote server that we connect with
    // return: the InputStream from the server
    public InputStream sendMessage( String message ) throws IOException{

        sendMessage( message, this.currentSeqNum, this.currentACKNum + 1 , ACK_FLAG);

        InputStream is = new ByteArrayInputStream( readAll().toByteArray() );

        return is ;
    }

    // handle the TCP's 3 way handshake
    // side-effect: change this.currentSeqNum and this.currentACKNum
    private  void handShake(){
        try {
            // send the SYN packet
            sendMessage( null, this.currentSeqNum , this.currentACKNum, SYN_FLAG);

            TCPPacket returnedPacket = null ;
            while(true){
                returnedPacket = read() ;
                if( returnedPacket.getHeader().isACKFlagOn()
                 && returnedPacket.getHeader().isSYNFlagOn()
                 && returnedPacket.getHeader().getACKNumber() == this.currentSeqNum + 1 ){
                    break ;
                }
            }
            // ACK-ing the SYN/ACK from server and also:
            // -- increase currentSeqNum since the previous SYN packet is successfully received
            // -- capture the server chosen starting sequence number
            this.currentACKNum = returnedPacket.getHeader().getSequenceNumber() ;
            sendMessage(null, ++this.currentSeqNum,
                    this.currentACKNum + 1 ,
                    ACK_FLAG);
        } catch (IOException e) {
            System.out.println( "Failed to connect to remote server: " + e.toString() ) ;
        }

    }

    // read all incoming packets until it get a complete HTML request for the server
    // the method will send back an ACK to the server if there is no error
    // Assumption: we have finished the handshake and send out the request message
    // side-effect:
    // -- changing this.currentACKNum: when receiving data (NOTE: we need to + 1 with this value when ACKING)
    // -- changing this.currentSeqNum:

    private ByteArrayOutputStream readAll(){

        ByteArrayOutputStream out = new ByteArrayOutputStream( DATA_BUFFER_SIZE * 2 ) ;
        boolean done = false ;
        boolean firstACK = true;
        while(true){
            try {

                TCPPacket packet = read() ;

                if( packet == null ){
                    continue ;
                }

                // if the packet is not in order, we ACK put do not use it or buffer it
                if( packet.getData() != null ){

                    if ( isInOrder(packet) ){
                        this.currentACKNum += this.numBytesReceived ;
                        this.numBytesReceived = 0 ;
                        out.write( packet.getData() );
                    }

                }

                if(done){
                    break ;
                }

                if( packet.getHeader().isACKFlagOn() ){
                    this.currentSeqNum = packet.getHeader().getACKNumber() - 1;
                }

                if(!firstACK){
                    sendMessage( null, this.currentSeqNum, this.currentACKNum + 2 - this.numBytesReceived , ACK_FLAG);
                }
                firstACK = false ;

                // check if server stop sending data
                if( packet.getHeader().isFINFlagOn() ){
                    // send back a FIN/ACK
                    sendMessage( null, this.currentSeqNum, packet.getHeader().getACKNumber() , ACK_FIN_FLAG) ;
                    this.currentSeqNum++ ;
                    done = true ;
                }

            } catch (IOException e) {
                System.out.println( "Failed to get data from remote server: " + e.toString() ) ;
            }
        }

        return out ;

    }

     // return true if this packet is in order: it is the packet we expect next
     private boolean isInOrder(TCPPacket packet){
        return (packet.getHeader().getSequenceNumber() - 1 )
                == this.currentACKNum ;

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