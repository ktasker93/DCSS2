/**
 * A class to host a SensorServer.
 * Contains two textboxes to mimic two sensors several metres apart on a river.
 * Sends readings to a LMC specified on startup.
 * Either one of the two sensors may be disabled remotely via LMCServer (automatically done if incorrect reading for one sensor) or SensorClient.
 * Some elements of the GUI were borrowed from the RelayGUI example provided on Unilearn.
 * 
 * @author - Kieron Tasker u1258496
 * 
 * Created as a part of my submission for the Distributed Client Server Systems CHS2546 module.
 * 
 */
import LMCentre.Alarm;
import LMCentre.LMS;
import RiverSensor.Reading;
import RiverSensor.Sensor;
import RiverSensor.SensorPOA;

import org.omg.CORBA.*;
import org.omg.PortableServer.*;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.*;


class SensorServant extends SensorPOA {
    private SensorServer parent;
    static boolean power1;
    static boolean power2;
    static Reading reading;
    

    public SensorServant(SensorServer parentGUI) {
    	// store reference to parent GUI
    	parent = parentGUI;
    }
    
    //retrieve this sensor's name.
    //readonly attribute - cannot be changed after it is set.
	@Override
	public String name() {
		return SensorServer.getSensorID();
	}
	
	//retrieve this sensor's zone.
	//readonly attribute - cannot be changed after it is set.
	@Override
	public String zone() {
		return SensorServer.getZone();
	}
	
	//get this sensor's current reading level.
	@Override
	public Reading current() {
		return reading;
	}
	//set this sensor's current reading level.
	@Override
	public void current(Reading newCurrent) {
		reading = newCurrent;
	}
	
