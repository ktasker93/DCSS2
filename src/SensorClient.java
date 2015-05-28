/**
 * A class to create a SensorClient.
 * Grants the ability to view any Sensor that is currently hosted and switch sensors on and off.
 * Retrieves the current Reading object through the Sensor object reference.
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
public class SensorClient extends JFrame {
    private JPanel textpanel, buttonpanel;
    private JScrollPane scrollpane;
    private JButton getSensorButton;
    private JButton onePowerSwitch;
    private JButton twoPowerSwitch;
    private JTextField sensorIDInput;
    private JTextArea textarea;
    private JButton getItButton;

    public SensorClient(String[] args) {
	try {
	    // set up the GUI
	    textarea = new JTextArea(20,25);
	    textarea.setEditable(false);
	    scrollpane = new JScrollPane(textarea);
	    textpanel = new JPanel();
	    textpanel.add(scrollpane);
	    scrollpane.setBounds(12, 5, 368, 352);
	    {
	    	//Text field to input the name of a sensor to investigate.
	    	sensorIDInput = new JTextField();
	    	textpanel.add(sensorIDInput);
	    	sensorIDInput.setText("Sensor ID...");
	    	sensorIDInput.setBounds(48, 370, 102, 23);
	    }
	    {
	    	//button to return the reading of a sensor.
	    	//retrieves the sensor from SENSOR_[inputSensorName].ref and uses sensor.current() to retrieve info.
	    	getSensorButton = new JButton();
	    	textpanel.add(getSensorButton);
	    	getSensorButton.setText("Get Reading");
	    	getSensorButton.setBounds(162, 370, 78, 23);
	    	getSensorButton.addActionListener(new ActionListener() {
	    		public void actionPerformed(ActionEvent evt) {
	    			textarea.setText("");
	    			System.out.println("getSensorButton.actionPerformed, event="+evt);
	    			try{
	    			Sensor sensor = getSensor(sensorIDInput.getText().toString());
	    			if(sensor.power1() == false && sensor.power2() == false){
	    				textarea.append("This sensor is disabled.\nShowing last known reading instead.");
	    			}
	    		    textarea.append("Reading for \"" + sensorIDInput.getText().toString() + "\": \n");
	    		    Reading thisReading = sensor.currentReading();
	    		    textarea.append("Date: " + thisReading.date + "\nTimestamp: " + thisReading.time + "\nLevels: 1: " + thisReading.level1 + " 2: " + thisReading.level2 + "\n");
	    			textarea.append("Power: 1: " + sensor.power1() + " 2: " + sensor.power2() + "\n");
	    			if(sensor.power1() == true && sensor.current().level1 >= 70){
	    				textarea.append("WARNING: Flood risk! A LMC has been alerted.\n");
	    			}
	    			} catch(Exception e){
	    				e.printStackTrace();
	    				System.out.println("something went wrong, input sensor probably doesn't exist or isn't running!");
	    			}  
	    		}
	    	});
	    }
	    {
	    	onePowerSwitch = new JButton();
	    	textpanel.add(onePowerSwitch);
	    	onePowerSwitch.setText("Sensor 1 Power");
	    	onePowerSwitch.setBounds(162, 398, 120, 23);
	    	onePowerSwitch.addActionListener(new ActionListener() {
	    		public void actionPerformed(ActionEvent evt) {
	    			System.out.println("onePowerSwitch.actionPerformed, event="+evt);
	    			Sensor sensor = getSensor(sensorIDInput.getText().toString());
	    			if(sensor.power1() == true){
	    				sensor.power1(false);
		    			textarea.append("\n" + sensorIDInput.getText().toString() + ":1 is now switched off.\n");
	    			} else {
	    				sensor.power1(true);
		    			textarea.append("\n" + sensorIDInput.getText().toString() + ":1 is now switched on.\n");
	    			}
	    		}
	    	});
	    }
	    {
	    	twoPowerSwitch = new JButton();
	    	textpanel.add(twoPowerSwitch);
	    	twoPowerSwitch.setText("Sensor 2 Power");
	    	twoPowerSwitch.setBounds(162, 426, 120, 23);
	    	twoPowerSwitch.addActionListener(new ActionListener() {
	    		public void actionPerformed(ActionEvent evt) {
	    			System.out.println("onePowerSwitch.actionPerformed, event="+evt);
	    			Sensor sensor = getSensor(sensorIDInput.getText().toString());
	    			if(sensor.power2() == true){
	    				sensor.power2(false);
		    			textarea.append("\n" + sensorIDInput.getText().toString() + ":2 is now switched off.\n");
	    			} else {
	    				sensor.power2(true);
		    			textarea.append("\n" + sensorIDInput.getText().toString() + ":2 is now switched on.\n");
	    			}
	    		}
	    	});
	    }


	    getContentPane().add(textpanel, "Center");
	    textpanel.setLayout(null);

	    setSize(400, 500);
            setTitle("Sensor Client");

            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );

	    textarea.append("Client started.  Click the button to contact relay...\n\n");
	    
	} catch (Exception e) {
	    System.out.println("ERROR : " + e) ;
	    e.printStackTrace(System.out);
	}
    }



    public static void main(String args[]) {
	final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
		public void run() {
		    new  SensorClient(arguments).setVisible(true);
		}
	    });
    }
    
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
    		System.out.println("Selected sensor.ref file not found (or it has been deleted since this sensor started running.)");
    		textarea.append("Sensor not found!");
    	}
    	return null;
    
    }
}
