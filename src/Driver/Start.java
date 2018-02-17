package Driver;

import java.io.*;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import Worker.*;
import Sensor.*;


public class Start 
{
	public static final String myClassName = "Start";
	public static volatile Driver driver = new Driver();
	
	public static volatile BufferedReader brIn = null;
	
	public static volatile String encryption_key = null;
	public static volatile String sensor_name = Driver.NAME + "_" + System.currentTimeMillis();
	
	public static volatile String TSHARK_RUN_COMMAND = "tshark" ;
	public static final String SENSOR_PARAMETERS = "-T fields -e frame.time -e ip.proto -e eth.src -e ip.src -e tcp.srcport -e udp.srcport -e _ws.col.Protocol -e eth.dst -e ip.dst -e tcp.dstport -e udp.dstport -e dns.qry.name -e http.referer -e http.request.full_uri -e http.request -e http.cookie -e _ws.col.Info -e http.host -e http.user_agent";

	public static volatile String arg = "";
	public static volatile String specific_interface = null;
	public volatile SensorServerSocket svrskt = null;
	public static volatile int specific_SENSOR_port = SensorServerSocket.DEFAULT_PORT;
	public static volatile ThdWorker worker = new ThdWorker();
	public static volatile StandardInListener std_in = null;

	public static String [] args = null;
	
	public Start(String [] argv)
	{
		try
		{
			args = argv;
			initialize();			
		}
		catch(Exception e)
		{
			
		}
	}
	
	
	public boolean initialize()
	{
		try
		{
			//establish the bufferedreader on the standard_in. 
			//procure arguments from user
			//when finished, transfer control to StandardInListener to handle remainder of user input
									
			//notify user
			driver.directivesp("Welcome to " + Driver.FULL_NAME + " by \nSolomon Sonya @Carpenter and Suhail Mushtaq\n\n");
			
			//launch bufferedreader
			String line = "";
			brIn = new BufferedReader(new InputStreamReader(System.in));
			
			this.analyze_input(args);
			
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "initialize", e);
		}
		
