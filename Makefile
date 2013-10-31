JFLAGS = -g
JC = javac
JARPATH = lib/*
LP = -Djava.library.path
LIBPATH = rocksaw-1.0.3/lib/
MAIN = ccs.neu.edu.andang.RawSocketClient
CP = -classpath

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) -classpath "${CLASSPATH}:lib/*" $*.java

CLASSES = \
        ./ccs/neu/edu/andang/Crawler.java \
        ./ccs/neu/edu/andang/HTTPClient.java \
        ./ccs/neu/edu/andang/HTTPRequest.java \
        ./ccs/neu/edu/andang/HTTPResponse.java \
        ./ccs/neu/edu/andang/SocketClient.java \
        ./ccs/neu/edu/andang/RawSocketClient.java 


default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) ./ccs/neu/edu/andang/*.class 
	
run: $(CLASSES)
	sudo java $(CP) .:$(JARPATH) $(LP)=$(LIBPATH) $(MAIN)


