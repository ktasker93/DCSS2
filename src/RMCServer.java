/**
 * A class to host a RMC - Regional Monitoring Centre.
 * All LMCs automatically connect here (via RMC.ref).
 * LMCs and RMCClients utilise various methods defined in the RMCServant class.
 * Some elements of the GUI were borrowed from the RelayGUI example provided on Unilearn.
 * @author - Kieron Tasker u1258496
 * 
 * Created as a part of my submission for the Distributed Client Server Systems CHS2546 module.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
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
import LMCentre.LogHolder;
import RMCentre.AlarmData;
import RMCentre.AllAlarmListHolder;
import RMCentre.RMCPOA;
import RiverSensor.Reading;

//a class for holding ordinary users.
class AlarmRecipient{
	private String name;
	private String contact;
	private String zone;
	
	public AlarmRecipient(String n, String c, String z){
		this.name = n;
		this.contact = c;
		this.zone = z;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}

//a class for holding registered agencies.
class AgencyRecipient{
	private String name;
	private String contact;
	private String zone;
	
	public AgencyRecipient(String n, String c, String z){
		this.setName(n);
		this.contact = c;
		this.zone = z;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}

class RMCServant extends RMCPOA {
	//holds a reference to the parent class/gui.
    private RMCServer parent;
    //holds all registered users/agencies.
    private ArrayList<AlarmRecipient> recipientList = new ArrayList<AlarmRecipient>();
    private ArrayList<AgencyRecipient> agencyList = new ArrayList<AgencyRecipient>();
    //holds all confirmed alarms passed from the LMCs.
    private ArrayList<AlarmData> currentAlarms = new ArrayList<AlarmData>();
    private AllAlarmListHolder allAlarmListHolder = new AllAlarmListHolder();
    
    public RMCServant(RMCServer parentGUI) {
    	// store reference to parent GUI
    	parent = parentGUI;
    }
	
    //recieve an alarm from a LMC and add it to the currentAlarms list for retrieval by RMCClients.
    @Override
	public void raiseAlarm(AlarmData anAlarm) {
    	parent.addMessage("---\nAn alarm was raised by LMC \"" + anAlarm.aReading.zone + "\".\n");
    	parent.addMessage("RMCClient users (police, public, authorities etc) will be shown this \nalert when requesting the current alarm status.\n---\n");
		currentAlarms.add(anAlarm);
    	//TODO possible improvement:
    	//recieve alarm from LMCServer
		//turn RMCClients into 'RMCRelays' which act as a client to RMCServer and a host of RMCClient
    	//use for loop in registerAgency/recipientList arrays to send anAlarm to all connected RMCClients (which are servers/relays)
	}
    
    //recieve a request to add a new agency from the RMCClient
    //this method is called when 'who' is 'police', 'agency', or 'government'
    //ambiguous arbitrary buzzwords, more can be added/removed in the RMCClient code
    //agencies see all of the alarms in their registered zone and change their status to 'viewed' (isResolved = true)
	@Override
	public void registerAgency(String who, String contact_details, String zone) {
		AgencyRecipient thisAgency = new AgencyRecipient(who, contact_details, zone);
		agencyList.add(thisAgency);
		parent.addMessage("\nAdded the agency \"" + thisAgency.getName() + "\" successfully.\nThey will be alerted of *all* future confirmed floods.\n");
		parent.addMessage("Number of registered agencies: " + agencyList.size() + "\n");
	}
	
	//recieve a request to add a new user from the RMCClient
	//this method is called when none of the buzzwords mentioned previously are found in 'who'
	//users see only the latest alarm in their registered zone and whether an agency has viewed the alarm
	@Override
	public void registerUser(String who, String contact_details, String zone) {
		AlarmRecipient thisUser = new AlarmRecipient(who, contact_details, zone);
		recipientList.add(thisUser);
		parent.addMessage("\nAdded the user \"" + thisUser.getName() + " successfully.\nThey will be alerted of only the latest future confirmed floods.\n");
	}
	
	//returns the latest alarm in a zone for public (non authority) users of RMCClient.
	@Override
	public Alarm currentAlarm(String district, String who) {
		for(int i = currentAlarms.size()-1; i >=0; i--){
			if(currentAlarms.get(i).aReading.zone.equals(district))
				return currentAlarms.get(i).aReading;
		}	
		return null;
	}
	
	//returns a list of all confirmed alarms in a zone for authority users of RMCClient (police etc)
	@Override
	public Alarm[] allCurrentAlarms(String district, String who) {
		ArrayList<Alarm> alarmList = new ArrayList<Alarm>();
		Alarm thisAlarm = new Alarm();
		//convert all current alarms to reading objects
		for(int i = 0; i < currentAlarms.size(); i++){ 
			//if the current alarm is in the requesting client's specified LMC zone
			if(currentAlarms.get(i).aReading.zone.equals(district)){
				currentAlarms.get(i).aReading.isResolved = true;
				thisAlarm.isResolved = true;
				thisAlarm.date = currentAlarms.get(i).aReading.date;
				thisAlarm.level1 = currentAlarms.get(i).aReading.level1;
				thisAlarm.level2 = currentAlarms.get(i).aReading.level2;
				thisAlarm.time = currentAlarms.get(i).aReading.time;
				thisAlarm.sensor = currentAlarms.get(i).aReading.sensor;
				thisAlarm.zone = currentAlarms.get(i).aReading.zone;
				alarmList.add(thisAlarm);
			}
		}
		//return (send) the array to the client that called this method
		Alarm[] alarmL = new Alarm[alarmList.size()];
		allAlarmListHolder.value = alarmList.toArray(alarmL);
		return allAlarmListHolder.value;
	}
	
	//check if a user is registered as an agency or member of the public.
	//read: check if the userID contains 'police' or 'agency' or 'government' (arbitrary buzzwords set in RMCClient!)
	@Override
	public boolean checkUser(String userID) {
		for(int i = 0; i < agencyList.size(); i++){
			parent.addMessage("Checking user.\n");
			if(agencyList.get(i).getName().equals(userID)){
				parent.addMessage("Authorised user found!\n");
				//yes, this user is 'authorised'
				return true;
			}
		}
		//no, this user is not 'authorised'
		return false;
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
public class RMCServer extends JFrame {
    private JPanel panel;
    private JScrollPane scrollpane;
    private JButton getItButton;
    private static JTextArea textarea;

    public RMCServer(String[] args){
	try {
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null);
	    
	    // get reference to rootpoa & activate the POAManager
	    POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
	    rootpoa.the_POAManager().activate();
	    
	    // create servant and register it with the ORB
	    RMCServant helloRef = new RMCServant(this);
	    
	    // get the 'stringified IOR'
	    org.omg.CORBA.Object ref = rootpoa.servant_to_reference(helloRef);
	    String stringified_ior = orb.object_to_string(ref);
	    
    	// Save IOR to file
        BufferedWriter out = new BufferedWriter(new FileWriter("RMC.ref"));
        out.write(stringified_ior);
	    out.close();

	    // set up the GUI
	    textarea = new JTextArea(20,25);
	    scrollpane = new JScrollPane(textarea);
	    panel = new JPanel();

	    panel.add(scrollpane);
	    scrollpane.setBounds(12, 5, 368, 352);
	    {
	    	getItButton = new JButton();
	    	panel.add(getItButton);
	    	getItButton.setText("Clear log");
	    	getItButton.setBounds(125, 369, 126, 23);
	    	getItButton.addActionListener (new ActionListener() {
			    public void actionPerformed (ActionEvent evt) {
			    	textarea.setText("");

			    }
			});
	    }
	    getContentPane().add(panel, "Center");
	    panel.setLayout(null);

	    setSize(400, 500);
            setTitle("RMC");

            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );

	    
	    // wait for invocations from clients
	    textarea.append("Hello world! This is the RMC.\n");
	    textarea.append("All LMCServers will automatically connect to this server.\n");
	    
	    // remove the "orb.run()" command,
	    // or the server will run but the GUI will not be visible
	    // orb.run();
	    
	} catch (Exception e) {
	    System.err.println("ERROR: " + e);
	    e.printStackTrace(System.out);
	}
    }
    
    //for use in RMCServant to append messages to this GUI
    public void addMessage(String message){
    	textarea.append(message);
    }
    
    public static void main(String args[]) {
	final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
		public void run() {
		    new RMCServer(arguments).setVisible(true);
		}
	    });
    }
}


