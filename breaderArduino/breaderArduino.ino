#include <SoftwareSerial.h>
#include <Servo.h> 
#define CHAR_ARRAY_SIZE 72
#define PIN_COUNT 6

SoftwareSerial mySerial(10, 11); // RX, TX

byte byteRead;
 
int braillePins[] = { 2, 3, 4, 5, 6, 7 };
int refreshSpeed = 3000;
 
String inData = String("");
char caps[] = "000001";
char number[] = "001111";
char space[] = "111111";

const int numServos = 6;
Servo servos[numServos];

int delayBetweenPins = 100;
int rotation = 30;

boolean runningBluetooth = true;
boolean debuggingServos = false;

// Array of braille binary representations mapped to their equivalent letter
char *braileData[] = {"a", "100000", "b", "110000", "c", "100100", "d", "100110", "e", "100010", "f", "110100",
		"g", "110110", "h", "110010", "i", "010100", "j", "010110", "k", "101000", "l", "111000",
                "m", "101100", "n", "101110", "o", "101010", "p", "111100", "q", "111110", "r", "111010",
                "s", "011100", "t", "011110", "u", "101001", "v", "111001", "w", "010111", "x", "101101",
                "y", "101111", "z", "101001", ".", "010011", ",", "010000", "!", "011010", ";", "011000",
                ":", "100011",};      
				    

void setup() {                
   Serial.begin(9600);
    delay(2000); // Give the solenoids a moment to set their state 
    Serial.println("b.reader");
    
    // set the data rate for the SoftwareSerial port
    mySerial.begin(9600);
	
    mySerial.println("Hello, world?");
//
//  
//    for(int i=0; i<numServos; i++) {
//      servos[i].attach(i+2);
//    }
} 


void loop() {   
    // testing the servo
    if(debuggingServos) {
      sendBinaryToArduino("111111");
      delay(1000);
      sendBinaryToArduino("000000");
      delay(1000);
    }
    
    // bluetooth
    if(runningBluetooth) {
      // Implement variable speed using pentotiometer   
	 // Here we listen for serial input to translate the words   
	 // to braille representation   
	if( mySerial.available() > 0 )
	{
		inData = "";
		int h = mySerial.available();
		for (int i = 0; i < h; i++) 
		{
                        char c = (char)mySerial.read();
//			Serial.print("top in for \n");
			Serial.println(c);
//			Serial.print("bottom  in for \n");
			inData.concat(c);
		}
//		Serial.print("top of loop \n");
		Serial.println(inData);
//		Serial.print("bottom of loop \n");

		if (inData!="")
		{
			convertStringToBraille(inData);
//			convertStringToBraille(space);
		}
		inData = "";   
	}
//
//	if (mySerial.available())
//		Serial.println(mySerial.available());
//		Serial.write(mySerial.read());
	if (Serial.available())
		mySerial.write(Serial.read());

	delay(500);
    }
}
 
void convertStringToBraille( String text ) 
{
	int stringLength = text.length();
 
	for( int i=0; i < stringLength; i++ ) 
	{
		char currentChar = text.charAt(i);       
	// Handle special case for capital letters, 
	// numbers, spaces and then standard lower   
	// case letters    
//		Serial.print("top of number \n");
		Serial.println(currentChar);
//		Serial.print("\n");
//		Serial.print(text);
//		Serial.print("bottom of number \n");
		if(currentChar>='A' && currentChar <='Z')
		{      
			sendBinaryToArduino(caps);    
		} 
		else if(currentChar>='0' == currentChar<='9')
		{
			sendBinaryToArduino(number);
			String braille = getBrailleEquivalent(isANumber(currentChar));
			sendBinaryToArduino(braille);
		}
		else if( currentChar == ' ' )
		{ 
			sendBinaryToArduino(space);
		} 
		else 
		{
			String braille = getBrailleEquivalent(currentChar);
			sendBinaryToArduino(braille);
		}
		delay(refreshSpeed);
	} 

        Serial.print("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
}
 
char isANumber( char c ){
	// In braille numbers 0 - 1 are shown as letters a - j with a
	// # in front of the letter, so here we just return the 
	// equivalent letter
	if(c == '0'){
		return 'j';
	} else if(c == '1'){
		return 'a';
	} else if(c == '2'){
		return 'b';
	} else if(c == '3'){
		return 'c';
	} else if(c == '4'){
		return 'd';
	} else if(c == '5'){
		return 'e';
	} else if(c == '6'){
		return 'f';
	} else if(c == '7'){
		return 'g';
	} else if(c == '8'){
		return 'h';
	} else if(c == '9'){
		return 'i';
	}
}
 
void sendBinaryToArduino(String binary){
	// Loop through the binary braille representation
	// and set the pin status for each bit
//	Serial.print(binary);
//	Serial.print("\n");
	for( int i = 0; i < binary.length(); i++ ) 
	{
		char currentChar = binary.charAt(i);
                servos[i].attach(i+2);
		if( currentChar == '1') {
//			digitalWrite(braillePins[i], LOW); 
                      servos[i].write(rotation);
		}
		else if ( currentChar == '0') 
		{ 
//			digitalWrite(braillePins[i], HIGH); 
                      servos[i].write(0);
		} 
                 delay(delayBetweenPins);
                servos[i].detach();
//              delay(250);
	}
}
 
String getBrailleEquivalent( char c ) 
{
	// Loop through the array of characters and match the character
	// parameter to return the binary equivelant
	for (int i=0; i < CHAR_ARRAY_SIZE; i++) 
	{
		if (String(c) == braileData[i]) 
		{
			return braileData[i+1];
			break;
		}
	}
}
