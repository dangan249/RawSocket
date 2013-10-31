package ccs.neu.edu.andang ;

import java.net.URL ;
import com.google.common.collect.Multimap ;
import com.google.common.collect.HashMultimap ; 
import java.util.Map ;

// HTTPResponse contains the headers and the body of any HTTP Message returned by a server
public class HTTPResponse{

	private URL url ;
	private String responseBody ;
	private Multimap< String, String > headers ; // this.headers will not contain any info about Cookies
	private Map<String,String> cookies ; // convenient place to get cookies
	private HTTPClient.StatusCode code ;

	public void setURL(URL url){
		this.url = url ;
	}

	public URL getURL(){
		return this.url ;
	}

	public void setStatusCode( HTTPClient.StatusCode code ){
		this.code = code ;
	}

	public HTTPClient.StatusCode getStatusCode(){
		return this.code ;
	}

	public void setResponseBody(String responseBody){
		this.responseBody = responseBody ;
	}

	public String getResponseBody(){
		return this.responseBody ;
	}

	public void setHeaders(Multimap< String, String > headers){
		this.headers = headers ;
	}

	public Multimap< String, String >  getHeaders(){
		return this.headers ;
	}

	public void setCookies( Map< String, String > cookies){
		this.cookies = cookies ;
	}

	public Map< String, String >  getCookies(){
		return this.cookies ;
	}

	@Override
	public String toString(){
		return  "RESPONSE :" + "\n" + 
				"URL : " + url.toString() + "\n" 				
				+ "STATUS CODE : " + Integer.toString( code.value() )  + "\n"  
				+ "HEADERS: " + "\n"
				+ headers.toString() + "\n"  
				+ "COOKIES: " + "\n"
				+ cookies.toString() + "\n"  
				+ "BODY : " + "\n" 
				+ responseBody + "\n" ;
	}

}
