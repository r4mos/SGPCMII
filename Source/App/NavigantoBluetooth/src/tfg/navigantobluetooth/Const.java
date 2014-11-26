package tfg.navigantobluetooth;

public interface Const {
	//Actions
	static final int WRONG = -1;
	static final int STRAIGHT = 0;
	static final int LEFT = 10;
	static final int RIGHT = 1;
	static final int UTURN = 11;
	static final int ROUNDABOUT1 = 21;
	static final int ROUNDABOUT2 = 22;
	static final int ROUNDABOUT3 = 23;
	static final int ROUNDABOUT4 = 24;
	static final int ROUNDABOUT5 = 25;
	static final int ROUNDABOUT6 = 26;
	static final int ROUNDABOUT7 = 27;
	static final int ROUNDABOUT8 = 28;
	static final int DESTINATION = 30;
	
	//Bluetooth messages
	static final String START = "1";
	static final String STOP = "0";
	static final char SPLIT = '|';
}
