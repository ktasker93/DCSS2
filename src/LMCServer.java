/**
 * A class to host a LMC - Local Monitoring Center.
 * Recieves Readings from SensorServers and checks readings are valid before passing on to RMC.
 * If readings are invalid (one sensor reads a higher reading than the other) the erroneous sensor is remotely disabled.
 * If one sensor in a pair is deactivated, the other sensor is used for readings.
 * Recieves method calls from LMCClients and SensorServers.
 * Sends method calls to RMC and SensorServers.
 * Some elements of the GUI were borrowed from the RelayGUI example provided on Unilearn.
 * @author - Kieron Tasker u1258496
 * 
 * Created as a part of my submission for the Distributed Client Server Systems CHS2546 module.
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import LMCentre.Alarm;
import LMCentre.ConnectedSensorsHolder;
import LMCentre.LMSPOA;
import LMCentre.LogHolder;
import RMCentre.AlarmData;
import RMCentre.RMC;
import RiverSensor.Sensor;


class LMCServant extends LMSPOA {
	//holds a reference to the parent class GUI.
    private LMCServer parent;
    //holds a reference to the CORBA generated LogHolder class to contain a Log array (Alarm[]).
    LogHolder holderLog = new LogHolder();
    //holds a reference to the CORBA generated ConnectedSensorsHolder to contain a Sensor array (Sensor[]).
    ConnectedSensorsHolder sensorHolder = new ConnectedSensorsHolder();
    //holds the list of all alarms (confirmed or not) recieved from SensorServers.
    private ArrayList<Alarm> alarmList = new ArrayList<Alarm>();
    //holds the list of all sensors connected to this LMC.
    private ArrayList<String> sensorList = new ArrayList<String>();
    int logSize = 0;
    private int alarmCounter = 0;
    
    public LMCServant(LMCServer parentGUI) {
    	// store reference to parent GUI
    	parent = parentGUI;
    }
    //the name of this LMC.
	@Override
	public String name() {
		// TODO Auto-generated method stub
		return LMCServer.getLMCID();
	}
	
	//return the log of alarms raised by this LMC.
	@Override
	public Alarm[] theLog() {
		Alarm[] alarmL = new Alarm[alarmList.size()];
		holderLog.value = alarmList.toArray(alarmL);
		return holderLog.value;
	}
	
	//set the log of alarms raised by this LMC.
	@Override
	public void theLog(Alarm[] newTheLog) {
		//alarmLog = newTheLog;
	}
	
	//create an alarm to send to the RMC.
	@Override
	public void raise_Alarm(Alarm aReading) {
		RMC rmc = LMCServer.getRMC(); //get a reference to the RMC
		String sensorID = aReading.sensor;
		Sensor sensor = LMCServer.getSensor(sensorID); //get a reference to the Sensor that sent this reading
		//verbose info printing
		parent.addMessage("\n---ALARM---\n");
		parent.addMessage("An alarm was triggered by a sensor in this zone!\n");
		parent.addMessage("Sensor name: " + aReading.sensor + "\n");
		parent.addMessage("Timestamp ddMMyyyy hhmmss:\n " + aReading.date + " " + aReading.time + "\n");
		parent.addMessage("Zone: " + aReading.zone + "\n");
		parent.addMessage("Levels: 1:" + aReading.level1 + ", 2: " + aReading.level2 + "\n");
		if(aReading.level1 >=70 && aReading.level2 >=70){ //if both of the alarms are above 70, assume a confirmed alarm.
			parent.addMessage("Sending this to RMC, added to log.\n");
			aReading.alarmID = alarmCounter;
			alarmCounter++;
			alarmList.add(aReading);
			logSize++;
			AlarmData thisAlarm = new AlarmData();
			thisAlarm.aReading = aReading;
			thisAlarm.aConfirmingSensor = aReading.sensor;
			rmc.raiseAlarm(thisAlarm);

		} else if(aReading.level1 >=70 && aReading.level2 == 0) { //if one sensor is deactivated and the other is dangerously high, assume an alarm.
			parent.addMessage("One sensor is disabled, so this will be treated as an alert.\n");
			parent.addMessage("Sending this to RMC, added to log.\n");
			aReading.alarmID = alarmCounter;
			alarmCounter++;
			alarmList.add(aReading);
			logSize++;
			AlarmData thisAlarm = new AlarmData();
			thisAlarm.aReading = aReading;
			thisAlarm.aConfirmingSensor = aReading.sensor;
			rmc.raiseAlarm(thisAlarm);

		}  else if(aReading.level2 >=70 && aReading.level1 == 0) {//if one sensor is deactivated and the other is dangerously high, assume an alarm.
			parent.addMessage("One sensor is disabled, so this will be treated as an alert.\n");
			parent.addMessage("Sending this to RMC, added to log.\n");
			aReading.alarmID = alarmCounter;
			alarmCounter++;
			alarmList.add(aReading);
			AlarmData thisAlarm = new AlarmData();
			thisAlarm.aReading = aReading;
			thisAlarm.aConfirmingSensor = aReading.sensor;
			rmc.raiseAlarm(thisAlarm);
			logSize++;
		} else { //only one sensor reads a dangerous level - so power it down
			parent.addMessage("Possible false alarm - only one sensor reads a dangerous level.\n");
			parent.addMessage("Powering down the erroneous sensor.\n");
			if(aReading.level1 > aReading.level2){
				sensor.power1(false); //call the power down method using the Sensor reference
			} else {
				sensor.power2(false); //call the power down method using the Sensor reference
			}
		}
		parent.addMessage("---END ALARM---\n\n");
	}
	
	//add a sensor to the list of sensors that are connected to this LMC
	@Override
	public void add_Sensor(Sensor aSensor) {
		parent.addMessage("\nA sensor has just been installed.\n");	
		sensorList.add(aSensor.name());
	}

	//return the list of sensors connected to this LMC
	public String[] linkedSensors() {
		String[] sensorL = new String[sensorList.size()];
		sensorHolder.value = sensorList.toArray(sensorL);
		return sensorHolder.value;
	}
	@Override
	public void linkedSensors(String[] newLinkedSensors) {
		// TODO Auto-generated method stub
		
	}
}

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class LMCServer extends JFrame {
    private JPanel panel;
    private JScrollPane scrollpane;
    private JButton clearLogButton;
    private static JTextArea textarea;
    private static String LMCID;
    private static String zone;
    private static Alarm[] alarmLogs;

    public LMCServer(String[] args){
	try {	
		setTitle("New LMC - Zone input");
		setLMCID((String)JOptionPane.showInputDialog("LMC Zone Name:",null));				
		if(getLMCID() == null || getLMCID().isEmpty() || getLMCID().length() > 50){
			//an invalid LMC name was chosen. enforce the user selects an appropriate sensor name.
			while(getLMCID() == null || getLMCID().isEmpty() || getLMCID().length() > 50){
			setLMCID((String)JOptionPane.showInputDialog("Enter a valid LMC Name. (Alphanumeric string of 50 characters or less.)",null));				
			}
		}
		
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null);
	    
	    // get reference to rootpoa & activate the POAManager
	    POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
	    rootpoa.the_POAManager().activate();
	    
	    // create servant and register it with the ORB
	    LMCServant helloRef = new LMCServant(this);
	    
	    // get the 'stringified IOR'
	    org.omg.CORBA.Object ref = rootpoa.servant_to_reference(helloRef);
	    String stringified_ior = orb.object_to_string(ref);
	    
    	// Save IOR to file
        BufferedWriter out = new BufferedWriter(new FileWriter("LMC_" + getLMCID() + ".ref"));
        out.write(stringified_ior);
	    out.close();

	    // set up the GUI
	    textarea = new JTextArea(20,25);
	    textarea.setEditable(false);
	    scrollpane = new JScrollPane(textarea);
	    panel = new JPanel();

	    panel.add(scrollpane);
	    scrollpane.setBounds(12, 5, 368, 352);
	    {
	    	clearLogButton = new JButton();
	    	panel.add(clearLogButton);
	    	clearLogButton.setText("Clear log");
	    	clearLogButton.setBounds(125, 369, 126, 23);
	    	clearLogButton.addActionListener (new ActionListener() {
			    public void actionPerformed (ActionEvent evt) {
			    	textarea.setText("");
			    }
			});
	    }
	    
	    getContentPane().add(panel, "Center");
	    panel.setLayout(null);

	    setSize(400, 500);
            setTitle("LMC: " + getLMCID());

            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );
  
	    // wait for invocations from clients
	    textarea.append("Hello world! This is LMC " + LMCServer.getLMCID() + ".\n");
	    
	    // remove the "orb.run()" command,
	    // or the server will run but the GUI will not be visible
	    // orb.run();
	    
	} catch (Exception e) {
	    System.err.println("ERROR: " + e);
	    e.printStackTrace(System.out);
	}
    }

    public void addMessage(String message){
    	textarea.append(message);
    }
    
    public static void main(String args[]) {
	final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
		public void run() {
		    new LMCServer(arguments).setVisible(true);
		}
	    });
    }
    //return the name of this LMC (used in the LMCServant sub class)
	public static String getLMCID() {
		return LMCID;
	}
	//set the name of this LMC
	public void setLMCID(String LMCID) {
		LMCServer.LMCID = LMCID;
	}
	/*
	public static Alarm[] getAlarmLogs(){
		return alarmLogs;
	}
	*/
	//get a reference to the RMC
    public static RMC getRMC(){
    	try{
	    // create and initialize the ORB
		String[] args = new String[0];
	    ORB orb = ORB.init(args, null);
	    
	    // read in the 'stringified IOR' of the Relay
        BufferedReader in = new BufferedReader(new FileReader("RMC.ref"));
      	String stringified_ior = in.readLine();
	    
	    // get object reference from stringified IOR
      	org.omg.CORBA.Object server_ref = 		
		orb.string_to_object(stringified_ior);
	    
	    final RMC rmc = 
		RMCentre.RMCHelper.narrow(server_ref);
	    return rmc;

    	} catch(Exception e){
    		System.out.println("Selected sensor.ref file not found (or it has been deleted since this sensor started running.)");
    		textarea.append("Sensor not found!");
    	}
    	return null;
    
    }
    //get a reference to a specified sensor
    public static Sensor getSensor(String sensorID){
    	try{
	    // create and initialize the ORB
		String[] args = new String[0];
	    ORB orb = ORB.init(args, null);
	    
	    // read in the 'stringified IOR' of the Relay
        BufferedReader in = new BufferedReader(new FileReader("SENSOR_" + sensorID + ".ref"));
      	String stringified_ior = in.readLine();
	    
	    // get object reference from stringified IOR
      	org.omg.CORBA.Object server_ref = 		
		orb.string_to_object(stringified_ior);
	    
	    final Sensor sensor = 
		RiverSensor.SensorHelper.narrow(server_ref);
	    return sensor;

    	} catch(Exception e){
    		System.out.println("Selected sensor.ref file not found (or it has been deleted since this sensor started running.)");
    		textarea.append("Sensor not found!");
    	}
    	return null;
    }
}