	//get this sensor's current reading level.
	@Override
	public Reading currentReading() {
		return reading;
	}
	//get sensor1's current power status.
	@Override
	public boolean power1() {
		return power1;
	}
	//change sensor1's current power stauts.
	@Override
	public void power1(boolean newPower1) {
		if(power1 != newPower1){
			power1 = newPower1;
		}
	}
	//get sensor2's current power status.
	@Override
	public boolean power2() {
		return power2;
	}
	//change sensor2's current power status.
	@Override
	public void power2(boolean newPower2) {
		if(power2 != newPower2){
			power2 = newPower2;
		}	
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
public class SensorServer extends JFrame {
    private JPanel panel;
    private JScrollPane scrollpane;
    private JLabel sensor1Label;
    private static JTextField currentSensorLevel1;
    private static JTextField currentSensorLevel2;
    private JLabel labelSensor2;
    private JButton sendReadingButton;
    private static JTextArea textarea;
    private static String sensorID;
    private static String zone;

    public SensorServer(String[] args){
	try {
		setTitle("New Sensor - Name input");
		setSensorID((String)JOptionPane.showInputDialog("Sensor Name:",null));				
		if(getSensorID() == null || getSensorID().isEmpty() || getSensorID().length() > 50){
			//an invalid sensor name was chosen. enforce the user selects an appropriate sensor name.
			while(getSensorID() == null || getSensorID().isEmpty() || getSensorID().length() > 50){
			setSensorID((String)JOptionPane.showInputDialog("Enter a valid Sensor Name. (Alphanumeric string of 50 characters or less.)",null));				
			}
		}
		
		setTitle("New Sensor - Zone input");
		setZone((String)JOptionPane.showInputDialog("Sensor Zone:",null));				
		if(getZone() == null || getZone().isEmpty() || getZone().length() > 50){
			//an invalid sensor zone was chosen. enforce the user selects an appropriate sensor zone.
			while(getZone() == null || getZone().isEmpty() || getZone().length() > 50){
			setZone((String)JOptionPane.showInputDialog("Enter a valid Sensor Zone. (Alphanumeric string of 50 characters or less.)",null));				
			}
		}
		
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null);
	    
	    // get reference to rootpoa & activate the POAManager
	    POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
	    rootpoa.the_POAManager().activate();
	    
	    // create servant and register it with the ORB
	    SensorServant helloRef = new SensorServant(this);
	    
	    // get the 'stringified IOR'
	    org.omg.CORBA.Object ref = rootpoa.servant_to_reference(helloRef);
	    String stringified_ior = orb.object_to_string(ref);
	    
    	// Save IOR to file
        BufferedWriter out = new BufferedWriter(new FileWriter("SENSOR_" + getSensorID() + ".ref"));
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
	    	sendReadingButton = new JButton();
	    	panel.add(sendReadingButton);
	    	sendReadingButton.setText("Save reading");
	    	sendReadingButton.setBounds(126, 412, 126, 23);
	    	sendReadingButton.addActionListener (new ActionListener() {
			    public void actionPerformed (ActionEvent evt) {
			    	createReading();
			    	boolean power1 = SensorServant.power1;
			    	boolean power2 = SensorServant.power2;
			    if(!(power1 == false && power2 == false)){  //if at least one of the sensors is switched on, or both (OR gate!)
			    	if(SensorServant.reading.level1 >= 70 || SensorServant.reading.level2 >= 70){ //one of the sensor levels is dangerously high!!

			    		try{ //connect to the LMC and raise an alarm
					    // get reference to our LMC
						LMS lms = getLMC();
						//create the alarm and populate with timestamps, sensor levels, sensor powers
				    	textarea.append("WARNING: Possible flood! (level >= 70)\n");
				    	textarea.append("Sending this reading to a LMS.\n");
						String timeStampDate = new SimpleDateFormat("ddMMyyyy").format(Calendar.getInstance().getTime());
						String timeStampTime = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());	
				    	
						Alarm alarm = new Alarm();
				    	alarm.time = Integer.parseInt(timeStampTime);
				    	alarm.date = Integer.parseInt(timeStampDate);
				    	alarm.sensor = getSensorID();
				    	alarm.isResolved = false;
				    	alarm.zone = getZone();
				    	if(SensorServant.power1) //if sensor 1 is on, get the value in sensor 1 text box
				    		alarm.level1 = Integer.parseInt(currentSensorLevel1.getText().toString());
				    	else //set sensor1's level to 0 (dry river, or disabled sensor)
				    		alarm.level1 = 0;
				    	//repeat for sensor 2
				    	if(SensorServant.power2)
				    		alarm.level2 = Integer.parseInt(currentSensorLevel2.getText().toString());
				    	else
				    		alarm.level2 = 0;
				    	
				    	//the alarm has not yet been viewed by an agency
				    	alarm.isResolved = false;
				    	
				    	//get a reference to this sensor, and its name
				    	RiverSensor.Sensor sensor = getSensor(getSensorID());
				    	System.out.println("debug sensor name: " + sensor.name());
				    	
				    	//raise an alarm at the LMC
				    	lms.raise_Alarm(alarm);
				    	} catch(Exception e){
				    		e.printStackTrace();
				    		System.out.println("something went wrong! probably LMC is not running or specified LMC does not exist.");
				    	}
				    }
			    } else { //both of the sensors are switched off, so send nothing
			    	textarea.append("\nError: This device is switched off!\nTurn it back on using the SensorClient.\n");
			    }

			    }
			});
	    }
	    {
	    	currentSensorLevel1 = new JTextField();
	    	panel.add(currentSensorLevel1);
	    	panel.add(getSensor1Label());
	    	currentSensorLevel1.setText("50");
	    	currentSensorLevel1.setBounds(56, 383, 124, 23);
	    }
	    
	    {
	    	currentSensorLevel2 = new JTextField();
	    	panel.add(currentSensorLevel2);
	    	panel.add(getLabelSensor2());
	    	panel.add(getSensor1Label());
	    	currentSensorLevel2.setText("50");
	    	currentSensorLevel2.setBounds(208, 383, 124, 23);
	    }
	    getContentPane().add(panel, "Center");
	    panel.setLayout(null);

	    setSize(400, 500);
            setTitle("Sensor: " + getSensorID());

            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );

	    
	    // wait for invocations from clients
	    textarea.append("Hello world! This is sensor " + getSensorID() + " in zone " + getZone() + ".\n");
	    textarea.append("Readings of 0 indicate that that sensor is currently disabled.\n");
	    //turn both of the sensors on initially
	    SensorServant.power1 = true;
	    SensorServant.power2 = true;
	    
