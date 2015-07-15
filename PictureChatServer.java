import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;


public class PictureChatServer 
{
	ServerSocket ss;
	ConcurrentHashMap<String, InetAddress> whosIn = new ConcurrentHashMap<String, InetAddress>();
	ConcurrentHashMap<String, String> passwords = new ConcurrentHashMap<String, String>();
	ConcurrentHashMap<String, Vector<Object>> savedMessages = new ConcurrentHashMap<String, Vector<Object>>();
	ConcurrentHashMap<String, Vector<InetAddress>> ignoreMessagesFrom = new ConcurrentHashMap<String, Vector<InetAddress>>();
	Vector<String> whosNotIn = new Vector<String>();
			

	public PictureChatServer() throws Exception 
	{
		//Construct server socket
		ss = new ServerSocket(7777);
		
		// Retrieve the passwords collection from disk.
		try 
		{
			FileInputStream fis = new FileInputStream("passwords.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			passwords = (ConcurrentHashMap)ois.readObject();
			ois.close();
			System.out.println("Contents of the passwords collection is:");
			System.out.println(passwords);
			Set<String> previousClients = passwords.keySet();
			for(String chatName : previousClients)
				whosNotIn.add(chatName);
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("passwords.ser is not found, so an empty collection will be used.");
		}	
		catch (ClassNotFoundException cnfe) {} // not going to happen!
		
		// Retrieve the saved messages from the disk.
		try
		{
			FileInputStream fis = new FileInputStream("SavedMessages.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			savedMessages = (ConcurrentHashMap)ois.readObject();
			ois.close();
			System.out.println("Contents of the SavedMessages collection is:");
			System.out.println(savedMessages);
			
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("SavedMessages.ser is not found, so an empty collection will be used.");
			Set<String> previousClients = passwords.keySet();
			for(String chatName : previousClients)
				savedMessages.put(chatName, new Vector());
		}	
		catch (ClassNotFoundException cnfe) {} // not going to happen!
		
		try
		{
			FileInputStream fis = new FileInputStream("IgnoreMessages.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			ignoreMessagesFrom = (ConcurrentHashMap)ois.readObject();
			ois.close();
			System.out.println("Contents of the ignoreMessages collection is:");
			System.out.println(ignoreMessagesFrom);
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("IgnoreMessages.ser is not found, so an empty collection will be used.");
			Set<String> previousClients = passwords.keySet();
			for(String chatname : previousClients)
				ignoreMessagesFrom.put(chatname, new Vector());
		}
		catch (ClassNotFoundException cnfe) {}
		// Print out relevant network info
		System.out.println("Message chat server is up at " + InetAddress.getLocalHost().getHostAddress() 
				+ " on port " + ss.getLocalPort());
	}

	public static void main(String[] args) 
	{
		//Print name to screen
		System.out.println("Vijay Thiagarajan, id : vkthiaga");
		// test for input params
		if(args.length > 0)
			System.out.println("The program does not take any input parameters!");
		
		try
		{
			PictureChatServer cs = new PictureChatServer();
			cs.receive();
		}
		
		catch(Exception e)
		{
			System.out.println(e);
		}

	}
	
	private void receive()
	{
		// capture the receive thread
		while(true)
		{
			try 
			{
				Socket s = ss.accept();
				InetAddress clientAddress = s.getInetAddress();
				System.out.println("A client has connected from " + clientAddress);
				
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				Object messageFromClient = ois.readObject();
				if (messageFromClient instanceof String)//JOIN or sendToAll CHAT?
				{
				   String message = (String) messageFromClient;
				   //Is this a message from a real PrivateChatClient?
				   char firstCharacter = message.charAt(0);
				   if (firstCharacter != '\u007F')//unicode Circle-C
				   {// WRONG NUMBER OR HACKER
				      System.out.println("Unrecognized message received: " + message);
				      ois.close(); // hang up
				      continue;//Go back to loop top to receive next client.
				   }
				   message = message.substring(1).trim();// drop 1st character 
				   //Is this a JOIN or a chat message?
				   if (message.startsWith("JOIN"))
				   {
				      joinProcessing(s, message.substring(4).trim());//drop "JOIN"
				      continue;//Go back to loop top to receive next client.
				   }         
				   else // must be a chat message for everyone  
				   {
				      chatProcessing(message);
				      continue;//Go back to loop top to receive next client.
				   }
				}

				if (messageFromClient instanceof Object[])//A Private Message
				{
				   Object[] privateMessageArray = (Object[]) messageFromClient;
				   // print out contents of String array
				   System.out.println("The contents of this private message is: ");
				   for(Object i : privateMessageArray)
					   System.out.println(i);
				   
				   // call appropriate method
				   sendPrivateMessage(privateMessageArray);
				   continue;
				}

				if (messageFromClient instanceof Vector)//A Save Message
				{
					Vector<Object> saveMessageVector = (Vector<Object>) messageFromClient;   
					//print out the contents of Vector
					System.out.println("The contents of this vector is: ");
					System.out.println(saveMessageVector);
					
					//call appropriate method
					saveMessage(saveMessageVector);
					continue;
				}
				if (messageFromClient instanceof ImageIcon)
				{
					ImageIcon pic = (ImageIcon)messageFromClient;
					sendToAll(pic);
					continue;
				}
				
				//ignore list
				if (messageFromClient instanceof List)
				{
					List<String> annoyingPeople = (List)messageFromClient;
					String sender = annoyingPeople.remove(annoyingPeople.size()-1);
					for(String a : annoyingPeople)
					{
						//add the annoyer's associated network address
						Vector<InetAddress> annoyers = ignoreMessagesFrom.get(sender);
						annoyers.add(whosIn.get(a));
					}
					
					//Save the new info to the system.
					FileOutputStream fos = new FileOutputStream("IgnoreMessages.ser");
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(ignoreMessagesFrom);
					oos.close();
					continue;
				}
				
				System.out.println("Unexpected object received: " + messageFromClient);
			} 
			catch (Exception e) 
			{
				continue;
			}
		}
	}
	
	private void joinProcessing(Socket s, String chatNameAndPassword) 
	{
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			int blankOffset = chatNameAndPassword.indexOf(" ");
			
			//test for no blank characters
			if(blankOffset < 0)
			{
				oos.writeObject("Invalid Format");
				oos.close();
				return;
			}
			
			String chatName = chatNameAndPassword.substring(0, blankOffset).toUpperCase();
			String password = chatNameAndPassword.substring(blankOffset).trim();
			
			if (!passwords.containsKey(chatName)) //never been in!
			{ // add and join Bubba
			   passwords.put(chatName,password); //add Bubba to pw collection
			   whosIn.put(chatName, s.getInetAddress()); //add Bubba to who's in now

			   // Send join confirmation to Bubba
			   oos.writeObject("Welcome " + chatName + " !");
			   oos.close(); // We're done!

			   // Save the updated passwords collection on disk.
			   FileOutputStream fos = new FileOutputStream("passwords.ser");
			   ObjectOutputStream pwoos = new ObjectOutputStream(fos);
			   pwoos.writeObject(passwords);
			   pwoos.close();

			   // Tell everyone that Bubba just joined the chat room.
			   sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
			   
			   // Send the new list of people to everyone in the chat room
			   String[] chatNameList = whosIn.keySet().toArray(new String[0]);
			   sendToAll(chatNameList);
			   
			   return; 
		   }
			
			String storedPassword = passwords.get(chatName);
			if (password.equals(storedPassword))//case-sensitive compare
			{ 		// rejoin Bubba
		         // Send join confirmation to Bubba
		         oos.writeObject("Welcome " + chatName + " !");
		         oos.close(); // We're done!

		         // See if Bubba is rejoining from a new location:
		         if (whosIn.containsKey(chatName))//Bubba is already in!
		             //We do need to update where he is now.
		        	 whosIn.replace(chatName, s.getInetAddress());
		         	 //but no need to "notify" everyone - Bubba never left!
		         else // Bubba was NOT already in the chat room.
	             {
		        	 whosIn.put(chatName, s.getInetAddress());//add Bubba to who's in now
		             // Tell everyone that Bubba just joined the chat room.
		             sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
	             }
		         // Send the saved messages if any, to the latest member
		         Vector<Object> messages = savedMessages.get(chatName);
		         if(!messages.isEmpty())
		         {
		        	 try
		        	 {
			        	 while(!messages.isEmpty())
			        	 {
			        		 Socket s1  = new Socket(whosIn.get(chatName), 5678);
			        		 ObjectOutputStream oos2 = new ObjectOutputStream(s1.getOutputStream());
			        		 oos2.writeObject(messages.remove(0));
			        		 oos2.close();
			        	 }
			        	 FileOutputStream fis = new FileOutputStream("savedMessages.ser");
			        	 ObjectOutputStream oos3 = new ObjectOutputStream(fis);
			        	 oos3.writeObject(savedMessages);
			        	 oos3.close();
		        	 }
		        	 catch(Exception e)
		        	 {
		        		 System.out.println("Error: " + e);
		        	 }
		         }
		         
		         // Send the new list of people to everyone in the chat room
		         String[] chatNameList = whosIn.keySet().toArray(new String[0]);
		         sendToAll(chatNameList);
		         
		         // remove the name from the whosNotIn list
		         whosNotIn.remove(chatName);
		         sendToAll(whosNotIn);
		         return; 
	         }
			
			// If we are still executing at this point, it is because
		    // we did not rejoin Bubba because his password is bad!
		    // Send join failure error message to Bubba:
		    oos.writeObject("Submitted password " + password + " does not match stored password for " + chatName);
		    oos.close(); // We're done!
		}
		catch(IOException ioe)
		{
			System.out.println("Join response failure " + ioe);
		}
		
	}
	
	private void chatProcessing(String chatMessageFromClient)
	{
		int blankOffset = chatMessageFromClient.indexOf(" ");
		
		if(blankOffset < 0)
		{
			System.out.println("Invalid Format, received message: " + chatMessageFromClient);
			return;
		}
		String chatName = chatMessageFromClient.substring(0, blankOffset).toUpperCase();
		
		if(!whosIn.containsKey(chatName))
		{
			System.out.println("Invalid username, received message: " + chatMessageFromClient);
		}
		String chatMessage = chatMessageFromClient.substring(blankOffset).trim();
		
		sendToAll(chatName + " says " + chatMessage);
	}
	
	private void sendToAll(Object message)
	{
		// Send this message to everyone in the whosIn collection.
		// Ask the keyed collection for a list of it's keys:
		String[] clientList = whosIn.keySet().toArray(new String[0]);
		String sender = null;
		if(!(message instanceof String[] || message instanceof Vector))
		{
			//isolate sender chatname
			String msg = message.toString();
			int blank = msg.indexOf(' ');
			sender = msg.substring(0, blank);
		}
		String leaveList = "";
	  	for (String chatName : clientList)
	  	{
	      InetAddress clientAddress = whosIn.get(chatName);
	      //check if address is in the annoyers list
	      if(!(message instanceof String[] || message instanceof Vector))
	      {
	    	  Vector<InetAddress> annoyers = ignoreMessagesFrom.get(chatName);
	    	  if(annoyers.contains(whosIn.get(sender)))
	    		  continue; // don't send the message.
	      }
	      try 
	      {
			  Socket s = new Socket(clientAddress,5678);
			  ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			  oos.writeObject(message);
			  oos.close();
		  }
	      catch(IOException ioe)//this client must have gone down without leaving...
		  {
	    	  whosIn.remove(chatName);
	    	  whosNotIn.add(chatName);
	    	  System.out.println(chatName + " has left.");
	    	  leaveList += chatName + " ";
		  }
	  	}
	  	
		// The calling join code will send updated whosIn and whosNotIn lists to everyone.
		// This notifies everyone of who left (different from whosNotIn).
	  	if (leaveList.length() > 0)
	  	{	
	  		sendToAll("Goodbye to " + leaveList
	  				+ "who has left the chat room.");
	  		sendToAll(whosNotIn); // Vector
	  		sendToAll(whosIn.keySet().toArray(new String[0]));//String[] array
	  	}
	}
	
	void sendPrivateMessage(Object[] messageAndRecipients)
	{
		String leaveList = "";
		// Check if confirmation message
		String confirm = (String)messageAndRecipients[0];
		if(confirm.startsWith("Confirmation"))
		{
			String sender = (String)messageAndRecipients[1];
			String receiver = (String)messageAndRecipients[2];
			InetAddress senderAddress = whosIn.get(sender);
			String confirmation;
			if(messageAndRecipients.length == 3)
				confirmation = "Message confirmation from " + receiver;
			else
				confirmation = "Message confirmation from " + receiver + ". picture filename: "
					+ (String)messageAndRecipients[3];
			try
			{
				Socket s = new Socket(senderAddress, 5678);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(confirmation);
				oos.close();
			}
			catch(IOException ioe)
			{
				whosIn.remove((String)messageAndRecipients[1]);
				whosNotIn.add((String)messageAndRecipients[1]);
				System.out.println(messageAndRecipients[1] + " has left.");
				leaveList += messageAndRecipients[1] + " ";
			}
		}
		// Picture/text message to be sent
		else
		{
			String sender = (String)messageAndRecipients[0];
			// list of people it was sent to
			String receiverList = "This message was sent to the following people: ";
			for(int j = 2; j < messageAndRecipients.length; j++)
			{
				receiverList += (String)messageAndRecipients[j] + " ";
			}
			String message;
			// Text message
			if(messageAndRecipients[1] instanceof String)
			{
				String chatToSend = (String)messageAndRecipients[1];
				message = "Private " + sender + " : "
						+ chatToSend;
				
				// send the messages);
				for(int i = 2; i < messageAndRecipients.length; i++)
				{
					String chatName = (String)messageAndRecipients[i];
					
				    //check if address is in the annoyers list
				    Vector<InetAddress> annoyers = ignoreMessagesFrom.get(chatName);
				    if(annoyers.contains(whosIn.get(sender)))
				    	continue; // don't send the message.
				    
				    
					// Get IP address of the recipient
					InetAddress InetClientAddress = whosIn.get((String)chatName);
					try
					{
						Socket s = new Socket(InetClientAddress, 5678);
						ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
						oos.writeObject(message + " \n"  + receiverList);
						oos.close();
					}
					
					catch(IOException ioe)
					{
						whosIn.remove((String)chatName);
						whosNotIn.add((String)chatName);
						System.out.println(chatName + " has left.");
						leaveList += (String)chatName + " ";
					}
				}
			}
			//Picture message
			else
			{
				ImageIcon pictureToSend = (ImageIcon)messageAndRecipients[1];
				String picDesc = pictureToSend.getDescription();
				pictureToSend.setDescription("(Private)" + picDesc + " " + receiverList);
				
				for(int i = 2; i < messageAndRecipients.length; i++)
				{
					String chatName = (String)messageAndRecipients[i];
					
				    //check if address is in the annoyers list
				    Vector<InetAddress> annoyers = ignoreMessagesFrom.get(chatName);
				    if(annoyers.contains(whosIn.get(sender)))
				    	continue; // don't send the message.
					
				    // Get IP address of the recipient
					InetAddress InetClientAddress = whosIn.get((String)chatName);
					try
					{
						Socket s = new Socket(InetClientAddress, 5678);
						ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
						oos.writeObject(pictureToSend);
						oos.close();
					}
					
					catch(IOException ioe)
					{
						whosIn.remove((String)chatName);
						whosNotIn.add((String)chatName);
						System.out.println(chatName + " has left.");
						leaveList += (String)chatName + " ";
					}
				}
			}
		}
		// The calling join code will send updated whosIn and whosNotIn lists to everyone.
		// This notifies everyone of who left (different from whosNotIn).
	  	if (leaveList.length() > 0)
	  	{	
	  		sendToAll("Goodbye to " + leaveList
	  				+ "who has left the chat room.");
	  		sendToAll(whosNotIn); // Vector
	  		sendToAll(whosIn.keySet().toArray(new String[0]));//String[] array
	  	}
	}
	
	void saveMessage(Vector<Object> messageAndRecipients)
	{
		//Remove and store the sender's name and message
		String sender = (String)messageAndRecipients.remove(0);
		Object message;
		String origPicDesc = null;
		if(messageAndRecipients.get(0) instanceof String)
		{
			message = (String)messageAndRecipients.remove(0);
		}
		else
		{
			message = (ImageIcon)messageAndRecipients.remove(0);
		}
		
		for(Object recipient : messageAndRecipients)
		{
			
			//check if address is in the annoyers list
		    Vector<InetAddress> annoyers = ignoreMessagesFrom.get(recipient);
		    if(annoyers.contains(whosIn.get(sender)))
		    	continue; // don't send the message.
		    
			if(savedMessages.get((String)recipient) == null)
			{
				savedMessages.put((String)recipient, new Vector()); //add newbie to the SM collection
			}
			Vector<Object> recipientVector = savedMessages.get((String)recipient);
			if(message instanceof String)
				recipientVector.add("Saved message from " + sender + ": " + message + " on " +
					new Date());
			else
			{
				ImageIcon pic = (ImageIcon)message;
				origPicDesc = pic.getDescription();
				pic.setDescription(origPicDesc + " on " + new Date());
				recipientVector.add(pic);
			}
		}
		
		try
		{
			FileOutputStream fos = new FileOutputStream("savedMessages.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(savedMessages);
			Socket s = new Socket(whosIn.get(sender),5678 );
			oos = new ObjectOutputStream(s.getOutputStream());		
			oos.writeObject("Message saved for " + messageAndRecipients);
			oos.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: " + e);
		}
		ImageIcon pic = (ImageIcon)message;
		pic.setDescription(origPicDesc);
	}

}