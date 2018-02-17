package Sensor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import Encryption.*;
import Driver.Driver;
import Driver.StandardInListener;
import Driver.Start;

public class Sensor extends Thread implements Runnable
{
	public static final String myClassName = "Sensor";
	public volatile Encryption ENCRYPTION = null;
	
	
	public static final String SENSOR_COMMAND_VERSION = "1.0";
	public String SENSOR_COMMAND = "";
	public static final String delimiter = "\t";
	public static Driver driver = new Driver();
	
	public volatile String encryption_line = "";
	
	public volatile String myInterface = "";
	
	/**e.g. tshark or c:\program files\wireshark\tshark.exe*/
	String sensor_command_header = "";
	
	public volatile int parser_index = 0;
	
	public volatile static LinkedList<Sensor> list_sensors = new LinkedList<Sensor>();
	
	public volatile String last_sensor_update = "Awaiting input...";
	public volatile DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy" + "-" + "MM" + "-" + "dd" + "-" + "HH" + "mm" + ":" + "ss");
	public volatile LocalDateTime date_now = LocalDateTime.now();
	
	public Sensor(String sensor_cmd, String iface)
	{	
		try
		{
			SENSOR_COMMAND = sensor_cmd;
			myInterface = iface;
			list_sensors.add(this);
			
			if(Start.encryption_key != null && !Start.encryption_key.trim().equalsIgnoreCase("null"))
			{
				//set the new key!
				ENCRYPTION = new Encryption(Start.encryption_key, Encryption.default_iv_value);
			}
			
			this.start();
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "Constructor - 1", e);
		}
	}
	
	public void run()
	{
		try
		{
			driver.directive("Attempting to establish sensor across interface --> " + myInterface);
						
			//
			//START SENSOR!
			//
			driver.directive("EXECUTING -->" + SENSOR_COMMAND);
			
			Process p = null;
			
			if(driver.isWindows)
			{
				p = Runtime.getRuntime().exec("cmd.exe /C " + "\"" + SENSOR_COMMAND + "\"");
			}
			else if(driver.isLinux)
			{
				String [] cmd = new String [] {"/bin/bash", "-c", SENSOR_COMMAND};
				p = Runtime.getRuntime().exec(cmd);	
				
				/*ProcessBuilder pb = new ProcessBuilder("tshark", "-i", "eth0");
				pb.redirectErrorStream(true);
				p = pb.start();*/
			}
			
			
			BufferedReader brIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader brIn_Error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			String line = "";
			
			//
			//clearn previous instances
			//
			try	{	Start.worker.processInterrupt_60_MINS();	}	 catch(Exception e){}
			
			//
			//INFINITE LISTEN TO SENSOR
			//
			while((line = brIn.readLine()) != null)
			{
				if(line == null || line.trim().equals(""))
					continue;
				
				//DO NOT TRIM LINE!
				line = SENSOR_COMMAND_VERSION + delimiter + Start.sensor_name + delimiter + this.myInterface + delimiter + line;//DO NOT TRIM LINE!
				
				//
				//NOTIFY USER
				//
				sop(line);
				
				//
				//TRANSMIT LINE!
				//
				send(line);				
				
				last_sensor_update = getTime_Current_hyphenated_with_seconds();
				
				
			}
			
			driver.directive("\nReading error stream...");
			
			while((line = brIn_Error.readLine()) != null)
			{
				if(line == null || line.trim().equals(""))
					continue;
				
				driver.directive("ERROR -->" + line);
				
				if(line.toLowerCase().contains("tshark") && line.toLowerCase().contains("is no longer running"))
				{
					driver.directive("ERROR!\n\nIt appears the packet capture daemon is no longer running!\n\n" + line);
				}
								
			}
			
			//a problem here, this punts out of the infinite while loop above in wireless monitor mode at times. if so, detect, and restart the sensor.
			try	{	list_sensors.remove(this);	}	catch(Exception e){}
			
			if(this.myInterface != null && this.myInterface.trim().startsWith("wlan"))
			{
				//restart the wireless sensor
				
				try	{	p.destroyForcibly();}	catch(Exception e){}
				try	{	brIn.close();} catch(Exception e){}
				try	{	brIn_Error.close();} catch(Exception e){}
				
				driver.directive("Punt! I've detected the wlan sensor has stopped. I will attempt to restart the sensor now...");
				Sensor sensor = new Sensor(SENSOR_COMMAND, this.myInterface);
			}
			
			driver.directive("Closing sensor for interface [" + this.myInterface + "]");
			
			try	{	System.gc();	} catch(Exception e){}
			
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "run", e);
		}
	}
	
	public boolean send(String line)
	{
		try
		{		
			
			//assume encryption
			encryption_line = line;
			
			if(this.ENCRYPTION != null)
				encryption_line = ENCRYPTION.encrypt(line);
			
			for(ThdSensorSocket skt : ThdSensorSocket.ALL_CONNECTIONS)
			{				
				skt.send(encryption_line);											
			}
			
			return true;
		}
		catch(ConcurrentModificationException con)
		{
			driver.directive("Holdfast, I am currently executing from the list at the same time");						
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "send", e);
		}
		
		return false;
	}
	
	public boolean sop(String line)
	{
		try
		{
			if(line == null || line.trim().equals(""))
				return false;
			
			line = line.trim();
			
			if(Driver.sensor_output_enabled)
			{
				driver.sop(line);
			}
			
			return true;
		}
		
		catch(Exception e)
		{
			driver.eop(myClassName, "sop", e);
		}
		
		return false;
	}
	
	
	
	
	
	public String getTime_Current_hyphenated_with_seconds()
	{
		try
		{						
			return dateFormat.format(LocalDateTime.now());
		}
		catch(Exception e)
		{
			driver.sop("Invalid date specified - -" + " it does not a proper date was selected");
			//Drivers.eop("Drivers", "getTime_Specified_Millis", "", e, false);
		}
		
		
		return ""+ System.currentTimeMillis();
	}
	
	
	
	
	
	
	
	
	
	
}
