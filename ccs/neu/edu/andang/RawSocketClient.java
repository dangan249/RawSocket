package ccs.neu.edu.andang ;

import java.net.InetAddress ;
import java.net.SocketException ;
import java.io.IOException ;

import com.savarese.rocksaw.net.RawSocket;
import static com.savarese.rocksaw.net.RawSocket.PF_INET;

public class RawSocketClient{

	RawSocket rSock = new RawSocket() ;

	public void close() throws IOException {
		this.rSock.close() ;
	}
	public static void main( String args[] ){
		
		RawSocketClient client = new RawSocketClient() ;
		try{
			client.rSock.open( PF_INET, RawSocket.getProtocolByName("tcp")) ;
			System.out.println( client.rSock.getIPHeaderInclude() ) ;
			client.close() ;

		}
		catch (SocketException ex){
			System.out.println( ex.toString() ) ;
		}
		catch( IOException ex ){
			System.out.println( ex.toString() ) ;
		}
	}
}