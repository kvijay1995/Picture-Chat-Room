import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class PictureChatClient implements ActionListener, ListSelectionListener
{
	
	String serverAddress;
	String chatName;
	String password;
	ServerSocket ss;
	File localDirectory = new File(System.getProperty("user.dir"));
	JList<String> whosInList = new JList<String>();
	JList<String> whosNOTInList = new JList<String>();
	JList<ImageIcon> myPicturesList = new JList<ImageIcon>();
	
	// Chat window GUI Objects
	JFrame whosInWindow = new JFrame("Who's In");
	JFrame whosNotInWindow = new JFrame("Who's NOT In");
	JFrame myPictureListWindow = new JFrame("Put pictures to send in " + localDirectory);
	JButton sendPrivateButton = new JButton("Send Private To");
	JButton saveMessageButton = new JButton("Save Message For");
	JButton clearWhosInButton = new JButton("CLEAR SELECTIONS");
	JButton clearWhosNotButton= new JButton("CLEAR SELECTIONS");
	JButton previewPicturesButton = new JButton("Preview Pictures To Send");
	JButton ignoreButton = new JButton("Ignore");
	
	JFrame       chatWindow       = new JFrame();
	JLabel       errorLabelField  = new JLabel(chatName + "'s Chat Room. " +
			" Move separator bar to give more space to in vs out.");
	JLabel		 myPictureWindowLabel = new JLabel("Select a picture.");
	JButton		 clearPicSelectButton = new JButton("Clear Selection");
	JButton      sendToAllButton  = new JButton("Send To All");     
	JRadioButton horizontalRButton= new JRadioButton("Horizontal Split",true);     
	JRadioButton verticalRButton  = new JRadioButton("Vertical Split");     
	ButtonGroup  splitButtonGroup = new ButtonGroup();      
	JTextArea    inChatArea       = new JTextArea("(enter chat here)");
	JTextArea    outChatArea      = new JTextArea();
	JScrollPane  inChatScrollPane = new JScrollPane(inChatArea);
	JScrollPane  outChatScrollPane= new JScrollPane(outChatArea);
	JScrollPane  myPictureScrollPane = new JScrollPane(myPicturesList);
	JSplitPane   chatPane         = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	                                    inChatScrollPane, outChatScrollPane);
	JPanel       bottomPanel      = new JPanel();
	JPanel  	 bottomPanel2	  = new JPanel();
	String newLine = System.lineSeparator();

	public PictureChatClient(String serverAddress, String chatName, String password) throws Exception 
	{
		this.serverAddress = serverAddress.trim();
		this.chatName = chatName.trim().toUpperCase();
		this.password = password.trim();
		ss = new ServerSocket(5678);
		System.out.println("Local Directory is " + localDirectory);
		
		// test cases for input params
		if((serverAddress == null) || serverAddress.trim().length() == 0 ||
				serverAddress.trim().contains(" "))
			throw new IllegalArgumentException("serverAddress is null, zero length or contains"
					+ " blank(s)");
		
		if((chatName == null) || chatName.trim().length() == 0 ||
				chatName.trim().contains(" "))
			throw new IllegalArgumentException("chatName is null, zero length or contains"
					+ " blank(s)");
		
		if((chatName.trim().equalsIgnoreCase("JOIN"))
				|| chatName.trim().equalsIgnoreCase("LEAVE"))
			throw new IllegalArgumentException("Chat name cannot be 'join' or 'leave'.");
		
		if((password == null) || password.trim().length() == 0 ||
				password.trim().contains(" "))
			throw new IllegalArgumentException("password is null, zero length or contains"
					+ " blank(s)");
		// set the GUI attributes
		whosInWindow.getContentPane().add(whosInList,"Center");
		whosInWindow.getContentPane().add(clearWhosInButton,"North");
		bottomPanel2.setLayout(new GridLayout(2, 1));
		bottomPanel2.add(sendPrivateButton);
		bottomPanel2.add(ignoreButton);
		whosInWindow.getContentPane().add(bottomPanel2,"South");
		
		whosNotInWindow.getContentPane().add(whosNOTInList,"Center");
		whosNotInWindow.getContentPane().add(clearWhosNotButton,"North");
		whosNotInWindow.getContentPane().add(saveMessageButton,"South");
		
		myPictureListWindow.getContentPane().add(clearPicSelectButton, "North");
		myPictureListWindow.getContentPane().add(myPictureScrollPane, "Center");
		myPictureListWindow.getContentPane().add(myPictureWindowLabel, "South");
		
		chatWindow.getContentPane().add(errorLabelField, "North");
		chatWindow.getContentPane().add(chatPane, "Center");
		chatWindow.setTitle("close the window to leave the Chat Room.");
		
		bottomPanel.add(sendToAllButton);
		bottomPanel.add(horizontalRButton);
		bottomPanel.add(verticalRButton);
		bottomPanel.add(previewPicturesButton);
		
		chatWindow.getContentPane().add(bottomPanel, "South");
		
		outChatArea.setEditable(false);
		
		chatPane.setDividerLocation(200);
		
		myPicturesList.setSelectionMode(0);
		
		
		// Color the buttons
		clearWhosInButton.setBackground(Color.yellow);
		clearWhosNotButton.setBackground(Color.yellow);
		sendPrivateButton.setBackground(Color.green);
		saveMessageButton.setBackground(Color.cyan);
		sendToAllButton.setBackground(Color.green);
		errorLabelField.setForeground(Color.red);
		myPictureWindowLabel.setForeground(Color.red);
		
		
		// Add action listeners for each of the buttons
		clearWhosInButton.addActionListener(this);
		clearWhosNotButton.addActionListener(this);
		sendPrivateButton.addActionListener(this);
		saveMessageButton.addActionListener(this);
		sendToAllButton.addActionListener(this);
		horizontalRButton.addActionListener(this);
		verticalRButton.addActionListener(this);
		previewPicturesButton.addActionListener(this);
		myPicturesList.addListSelectionListener(this);
		clearPicSelectButton.addActionListener(this);
		ignoreButton.addActionListener(this);
		
		splitButtonGroup.add(horizontalRButton);
		splitButtonGroup.add(verticalRButton);
		
		whosInWindow.setSize(200,400); // width, height   
	    whosNotInWindow.setSize(200,400);
	    chatWindow.setSize(600, 400);
	    myPictureListWindow.setSize(600, 400);
	    
	    whosInWindow.setLocation(600,0);//x,y   
	    whosNotInWindow.setLocation(800,0); 
	    myPictureListWindow.setLocation(1000, 0);
	    
	    whosInWindow.setVisible(true);   // false closes the window   
	    whosNotInWindow.setVisible(true);// just like hitting the "X"
	    chatWindow.setVisible(true);
	    
	    whosInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   
	    whosNotInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
	    inChatArea.setFont (new Font("default",Font.BOLD,20));
	    outChatArea.setFont(new Font("default",Font.BOLD,20));
	    chatWindow.setFont(new Font("default", Font.BOLD, 20));
	    inChatArea.setLineWrap(true);
	    outChatArea.setLineWrap(true);
	    inChatArea.setWrapStyleWord(true);
	    outChatArea.setWrapStyleWord(true);
		
		// Create a new socket
		System.out.println("Connecting to PictureChatServer at " + serverAddress + " on port 7777.");
		Socket s = new Socket(this.serverAddress.trim(), 7777);
		System.out.println("Connected to PictureChatServer");
		
		ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
		oos.writeObject("\u007FJOIN" + " " + chatName + " " + password);
		
		ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
		String serverReply = (String) ois.readObject();
		
		if(serverReply.startsWith("Welcome"))
			System.out.println(serverReply);
		else
			throw new IllegalArgumentException(serverReply);
	}

	public static void main(String[] args) {
		// Print name to the screen
		System.out.println("Vijay Thiagarajan, vkthiaga");
		
		// Verify that 3 command line parameters are provided
		if(args.length != 3)
		{
			System.out.println("Please provide only 3 arguments");
			System.out.println("Namely the server Address, Chat name and Password");
			return;
		}
		// Construct a chat client object
		try 
		{
			PictureChatClient cc = new PictureChatClient(args[0], args[1], args[2]);
			cc.receive();
		} 
		
		catch (Exception e) 
		{
			System.out.println(e);
		}
	}
	
	private void receive()
	{
		while(true)
		{
			try	
			{
				Socket s = ss.accept();
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				Object something = ois.readObject();
				
				if(something instanceof String)
				{
					String chatMessage = (String)something;
					// Check if its a private message and send confirmation
					if(chatMessage.startsWith("Private"))
					{
						//Drop the "Private" string
						chatMessage = chatMessage.substring(7).trim();
						int blankoffset = chatMessage.indexOf(" ");
						String[] confirmation = new String[3];
						// store sender name in first index, receiver name in the second
						confirmation[0] = "Confirmation";
						confirmation[1] = chatMessage.substring(0, blankoffset);
						confirmation[2] = this.chatName;
						try
						{
							s = new Socket(serverAddress, 7777);
							ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
							oos.writeObject(confirmation);
							oos.close();
						}
						catch(Exception e)
						{
							System.out.println("Error: " + e);
							continue;
						}
						// print message to console and client GUI
						System.out.println("Private message from " + chatMessage);
						
						outChatArea.append(newLine + "Private message from " + chatMessage);
						// scroll the outChatArea to the bottom
						outChatArea.setCaretPosition(outChatArea.getDocument().getLength());
						continue;
					}
					System.out.println("Received: " + chatMessage);
					
					outChatArea.append(newLine + chatMessage);
					// scroll the outChatArea to the bottom
					outChatArea.setCaretPosition(outChatArea.getDocument().getLength()); 
				}
				
				else if(something instanceof String[])
				{
					String[] clientlist = (String[])something;
					System.out.println("Currently in the chat room:");
					for (String chatName : clientlist)
						System.out.println(chatName);
					whosInList.setListData(clientlist);
				}
				
				else if(something instanceof Vector)
				{
					Vector<String> notInClientList = (Vector<String>)something;
					System.out.println("Currently NOT in the chat room:");
					for(String chatname : notInClientList)
						System.out.println(chatname);
					whosNOTInList.setListData(notInClientList);
				}
				
				else if(something instanceof ImageIcon)
				{
					ImageIcon pic = (ImageIcon)something;
					
					String picDesc = pic.getDescription().trim();
					System.out.println(picDesc);
					int blankOffset1 = picDesc.indexOf(' ');
					int blankOffset2 = picDesc.indexOf(' ', blankOffset1+1);
					String title;
					String chat;
					if(blankOffset2 > 0)
					{
						title = picDesc.substring(0, blankOffset2);
						chat = picDesc.substring(blankOffset2);
					}
					else
					{
						title = picDesc;
						chat = null;
					}
					//Check if private Image or send to All
					if(title.startsWith("(Private)"))
					{
						title = title.substring(9);
						int blankOffset = title.indexOf(" ");
						// Send confirmation back to sender
						String[] confirmation = new String[4];
						// store sender name in first index, receiver name in the second
						confirmation[0] = "Confirmation";
						//Sender
						confirmation[1] = title.substring(0,blankOffset);
						//Receiver
						confirmation[2] = this.chatName;
						//picture filename
						confirmation[3] = title.substring(blankOffset+1);
						try
						{
							s = new Socket(serverAddress, 7777);
							ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
							oos.writeObject(confirmation);
							oos.close();
						}
						catch(Exception e)
						{
							System.out.println("Error: " + e);
							continue;
						}
					}
					//Build image window
					JFrame imageWindow = new JFrame(title);
					JTextArea chatArea = new JTextArea(chat);
					chatArea.setEditable(false);
					JPanel refreshingPicturePanel = new RefreshingPicturePanel(pic.getImage());
					imageWindow.getContentPane().add(refreshingPicturePanel,"Center");
					imageWindow.getContentPane().add(chatArea, "South");
					imageWindow.setLocation(0, 400);
					imageWindow.setSize(200, 200);
					imageWindow.setVisible(true);
					imageWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					
				}
				
				else System.out.println("Unknown Object type received from the server: " + 
							something);
			}
			catch(IOException ioe)
			{
				System.out.println("Received error: " + ioe);
			}
			catch(ClassNotFoundException cfne)
			{}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) //call buttons here 
	{
		errorLabelField.setText("");
		if (ae.getSource() == clearWhosInButton)
		{
			// do whatever when this button is pushed.
			System.out.println("clearWhosInButton was pushed.");
			whosInList.clearSelection();
		}

		if (ae.getSource() == clearWhosNotButton)
		{
			// do whatever when this button is pushed.
			System.out.println("clearWhosNotButton was pushed."); 
			whosNOTInList.clearSelection();
		}

		if (ae.getSource() == sendPrivateButton)
		{
			// do whatever when this button is pushed.
			System.out.println("sendPrivateButton was pushed.");
			List<String> privateMessageList = whosInList.getSelectedValuesList();
			String chatToSend = inChatArea.getText().trim();
			if(chatToSend.length() == 0 && myPicturesList.isSelectionEmpty())
			{
				errorLabelField.setText("NO MESSAGE WAS ENTERED AND NO PICTURE WAS SELECTED.");
				System.out.println("No message or picture was entered.");
				return;
			}
			if (privateMessageList.isEmpty())
			{
				System.out.println("No private message recipients were selected.");
				errorLabelField.setText("NO PRIVATE MESSAGE RECIPIENTS WERE SELECTED.");
				return; // give the button it's thread back!
			}
			
			String[] privateMessageRecipients = privateMessageList.toArray(new String[0]);
			System.out.println("Recipients of this private message will be:");
			for (String recipient : privateMessageRecipients)
				System.out.println(recipient);
			
			ImageIcon picture = myPicturesList.getSelectedValue();
			String origPicDesc = null;
			
			System.out.println("Sending private message" + chatToSend + " " + picture);
			
			//Make a new String array of length messageRecipients.length + 2;
			Object[] messagePlusRecipients = new Object[2+privateMessageRecipients.length];
			
			// Copy relevant info to the array to be sent
			messagePlusRecipients[0] = this.chatName;
			for(int i=0; i < privateMessageRecipients.length; i++)
			{
				messagePlusRecipients[i+2] = privateMessageRecipients[i];
			}
			
			// Send list to server
			if(myPicturesList.isSelectionEmpty())
			{
				// Copy message only to this String array
				messagePlusRecipients[1] = chatToSend;	
			}
			else
			{
				//Send picture and text chat
				origPicDesc = picture.getDescription();
				picture.setDescription(chatName + " " + origPicDesc + " " + chatToSend);
				messagePlusRecipients[1] = picture;
			}
			
			try 
			{
				Socket s = new Socket(serverAddress, 7777);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(messagePlusRecipients);
			}
			catch(Exception e)
			{
				System.out.println("ERROR SENDING: " + e);
				return;
			}
			
			//Clear selections
			if(!myPicturesList.isSelectionEmpty())
			{
				picture.setDescription(origPicDesc);
				myPicturesList.clearSelection();
				myPictureWindowLabel.setText("Select a picture");
			}
			inChatArea.setText("");
		}

		if (ae.getSource() == saveMessageButton)
		{
			// do whatever when this button is pushed.
			System.out.println("saveMessageButton was pushed.");
			List<String> saveMessageList = whosNOTInList.getSelectedValuesList();
			if(saveMessageList.isEmpty())
			{
				errorLabelField.setText("NO SAVE-MESSAGE RECIPIENTS WERE SELECTED.");
				System.out.println("No save-message recipients were selected.");
				return;
			}
			String chatToSend = inChatArea.getText().trim();
			if(chatToSend.length() == 0 && myPicturesList.isSelectionEmpty())
			{
				errorLabelField.setText("NO MESSAGE WAS ENTERED or NO PICTURE WAS SELECTED.");
				System.out.println("No message or picture was entered.");
				return;
			}
			Vector<Object> saveMessageRecipients = new Vector<Object>(saveMessageList);
			System.out.println("Recipients of this saved message will be:");
			System.out.println(saveMessageRecipients);
			
			// Add sender's chatname and message to the list
			saveMessageRecipients.insertElementAt(chatName, 0);
			ImageIcon picture = null;
			String origPicDesc = null;
			if(myPicturesList.isSelectionEmpty())
			{
				String messageToBeSaved = "Saved message from" + chatName + ": " + chatToSend +
						" on " + new Date();
				saveMessageRecipients.insertElementAt(messageToBeSaved, 1);
			}
			else
			{
				picture = myPicturesList.getSelectedValue();
				origPicDesc = picture.getDescription();
				picture.setDescription(chatName + " " + origPicDesc + " " + chatToSend);
				saveMessageRecipients.insertElementAt(picture, 1);
			}
			// Send list to server
			try
			{
				Socket s = new Socket(serverAddress, 7777);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(saveMessageRecipients);
			}
			
			catch(Exception e)
			{
				System.out.println("ERROR SENDING: " + e);
				return;
			}
			if(!myPicturesList.isSelectionEmpty())
			{
				picture.setDescription(origPicDesc);
				myPicturesList.clearSelection();
				myPictureWindowLabel.setText("Select a picture");
			}
			inChatArea.setText("");
		}
		
		if (ae.getSource() == sendToAllButton)
		{
			System.out.println("sendToAllButton was pressed.");
			String chatToSend = inChatArea.getText().trim();
			if(chatToSend.length() == 0 && myPicturesList.isSelectionEmpty())
			{
				errorLabelField.setText("NO MESSAGE WAS ENTERED AND NO PICTURE WAS SELECTED.");
				return;
			}
			ImageIcon picture = myPicturesList.getSelectedValue();
			inChatArea.setText("");
			myPicturesList.clearSelection();
			myPictureWindowLabel.setText("Select a picture");
			System.out.println("Sending " + chatToSend + " " + picture);
			// Just send the text chat
			if(picture == null)
			{
				try 
				{
					Socket s = new Socket(serverAddress, 7777);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject('\u007F' + chatName + " " + chatToSend);
					return;
				}
				catch(Exception e)
				{
					System.out.println("ERROR SENDING " + e);
					return;
				}
			}
			
			//Send picture and text chat
			String origPicDesc = picture.getDescription();
			picture.setDescription(chatName + " " + origPicDesc + " " + chatToSend);
			try 
			{
				Socket s = new Socket(serverAddress, 7777);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(picture);
			}
			catch(Exception e)
			{
				System.out.println("ERROR SENDING " + e);
				return;
			}
			//Restore
			picture.setDescription(origPicDesc);
		}
		
		if (ae.getSource() == verticalRButton)
		{
		chatPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		}
		
		if (ae.getSource() == horizontalRButton)
		{
		chatPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		}
		
		if(ae.getSource() == previewPicturesButton)
		{
			System.out.println("previewPictures was pushed!");
			String[] listOfFiles = localDirectory.list();
			Vector<String> pictureFileNames = new Vector<String>();
			for(String s : listOfFiles)
			{
				if(s.endsWith(".jpg") || s.endsWith(".png") || s.endsWith(".gif"))
					pictureFileNames.addElement(s);
			}
			if(pictureFileNames.isEmpty())
			{
				System.out.println("No pictures are found in the local directory");
				return;
			}
			System.out.println("Local directory pictures are " + pictureFileNames);
			Vector<ImageIcon> imageIcons = new Vector<ImageIcon>();
			for (String pictureFileName : pictureFileNames)
		    {
			    ImageIcon picture = new ImageIcon(pictureFileName,pictureFileName);// filename,description   	    	 
			    //myPictures.add(picture);
			    imageIcons.add(picture);
		    }
			myPicturesList.setListData(imageIcons);
			myPictureListWindow.setVisible(true);
		}
		if(ae.getSource() == clearPicSelectButton)
		{
			myPicturesList.clearSelection();
			myPictureWindowLabel.setText("Select a picture");
		}
		
		if(ae.getSource() == ignoreButton)
		{
			List<String> annoyingPeople = whosInList.getSelectedValuesList();
			// Check if list is empty and prompt user
			if(annoyingPeople.isEmpty())
			{
				String warn = "Please select a chatname to ignore";
				System.out.println(warn);
				errorLabelField.setText(warn);
			}
			// Iterate through list and remove self if present
			for(int i = 0; i < annoyingPeople.size(); i++)
			{
				if(annoyingPeople.get(i) == this.chatName)
					annoyingPeople.remove(i);
			}
			// add own chatName at the end
			annoyingPeople.add(chatName);
			//DEBUG
			System.out.println("List of annoying people followed by chatName: ");
			for(String s : annoyingPeople)
				System.out.println(s);
			try
			{
				Socket s = new Socket(serverAddress, 7777);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(annoyingPeople);
			}
			
			catch(Exception e)
			{
				System.out.println("ERROR SENDING ignore list" + e);
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent lse) {
		if(lse.getValueIsAdjusting()) return;
		ImageIcon selectedPicture = myPicturesList.getSelectedValue();
		if(selectedPicture == null) return;
		String pictureDescription = selectedPicture.getDescription();
		myPictureWindowLabel.setText(pictureDescription);
		
	}
}
