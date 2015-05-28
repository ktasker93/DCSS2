/**
 * A class to create a RMCClient - Regional Monitoring Centre Client.
 * Connects to an RMC and retrieves various states of alarms stored at the RMCServer.
 * Users are prompted to 'register' upon launching this class - this is mostly arbitrary however.
 * Selecting a username including 'police', 'agency' or 'government' will show all confirmed Alarms recorded by a LMC in a specified zone.
 * Selecting another arbitrary username will show only the latest Alarm recorded by a LMC in a specified zone.
 * These alarms are stored in the RMC server.
 * Viewing an alarm as an authorised role (police etc) will mark it as 'resolved' and users will see that the alarm has been viewed by an agency.
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.omg.CORBA.ORB;

import LMCentre.Alarm;
import RMCentre.RMC;
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
public class RMCClient extends JFrame {
	private JPanel textpanel;
    private JScrollPane scrollpane;
    private JButton buttonRetrieveLog;
    private JTextArea textarea;
    private String userID;
    private String userContact;
    private String userZone;

    public RMCClient(String[] args) {
	try {
		RMC rmc = getRMC();

		setTitle("Existing User?");
		userID = (String)JOptionPane.showInputDialog("Enter your username.\nIf you are not registered, an account will be created for you.",null);				
		if(userID == null || userID.isEmpty() || userID.length() > 50){
			while(userID == null || userID.isEmpty() || userID.length() > 50){
			userID = ((String)JOptionPane.showInputDialog("Enter a valid username. (Alphanumeric string of 50 characters or less.)",null));				
			}
		}
		
		setTitle("Contact information");
		userContact = (String)JOptionPane.showInputDialog("Enter contact information - email, phone etc. (input is not checked!)",null);				
		
		setTitle("Where are you located?");
		userZone = ((String)JOptionPane.showInputDialog("Enter the name of the LMC that covers your area. (LMC_[name].ref to connect to desired LMC).",null));				
		if(userZone == null || userZone.isEmpty() || userZone.length() > 50){
			while(userZone == null || userZone.isEmpty() || userZone.length() > 50){
			userZone = ((String)JOptionPane.showInputDialog("Enter a valid LMC Name. (Alphanumeric string of 50 characters or less.)",null));				
			}
		}
		
		//register a user or agency appropriately.
		if(userID.toLowerCase().contains("police") || userID.toLowerCase().contains("agency") || userID.toLowerCase().contains("government")){
			rmc.registerAgency(userID, userContact, userZone);
		} else {
			rmc.registerUser(userID, userContact, userZone);
		}
		
	    // set up the GUI
	    textarea = new JTextArea(20,25);
	    scrollpane = new JScrollPane(textarea);
	    textpanel = new JPanel();
	    textpanel.add(scrollpane);
	    scrollpane.setBounds(12, 5, 368, 352);

	    {
	    	//button for retrieving the alarms. works if the user is an agency or a user.
	    	buttonRetrieveLog = new JButton();
	    	textpanel.add(buttonRetrieveLog);
	    	buttonRetrieveLog.setText("Retrieve Latest Alarms");
	    	buttonRetrieveLog.setBounds(12, 363, 157, 23);
	    	buttonRetrieveLog.addActionListener(new ActionListener() {
	    		public void actionPerformed(ActionEvent evt) {
	    			try{
	    			//fetch a reference to the RMC
	    			RMC rmc = getRMC();
	    			
	    			textarea.setText("");
	    			textarea.append("Connected the RMC successfully!\n");
	    			if(rmc.checkUser(userID) == true){ //if the user is an agency, retrieve a list of all alarms in this zone
	    				textarea.append("Authorised user.\n");
	    				textarea.append("Printing all current alarms as recorded by the LMC \"" + userZone + "\"\n");
	    				//retrieve and store all alarms from RMC
	    				Alarm[] alarmLog;
	    				alarmLog = rmc.allCurrentAlarms(userZone, userID);
	    				textarea.append("---\n");
    					//print verbose info
	    				for(int i = 0; i< alarmLog.length; i++){
	    					Alarm thisAlarm = alarmLog[i];
	    					textarea.append("Alarm on date " + thisAlarm.date + " at time " + thisAlarm.time + "\n");
	    					textarea.append("\"" + thisAlarm.sensor + "\" triggered an alarm.\n");
	    					textarea.append("Sensor 1 level: " + thisAlarm.level1 + " Sensor 2 level: " + thisAlarm.level2 + "\n");
	    					textarea.append("---\n");
	    					} 
	    				} else { //the user is not an agency, so only retrieve the latest alarm
	    					textarea.append("Printing the most recent alarm as recorded by the LMC \"" + userZone + "\"");
		    				//retrieve and the latest alarm from RMC
	    					Alarm thisAlarm = new Alarm();
	    					thisAlarm = rmc.currentAlarm(userZone, userID);
	    					String isResolved;
    						System.out.println(thisAlarm.isResolved);

	    					//get isResolved, which is set based on whether an agency has retrieved an alarm in the RMC alarmList before
	    					if(thisAlarm.isResolved == true){
	    						isResolved = "Yes.";
	    					} else {
	    						isResolved = "No.";
	    					}
	    					//print verbose info
	    					textarea.append("Alarm on date " + thisAlarm.date + " at time " + thisAlarm.time + "\n");
	    					textarea.append("\"" + thisAlarm.sensor + "\" triggered an alarm.\n");
	    					textarea.append("Sensor 1 level: " + thisAlarm.level1 + " Sensor 2 level: " + thisAlarm.level2 + "\n");
		    				textarea.append("Alarm sent to registered Agencies? - " + isResolved + "\n");
	    				}
		    		textarea.append("End of log.\n\n");
	    			} catch(Exception e){
	    				System.out.println("Exception caught. Specified LMC probably isn't running/doesn't exist!");
	    				e.printStackTrace();
	    			}
	    		}
	    	});
	    }
	    getContentPane().add(textpanel, "Center");
	    textpanel.setLayout(null);

	    setSize(400, 500);
            setTitle("RMC Client: Logged in as " + userID);

            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );

	    textarea.append("Client started.  Click the button to contact relay...\n\n");
	    textarea.append("You are now ready to recieve warnings about floods in your area \"" + userZone + "\".\n");

	    
	} catch (Exception e) {
	    System.out.println("ERROR : " + e) ;
	    e.printStackTrace(System.out);
	}
    }



    public static void main(String args[]) {
	final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
		public void run() {
		    new  RMCClient(arguments).setVisible(true);
		}
	    });
    }
    
    public RMC getRMC(){
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
}