		return false;
	}
	
	public boolean configure_tshark_execution_path()
	{
		try
		{
			if(driver.isLinux)
			{
				//execute which cmd to ensure we have tshark configured on the machine
				try	
				{
					Process p = Runtime.getRuntime().exec("which tshark");
					
					BufferedReader brIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
					BufferedReader brIn_Error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					
					String tshark_path = brIn.readLine();
					String error = brIn_Error.readLine();
					
					if(tshark_path == null || tshark_path.trim().equals(""))
					{
						driver.directivesp("ERROR! I could not locate path to \"tshark\"! Your system may not be configured properly. \nPlease install tshark before using this sensor!");
						
						if(error != null && !error.trim().equals(""))
						{
							driver.directivesp("Error Message: \"" + error + "\"");
						}
						
						driver.directivesp("ERROR! I cannot find path to tshark.exe. \nPlease configure your system appropriately and then try again in order to ensure\nproper execution of " + driver.FULL_NAME);
					}
					
					else
					{
						//driver.directivesp("tshark was located at -->" + tshark_path);
						TSHARK_RUN_COMMAND = "tshark" ;
					}
					


				}
				
				catch(Exception e)
				{
					driver.directivesp("\nPUNT! I encountered an error when attempting to execute the which command\n!");
				}
				
			}
			
			else if(driver.isWindows)
			{
				//check program files for tshark, query user for tshark file if not found
				
				Process p = Runtime.getRuntime().exec("cmd.exe /C \"echo %programfiles%\\wireshark\\tshark.exe\"");
				
				BufferedReader brIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader brIn_Error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				
				String tshark_path = brIn.readLine();
				String error = brIn_Error.readLine();
				
				if(tshark_path == null || tshark_path.trim().equals(""))
				{
					driver.directivesp("ERROR! I could not locate path to \"tshark\"! Your system may not be configured properly. \nPlease install tshark before using this sensor!");
					
					if(error != null && !error.trim().equals(""))
					{
						driver.directivesp("Error Message: \"" + error + "\"");
					}
				}
				
				//driver.directivesp("tshark was located at -->" + tshark_path);
				TSHARK_RUN_COMMAND = "\"" + tshark_path + "\"";										
				
				//ensure true file
				File fle_tshark = new File(tshark_path);
				
				if(!fle_tshark.exists() || !fle_tshark.isFile())
				{
					driver.directivesp("ERROR! I cannot location path to tshark.exe. I will query user for tshark.exe path now...");
					
					driver.directivesp("ERROR! I cannot find path to tshark.exe. \nPlease configure your system appropriately and then try again in order to ensure\nproper execution of " + driver.FULL_NAME);
					
					
				}
				else
				{
					TSHARK_RUN_COMMAND = "\"" + fle_tshark.getCanonicalPath() + "\"";
				}
				
				//driver.directivesp("FINAL PATH -->" + TSHARK_RUN_COMMAND);
				
			}
			
			else
			{
				driver.directivesp("PUNT! I am unable to determine path to wireshark on this operating system! Execution may be unstable as a result...");
			}
			
			
			
			return true;
		}
		
		catch(Exception e)
		{
			driver.eop(myClassName, "configure_tshark_execution_path", e, true);
		}
		
		return false;
	}
	
	public boolean print_interfaces()
	{
		try
		{
			configure_tshark_execution_path();
			
			LinkedList<String> list_interfaces = driver.list_interfaces(false, TSHARK_RUN_COMMAND);
			
			driver.print_linked_list("\nInterfaces found:", list_interfaces);
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "print_interfaces", e);
		}
		
		return false;
	}
	
	public boolean process_args(String [] args)
	{
		try
		{
			if(args.length > 0)
			{
				
				
				int i = 0;
				for(i = 0; i < args.length; i++)
				{
					arg = args[i];
					
					if(arg == null)
						continue;
					
					arg = arg.trim();
					
					//
					//interface
					//
					if(arg.toLowerCase().startsWith("-d") || arg.toLowerCase().startsWith("-list interface") || arg.toLowerCase().startsWith("-list_interface") || arg.toLowerCase().startsWith("list interface") || arg.toLowerCase().startsWith("list_interface"))
					{
						try
						{
							print_interfaces();
							//System.exit(0);
						}
						catch(Exception e){specific_interface = null;}
					}
					
					//
					//interface
					//
					if(arg.toLowerCase().startsWith("-i"))
					{
						try
						{
							specific_interface = args[++i];
							
							continue;
						}
						catch(Exception e){specific_interface = null;}
					}
					
					//
					//SENSOR port
					//
					else if(arg.toLowerCase().startsWith("-p"))
					{
						try
						{
							specific_SENSOR_port = Integer.parseInt(args[++i].trim());
							
							if(specific_SENSOR_port < 0)
								throw new Error("Invalid SENSOR port specified. Port should be greater than 0!");
							
							continue;
						}
						catch(Exception e)
						{
							driver.directive("ERROR! I am unable to determine your preferred SENSOR port. I am setting to default [" + SensorServerSocket.DEFAULT_PORT + "]");
							specific_SENSOR_port = SensorServerSocket.DEFAULT_PORT;
						}
					}
															
					//
					//
					//
					else if(arg.toLowerCase().startsWith("-e"))
					{
						try
						{
							encryption_key = args[++i];
							
							if(encryption_key != null)
							{
								encryption_key = encryption_key.trim();
								
								if(encryption_key.equals(""))
									encryption_key = null;
							}
							
							continue;
						}
						catch(Exception e){encryption_key = null;}
					}
					
					else if(arg.toLowerCase().startsWith("-n") || arg.toLowerCase().startsWith("-name") || arg.toLowerCase().startsWith("-sensor"))
					{
						try
						{
							sensor_name = args[++i];
							
							if(sensor_name != null)
							{
								sensor_name = sensor_name.replaceAll("\t", "").trim() + " ";
								
								if(sensor_name.trim().equals(""))
									sensor_name = "" + Driver.NAME + "_" + System.currentTimeMillis();
							}
							
							continue;
						}
						catch(Exception e){sensor_name = "" + Driver.NAME + "_" + System.currentTimeMillis();}
					}
				}
				
			}
			
			return true;
		}
		catch(Exception e)
		{
			driver.directivesp("Invalid input! Please check your arguments.  Restart program if necessary");
		}
		
		return false;
	}
	
	
	public boolean analyze_input(String [] args)
	{
		try
		{		
			//quick start, jump to avoid configuration if necessary
			if(args != null && args.length > 0)
			{
				for(String arg : args)
				{
					if(arg == null || arg.trim().equals(""))
						continue;
					
					//
					//interface
					//
					if(arg.toLowerCase().startsWith("-d") || arg.toLowerCase().startsWith("-list interface") || arg.toLowerCase().startsWith("-list_interface") || arg.toLowerCase().startsWith("list interface") || arg.toLowerCase().startsWith("list_interface"))
					{
						try
						{
							print_interfaces();
							System.exit(0);
						}
						catch(Exception e){specific_interface = null;}
					}
				}
			}
					
			//
			//process args
			//
			process_args(args);
			
			//
			//check if we're to query for encrytpion key
			//
			if(encryption_key == null)
			{
				driver.directivesp("Enter encryption key. You may also leave this field blank to set encryption disabled: ");
				String key = brIn.readLine();
				
				
				if(key != null && !key.trim().equals(""))
					encryption_key = key.trim();
			}
			
			//
			//check if we're to query for encrytpion key
			//
			if(sensor_name.startsWith(Driver.NAME))
			{
				driver.directivesp("\nSensor Name is currently set to [" + sensor_name + "].\nEnter a new name if you wish to change this designation. Press enter to continue: ");
				String name = brIn.readLine();
				
				
				if(name != null && !name.trim().equals(""))
					sensor_name = name.trim();
			}
			
			//
			//Configure Sensor
			//
			configure_sensor();
			
			
			
			
			
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "analyze_input", e);			
		}
		
		return false;
	}
	
	
	public boolean configure_sensor()
	{
		try
		{
			//
			//find tshark
			//
			driver.sop("\nConfiguring system. Please standby...");
			
			configure_tshark_execution_path();
			
			driver.sop("\nInstantiation of tshark set to be --> "  + TSHARK_RUN_COMMAND);
			
			//list interfaces
			driver.directive("\nDetecting network interfaces now.  Please take note and use one \nof the specific interface numbers below to establish the sensor...\n");
			
			LinkedList<String> list_interfaces = driver.list_interfaces(true, TSHARK_RUN_COMMAND);
			
			driver.print_linked_list("\nInterfaces found:", list_interfaces);
			
			//
			//query for interface
			//
			if((list_interfaces == null || list_interfaces.isEmpty()) && specific_interface == null)
			{
				driver.directivesp("\nPUNT!!!!!!! I was not able to locate a single interface to establish the sensors!");
			}
			
			if(specific_interface == null)
			{
				//query user if there is a specific interface
				try
				{
					
					driver.directivesp("\nPlease select the specific interface [NUMBER] to establish the sensor. \nEnter -1 or leave blank to listen across all interfaces: ");
					String selection = brIn.readLine();
															
					if(selection != null)
					{
						String interface_value =  selection;
						
						if(interface_value == null)
							specific_interface = null;
					
						interface_value = interface_value.trim();
						
						if(interface_value.equals(""))
							specific_interface = null;
						else
						{
							//get the interface number
							try
							{
								int interface_number = Integer.parseInt(interface_value.trim());
								
								if(interface_number < 0)
									throw new Exception("Port number must be non-negative integer 0!!!");
								
								specific_interface = ""+interface_number;
							}
							catch(Exception e)
							{
								driver.directive("\nPUNT! I did not receive a valid interface number. \nI will default to listen to all interfaces. If this is not\nacceptable, please re-establish the sensor...");
								specific_interface = null;
							}
						}
						
						
						
						
					}
					
				}
				catch(Exception ee){}
						
			}
			
			//
			//establish serversocket
			//
			if(specific_SENSOR_port == SensorServerSocket.DEFAULT_SENSOR_PORT)
			{
				
				driver.directivesp("\nSensor Listen Port (ServerSocket) is currently set to [" + specific_SENSOR_port + "].\nEnter a new port number if you wish to change this configuration: ");
				String port = brIn.readLine();
				
				
				//user canceled action, skip
				if(port !=  null && !port.trim().equals(""))
				{
					try
					{
						specific_SENSOR_port = Integer.parseInt(port.trim());
						
						if(specific_SENSOR_port < 0)
							throw new Exception("Invalid port number. You can not specify a value less than 0.");
					}
					catch(Exception e)
					{
						specific_SENSOR_port = SensorServerSocket.DEFAULT_SENSOR_PORT;
						driver.directivesp("Invalid port number received! I am setting back to default [" + specific_SENSOR_port + "]");
					}
				}
				
				//otw, we keep default port!
			}
			
			//
			//NOTIFY
			//
			driver.directive("\nVery good. Displaying initial configuration settings now...");
			driver.directive("Sensor Name -->" + sensor_name);
			
			if(specific_interface == null)
				driver.directive("Specific Sensor Interface -->" + " // ALL INTERFACES //");
			else
				driver.directive("Specific Sensor Interface -->" + specific_interface);
			driver.directive("Specific Port -->" + specific_SENSOR_port);
			
			if(encryption_key == null)
				driver.directivesp("encryption_key -->" + " // NOT SET //");
			else
				driver.directivesp("encryption_key -->" + encryption_key);
			
			
			//
			//Transfer control to StandardInListener and then start the sensors
			//
			std_in = new StandardInListener(brIn);
			
			//
			//ESTABLISH SERVER SOCKET
			//
			svrskt = new SensorServerSocket(specific_SENSOR_port);
			
			//
			//ESTABLISH SENSOR
			//
			if(specific_interface == null)
			{
				
				//configure sensor for each interface
				
				String iface = "";
				
				for(int i = 0; i < list_interfaces.size(); i++)
				{
					iface = list_interfaces.get(i);
					
					if(driver.isLinux)
					{
						//only auto instantiate lo, eth's, and wlan's
						if(iface.toLowerCase().startsWith("eth") || iface.toLowerCase().startsWith("wlan") || iface.toLowerCase().equals("lo") || iface.toLowerCase().startsWith("mon"))
						{
							String command = TSHARK_RUN_COMMAND + " -i " + "\"" + iface + "\" " + SENSOR_PARAMETERS;
							
							Sensor sensor = new Sensor(command, iface);
						}
					}
					
					if(driver.isWindows)
					{
						
						String command = TSHARK_RUN_COMMAND + " -i " + (i+1) + " " + SENSOR_PARAMETERS;
						Sensor sensor = new Sensor(command, iface);												
					}
					
					
				}
			}
			
			else if(specific_interface != null)
			{
				//configure sensor for specific interface
				String command = TSHARK_RUN_COMMAND + " -i " + "\"" + specific_interface + "\" " + SENSOR_PARAMETERS;
				Sensor sensor = new Sensor(command, specific_interface);
			}
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "configure_sensor", e);
		}
		
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}

