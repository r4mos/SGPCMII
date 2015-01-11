package es.uclm.esi.tfg.naviganto;

public interface Const {
    //API keys
    static final String MAPQUESTAPIKEY = "Fmjtd%7Cluurnu0anl%2C2s%3Do5-9wrw94";

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

    //Menu & Request Code
    static final int EXPLORE = 0;
    static final int GO = 1;
    static final int SETTINGS = 2;
    static final int ABOUT = 4;
    static final int TEST = 3;

    //Request Strings
    static final String LOCAL = "local";
    static final String BLUETOOTH = "bluetooth";
    static final String WEAR = "wear";

    //Transport
    static final String FASTEST = "fastest";
    static final String SHORTEST = "shortest";
    static final String BICYCLE = "bicycle";
    static final String PEDESTRIAN = "pedestrian";

    //Vibrate mode
    static final int NONE = 0;
    static final int LLOCAL_RBLUETOOTH = 1;
    static final int LBLUETOOTH_RLOCAL = 2;
    static final int LLOCAL_RWEAR = 3;
    static final int LWEAR_RLOCAL = 4;
    static final int LBLUETOOTH_RWEAR = 5;
    static final int LWEAR_RBLUETOOTH = 6;
    static final int LBLUETOOTH_RBLUETOOTH = 7;
    static final int LWEAR_RWEAR = 8;

    //Bluetooth messages
    static final String START = "1";
    static final String STOP = "0";
    static final char SPLIT = '|';

    //Wear messages
    static final String START_ACTIVITY = "/start_activity";
    static final String END_ACTIVITY = "/end_activity";
    static final String WEAR_MESSAGE_PATH = "/message";
}
