/**
 * @author Solomon Sonya
 */

package Driver;

import java.io.*;
import java.net.Socket;

import Encryption.Encryption;
import Sensor.Sensor;
import Sensor.SensorServerSocket;
import Sensor.ThdSensorSocket;

public class StandardInListener extends Thread implements Runnable
{
	public static final String myClassName = "StandardInListener";
	public static volatile Driver driver = new Driver();
	
	public volatile BufferedReader brIn = null;
	
	public StandardInListener(BufferedReader br)
	{
		try
		{			
			brIn = br;
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
			brIn = new BufferedReader(new InputStreamReader(System.in));
			
			String line = "";
			while((line = brIn.readLine()) != null)
			{
				determine_command(line);
			}
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "run", e);
		}
	}
	
	public boolean display_status()
	{
		try
		{
			driver.directive("\n /////////// STATUS ////////////");
			driver.directive(driver.FULL_NAME);
			
			driver.directive("");
			driver.directive("Time of First Start: " + driver.TIME_OF_FIRST_START);
			
			if(Start.encryption_key == null)
				driver.directive("Encryption Key --> " + "//NOT SET//");
			else
				driver.directive("Encryption Key --> " + Start.encryption_key);
			
			driver.directive("Sensor Name --> " + Start.sensor_name);
			
			driver.directive("Verbose is enabled: " + driver.output_enabled);
			driver.directive("Sensor Verbose is enabled: " + driver.sensor_output_enabled);
			driver.directive("Parser Verbose is enabled: " + driver.parser_output_enabled);
			
			
			if((SensorServerSocket.list_server_sockets == null || SensorServerSocket.list_server_sockets.isEmpty()))
			{
				driver.directive("No server sockets instantiated yet!");
			}
			else
			{
				for(SensorServerSocket svrskt : SensorServerSocket.list_server_sockets)
				{
					driver.directive("Sensor ServerSocket --> " + svrskt.get_status());
				}
				
				
			}
			
						
			driver.directive("");			
			driver.directive("Heap Size: " + Runtime.getRuntime().totalMemory()/1e6 + "(MB) Max Heap Size: " + Runtime.getRuntime().maxMemory()/1e6 + "(MB) Free Heap Size: " + Runtime.getRuntime().freeMemory()/1e6 + "(MB) Consumed Heap Size: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1e6 + "(MB)");
			driver.directive("");	
			
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "display_status", e);
		}
		
		return false;
	}
	
	public boolean toggle_verbose()
	{
		try
		{
			driver.output_enabled = !Driver.output_enabled;
			
			
			
			Driver.sensor_output_enabled = !Driver.sensor_output_enabled;
			
			if(Driver.sensor_output_enabled)
				driver.directive("Sensor output is enabled!");
			else
				driver.directive("Sensor output is disabled!");
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "toggle_verbose", e);
		}
		
		return false;
	}
	
	public boolean parser_connect(String location)
	{
		try
		{
			if(location == null || location.trim().equals(""))
			{
				driver.directive("ERROR! It appears you are missing location parameters for the connect command! Please try again!");
				return false;
			}
			
			location = location.trim();
			
			
			String array [] = null;
			
			if(location.contains(":"))
				array = location.split(":");
			else if(location.contains(","))
				array = location.split(",");
			else 
				array = location.split(" ");
			
			String address = array[0].trim();
			int port = Integer.parseInt(array[1].trim());
			
			if(address.equalsIgnoreCase("localhost") || address.equalsIgnoreCase("local host") || address.equalsIgnoreCase("-localhost") || address.equalsIgnoreCase("-local host"))
				address = "127.0.0.1";
			
			//Connect
			driver.directive("Attempting to connect sensor out to transport data to PARSER --> " + address + " : " + port);
			
			try
			{
				Socket skt = new Socket(address, port);
				
				ThdSensorSocket thd = new ThdSensorSocket(null, skt);
			}
			catch(Exception ee)
			{
				driver.directive("ERROR! I was unable to establish a connection to PARSER at --> " + address + " : " + port);
			}
			
			return true;
		}
		catch(Exception e)
		{
			driver.directive("ERROR! I was expecting command: parser_connect <ip address> <port>\nPlease try again...");
		}
		
		return false;
	}
	
	public boolean establish_server_socket(String port)
	{
		try
		{
			int PORT = Integer.parseInt(port.trim());
			
			if(PORT < 0)
			{
				throw new Exception("PORT number must be greater than 0!");
			}
			
			SensorServerSocket svrskt = new SensorServerSocket(PORT);
			
			return true;
		}
		catch(Exception e)
		{
			driver.directive("ERROR! Invalid port received. Please run command again and specify valid listen port!");
		}
		
		return false;
	}
	
	public boolean toggle_verbose_sensor()
	{
		try
		{
			driver.sensor_output_enabled = !driver.sensor_output_enabled;
			
			if(driver.sensor_output_enabled)
				driver.directive("Sensor output is enabled.");
			else
				driver.directive("Sensor output is disabled.");
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "toggle_verbose_sensor", e);
		}
		
		return false;
	}
	
	public static boolean set_encryption(String key)
	{
		boolean previous_output_state = driver.output_enabled;
		
		try
		{
			//disable output
			driver.output_enabled = false;
			
			if(key == null || key.trim().equals(""))
			{
				driver.directive("\nENCRYPTION HAS BEEN DISABLED!");	
				
				Start.encryption_key = null;
				
			}
			
			if(key != null && key.trim().equalsIgnoreCase("null"))
			{
				driver.directive("\n\nNOTE: your [null] parameter is a reserved word with this encryption command specifying to disable encryption");
				
				driver.directive("ENCRYPTION HAS BEEN DISABLED!");	
				
				Start.encryption_key = null;
				
				
			}
			
			if(key != null)
			{
				key = key.trim();
				
				Start.encryption_key = key;
				
				driver.directive("Encryption key has been set to [" + key + "]");
								
			}
			
			
			//set the encryption keys!
			
			for(Sensor sensor : Sensor.list_sensors)
			{
				try
				{
					if(key == null || key.trim().equals(""))
					{
						sensor.ENCRYPTION = null;	
					}
					else
					{
						//set the new key!
						sensor.ENCRYPTION = new Encryption(key, Encryption.default_iv_value);
					}
					
				}
				catch(Exception e)
				{
					continue;
				}
			}
			
			
			
			driver.output_enabled = previous_output_state;
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "set_encryption", e);
		}
		
		
		driver.output_enabled = previous_output_state;
		
		return true;
	}
	
	public boolean determine_command(String line)
	{
		try
		{
			if(line == null || line.trim().equals(""))
				return false;
			
			line = line.trim();
			
			if(line.equalsIgnoreCase("status") || line.equalsIgnoreCase("s") || line.equalsIgnoreCase("-status") || line.equalsIgnoreCase("-s") || (line.contains("display") && line.contains("status")))
				display_status();
			
			else if(line.equalsIgnoreCase("verbose") || line.equalsIgnoreCase("v") || line.equalsIgnoreCase("-verbose") || line.equalsIgnoreCase("-v"))
				toggle_verbose();							
						
			else if(line.toLowerCase().startsWith("parser_connect") || line.toLowerCase().startsWith("parser connect"))
				parser_connect(line.substring(14));
			
						
			else if(line.toLowerCase().startsWith("listen"))
				establish_server_socket(line.substring(6));
			
			else if(line.toLowerCase().startsWith("-listen"))
				establish_server_socket(line.substring(7));
			
			else if(line.toLowerCase().startsWith("-establish_server_socket"))
				establish_server_socket(line.substring(24));
			
			else if(line.toLowerCase().startsWith("establish_server_socket"))
				establish_server_socket(line.substring(23));
			
			else if(line.toLowerCase().startsWith("-establish server socket"))
				establish_server_socket(line.substring(24));
			
			else if(line.toLowerCase().startsWith("establish server socket"))
				establish_server_socket(line.substring(23));
			
			else if(line.toLowerCase().startsWith("verbose_sensor") || line.toLowerCase().startsWith("verbose sensor") || line.toLowerCase().startsWith("sensor_verbose") || line.toLowerCase().startsWith("sensor verbose") )
				toggle_verbose_sensor();			
			
			else if(line.toLowerCase().startsWith("-set_encryption") || line.toLowerCase().startsWith("-set encryption"))
				set_encryption(line.substring(15));
			
			else if(line.toLowerCase().startsWith("set_encryption") || line.toLowerCase().startsWith("set encryption"))
				set_encryption(line.substring(14));
			
			else if(line.toLowerCase().startsWith("encryption"))
				set_encryption(line.substring(10));				
			
			else if(line.toLowerCase().equalsIgnoreCase("log"))
				toggle_logging();
			
			else if(line.equalsIgnoreCase("disconnect"))
				disconnect_all();
			
			
			else
			{
				driver.directive("unrecognized command --> " + line);		
			}
				
		
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "determineCommand", e);
		}
		
		return false;
	}
	
	public boolean disconnect_all()
	{
		try
		{
			driver.directive("executing disconnection actions...");
			
			while(ThdSensorSocket.ALL_CONNECTIONS.size() > 0)
			{
				try
				{					
					ThdSensorSocket thd = ThdSensorSocket.ALL_CONNECTIONS.removeFirst();					
					thd.close_socket();
				}
				catch(Exception e)
				{
					continue;
				}
			}
						
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "disconnect_all", e);
		}
		
		return false;
	}
	
	public boolean toggle_logging()
	{
		try
		{
			Log.toggle_logging();
									
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "toggle_logging", e);
		}
		
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
