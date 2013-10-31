package ccs.neu.edu.andang ;

import java.net.URL ;
import java.util.Map ;
import java.util.Collection ;

import com.google.common.collect.Multimap ;
import com.google.common.collect.HashMultimap ; 
import java.lang.StringBuilder ;

// HTTPRequest contains the headers and the body of any HTTP Message
// Clients need to instantiate this class, populate its field, and set it in HTTPClient
// in order to make a request 
public class HTTPRequest{

	private URL url ; // if users do not specify port, URL default it to 80
	private String requestBody ;
	private Multimap< String, String > headers ;
	private Map<String, String> cookies ; // convenient place to get cookies

	public HTTPRequest( URL url ){

		this.url = url ;
		this.headers = HashMultimap.create() ;
		this.requestBody = "" ;

	}


	// adding cookies to the header of sending request
	// side-effect: add more pairs in this.headers
	public void addCookies(Map<String, String> cookies) {

		this.cookies = cookies ;

		String key = "Cookie";
		StringBuilder builder = new StringBuilder() ;

		int i = 0 ;

		for( String cookieKey : cookies.keySet() ){

				builder.append( cookieKey ) ;
				builder.append( "=") ;
				builder.append( cookies.get( cookieKey ) )  ;

			if( i != cookies.size() - 1 ){
				builder.append( "; ") ;
			}

			i++ ;
		}

		this.headers.put( key, builder.toString() );

	}


	// create the entire HTTP 1.0 message from this HTTPRequest
	public String serializeRequest( HTTPClient.HTTPMethod method ){
		// give StringBuilder some initial capacity will save time if the 
		// request is big
		StringBuilder builder = new StringBuilder(112) ;

		// BUILDING FIRST LINE 
		builder.append( method.value() ) ;
		builder.append( " " ) ;
		String path = this.getURL().getPath() ;
		builder.append(  path == "" ? "/" : path ) ; 
		builder.append( " " ) ;
		builder.append( "HTTP/1.0\r\n" ) ;


		// BUILDING HEADERS 
		Multimap<String,String> headers = this.getHeaders() ;

		for( String key : headers.keySet() ){

			Collection<String> values =  headers.get( key ) ;

			for( String value : values ){

				builder.append( key ) ;
				builder.append( ":" ) ;
				builder.append( value ) ;
				builder.append( "\r\n" ) ;				

			}

		}
 
		builder.append( "\r\n" ) ;

		if( method == HTTPClient.HTTPMethod.POST ){
			builder.append( this.getRequestBody() ) ;
		}	

		return builder.toString() ;
	}

	public void setURL(URL url){
		this.url = url ;
	}

	public URL getURL(){
		return this.url ;
	}

	public void setRequestBody(String requestBody){
		this.requestBody = requestBody ;
	}

	public String getRequestBody(){
		return this.requestBody ;
	}

	public void setHeaders( Multimap< String, String > headers){
		this.headers = headers ;
	}

	public Multimap< String, String > getHeaders(){
		return this.headers ;
	}

	public Map<String, String> getCookies(){
		return this.cookies ;
	}
	
	@Override
	public String toString(){
		return  "REQUEST :" + "\n" + 
				"URL : " + url.toString() + "\n" 				
				+ "HEADERS: " + "\n"
				+ headers.toString() + "\n"  
				+ "BODY : " + "\n" 
				+ requestBody + "\n" ;
	}

}