	    //create a reading on launch so any attempts to access this sensor don't break and give exceptions
	    SensorServant.reading = createReading();
	    //get reference to LMC
	    LMS lms = getLMC();
	    //register this sensor there
	    lms.add_Sensor(getSensor(getSensorID()));
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
		    new SensorServer(arguments).setVisible(true);
		}
	    });
    }


	public static String getSensorID() {
		return sensorID;
	}


	public void setSensorID(String sensorID) {
		SensorServer.sensorID = sensorID;
	}


	public static String getZone() {
		return zone;
	}

	public static void setZone(String zone) {
		SensorServer.zone = zone;
	}   
	//create a new Reading object using elements from GUI
	public static Reading createReading(){
		String timeStampDate = new SimpleDateFormat("ddMMyyyy").format(Calendar.getInstance().getTime());
		String timeStampTime = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());				//currentDate.DAY_OF_MONTH;
		Reading reading = new Reading();
		reading.date = Integer.parseInt(timeStampDate);
		reading.level1 = Integer.parseInt(currentSensorLevel1.getText().toString());
		reading.level2 = Integer.parseInt(currentSensorLevel2.getText().toString());
		reading.time = Integer.parseInt(timeStampTime);
		reading.sensorName = getSensorID();
		boolean power1 = SensorServant.power1;
    	boolean power2 = SensorServant.power2;
		SensorServant.reading = reading;

	    if(power1 == false)
	    	SensorServant.reading.level1 = 0;
	    if(power2 == false)
	    	SensorServant.reading.level2 = 0;
	    
		textarea.append("\nTimestamp: " + Integer.toString(reading.date) + " " + Integer.toString(reading.time) + "\nLevels: 1: " + Integer.toString(reading.level1) + " 2: " + Integer.toString(reading.level2) + "\n");
		return reading;
		
	}
	
	public static int getLevel(){
		return Integer.parseInt(currentSensorLevel1.getText().toString());
	}
	//get a reference to a specified SensorServant
    public Sensor getSensor(String sensorID){
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
	    
	    final RiverSensor.Sensor sensor = 
		RiverSensor.SensorHelper.narrow(server_ref);
	    return sensor;

    	} catch(Exception e){
    		System.out.println("something went wrong, sensorID either isn't running or doesn't exist!");
    		textarea.append("Sensor not found!");
    	}
    	return null;
    }
    //get a reference to the LMC created on initialisation
    public LMS getLMC(){
	    try{
		String[] args = new String[0];
	    ORB orb = ORB.init(args, null);
	    
	    // read in the 'stringified IOR' of the Relay
      	BufferedReader in = new BufferedReader(new FileReader("LMC_" + getZone() + ".ref"));
      	String stringified_ior = in.readLine();
	    
	    // get object reference from stringified IOR
      	org.omg.CORBA.Object server_ref = 		
		orb.string_to_object(stringified_ior);
	    
	    final LMCentre.LMS lms =
		LMCentre.LMSHelper.narrow(server_ref);
	    return lms;
	    } catch(Exception e){
	    	System.out.println("WHY");
	    }
	    return null;
    }
    //GUI labels (jigloo form editor put these down here for some reason)
    private JLabel getSensor1Label() {
    	if(sensor1Label == null) {
    		sensor1Label = new JLabel();
    		sensor1Label.setText("Sensor 1");
    		sensor1Label.setBounds(56, 367, 82, 16);
    	}
    	return sensor1Label;
    }
    
    private JLabel getLabelSensor2() {
    	if(labelSensor2 == null) {
    		labelSensor2 = new JLabel();
    		labelSensor2.setText("Sensor 2");
    		labelSensor2.setBounds(208, 367, 97, 16);
    	}
    	return labelSensor2;
    }
}


