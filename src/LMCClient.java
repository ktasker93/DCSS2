/**
 * A class to launch a LMCClient - Local Monitoring Centre Client.
 * Used to view all verified Alarms that have passed through a specified LMC.
 * Used to view all connected Sensors that are currently in a LMC.
 * Note - does not support removal of either alarms or connected sensors.
 * Some elements of the GUI were borrowed from the RelayGUI example provided on Unilearn.
 * @author - Kieron Tasker u1258496
 * 
 * Created as a part of my submission for the Distributed Client Server Systems CHS2546 module.
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.omg.CORBA.ORB;

import LMCentre.Alarm;
import LMCentre.LMS;
import LMCentre.LogHolder;
import RiverSensor.Reading;
import RiverSensor.Sensor;




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
public class LMCClient extends JFrame {
    private JPanel textpanel;
    private JScrollPane scrollpane;
    private JButton getLMCButton;
    private JButton buttonRetrieveLog;
    private JButton buttonRetrieveConnected;
    private JTextField lmcIDInput;
    private JTextArea textarea;

    public LMCClient(String[] args) {
	try {
	    // set up the GUI
	    textarea = new JTextArea(20,25);
	    scrollpane = new JScrollPane(textarea);
	    textpanel = new JPanel();
	    textpanel.add(scrollpane);
	    scrollpane.setBounds(12, 5, 368, 352);
	    {
	    	//Text field to input the name of a sensor to investigate.
	    	lmcIDInput = new JTextField();
	    	textpanel.add(lmcIDInput);
	    	lmcIDInput.setText("LMC ID...");
	    	lmcIDInput.setBounds(12, 363, 102, 23);
	    }
	    {
	    	//button to retrieve the alarmLog for the specified LMC.
	    	buttonRetrieveLog = new JButton();
	    	textpanel.add(buttonRetrieveLog);
	    	buttonRetrieveLog.setText("Retrieve Log");
	    	buttonRetrieveLog.setBounds(126, 363, 120, 23);
	    	buttonRetrieveLog.addActionListener(new ActionListener() {
	    		public void actionPerformed(ActionEvent evt) {
	    			try{
	    				//get a reference to the speicified LMC.
	    			LMS lms = getLMC(lmcIDInput.getText().toString());
	    			textarea.setText("");
	    			textarea.append("Connected to LMC " + lms.name() + " successfully!\n");
	    			textarea.append("Printing the log of alarms for LMS: \"" + lms.name() + "\".\n");
		    		//retrieve the LMC alarm log (LMCServant.theLog();)
	    			Alarm[] alarmLog;
	    			alarmLog = lms.theLog();
	    			textarea.append("---\n");
	    			//iterate through array alarmLog, print verbose info.
		    		for(int i = 0; i< alarmLog.length; i++){
		    			Alarm thisAlarm = alarmLog[i];
		    			textarea.append("Alarm on date " + thisAlarm.date + " at time " + thisAlarm.time + "\n");
		    			textarea.append("\"" + thisAlarm.sensor + "\" triggered an alarm.\n");
		    			textarea.append("Sensor 1 level: " + thisAlarm.level1 + " Sensor 2 level: " + thisAlarm.level2 + "\n");
		    			//textarea.append("Alarm sent to notification list? " + thisAlarm.isResolved);
		    			textarea.append("---\n");
		    		}
		    		textarea.append("End of log.\n\n");
	    			} catch(Exception e){
	    				System.out.println("Exception caught. Specified LMC probably isn't running/doesn't exist!");
	    			}
	    		}
	    	});
	    }
	    {	//button to retrieve all connected sensors to a specified LMC.
	    	buttonRetrieveConnected = new JButton();
	    	textpanel.add(buttonRetrieveConnected);
	    	buttonRetrieveConnected.setText("View Connected Sensors");
	    	buttonRetrieveConnected.setBounds(126, 391, 157, 23);
	    	buttonRetrieveConnected.addActionListener(new ActionListener() {
	    		public void actionPerformed(ActionEvent evt) {
	    			try{
	    				//get reference to specified LMC.
	    			LMS lms = getLMC(lmcIDInput.getText().toString());
	    			textarea.setText("");
	    			textarea.append("Connected to LMC " + lms.name() + " successfully!\n");
	    			textarea.append("Displaying all connected servers to this LMC: \"" + lms.name() + "\".\n");
		    		//get the connected sensors from the LMC.
	    			String[] connectedLog = lms.linkedSensors();
	    			textarea.append("---\n");
		    		//iterate through connectedLog, print verbose info.
	    			for(int i = 0; i< connectedLog.length; i++){
		    			String thisSensor = connectedLog[i];
		    			textarea.append("\"" + thisSensor + "\"\n");
		    		}
	    			textarea.append("---\n");
		    		textarea.append("End of log.\n\n");
		    		} catch(Exception e){
		    			System.out.println("Exception caught. Specified LMC probably isn't running/doesn't exist!");
		    		}
	    		}

	    	});
	    }

	    getContentPane().add(textpanel, "Center");
	    textpanel.setLayout(null);

	    setSize(400, 500);
            setTitle("LMC Client");

            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );

	    textarea.append("Client started. Input a LMS name to view connected sensors or the Alarm log.\n\n");
	    
	} catch (Exception e) {
	    System.out.println("ERROR : " + e) ;
	    e.printStackTrace(System.out);
	}
    }



    public static void main(String args[]) {
	final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
		public void run() {
		    new  LMCClient(arguments).setVisible(true);
		}
	    });
    }
    
    public LMS getLMC(String lmcID){
    	try{
	    // create and initialize the ORB
		String[] args = new String[0];
	    ORB orb = ORB.init(args, null);
	    
	    // read in the 'stringified IOR' of the Relay
        BufferedReader in = new BufferedReader(new FileReader("LMC_" + lmcID + ".ref"));
      	String stringified_ior = in.readLine();
	    
	    // get object reference from stringified IOR
      	org.omg.CORBA.Object server_ref = 		
		orb.string_to_object(stringified_ior);
	    
	    final LMS lms = 
		LMCentre.LMSHelper.narrow(server_ref);
	    return lms;

    	} catch(Exception e){
    		System.out.println("Selected sensor.ref file not found (or it has been deleted since this sensor started running.)");
    		textarea.append("Sensor not found!");
    	}
    	return null;
    
    }
}