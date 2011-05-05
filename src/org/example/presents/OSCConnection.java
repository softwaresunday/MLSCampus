package org.example.presents;


import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.util.Enumeration;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

import android.util.Log;

/**
 * TODO Replace illposed OSC library and make one which doesn't
 * allocate memory for each message.
 *  
 * @author peter.reintjes@ncmls.org
 *
 */
public class OSCConnection {
   
	String server = null;
	int port = 57120;
    String myIP = null;
	private OSCPortOut sender = null;

	public OSCConnection() {
	}
	
    public void getOSC(int port) {
    	OSCPortIn receiver  = null;
		try {
			receiver = new OSCPortIn(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
    	OSCListener listener = new OSCListener() {
    		public void acceptMessage(java.util.Date time, OSCMessage message) {
    			Object[] args = message.getArguments();
    				String cmd = message.getAddress();
    			System.out.println("Message received: "+ cmd + ":" + args[0] + " " +args[1]);
			}
    	};
    	receiver.addListener("/activity", listener);
    	receiver.startListening();
    }
	public void connect(String serverAddress) throws ConnectException {
		
        String[] split = serverAddress.split(":");
        if (split != null && split.length == 2) {
        	server = split[0];
        	port = Integer.valueOf(split[1]);
        } else {
        	server = serverAddress;
        	port = 57120;
        }
        try {
        	sender = new OSCPortOut(InetAddress.getByName(server), port);
        } catch (Exception e) {
        	Log.i("osc", e.toString());
        	throw new ConnectException(e.toString());
        }
        if (sender != null)
        {
			Object args[] = new Object[1];
			args[0] = getLocalIpAddress();
			OSCMessage msg = new OSCMessage("/receiver", args);
	    	try {
	    		sender.send((OSCPacket)msg);
	    	} catch (Exception e) {
	    		Log.i("osc", "Could not announce self to server");
	    	}
        }
	}

	public boolean isConnected() {
		return (sender != null);
	}

	public void send(String oscmsg, int square, int activity) {
		
		Object args[] = new Object[2];
		args[0] = new Integer(square);
		args[1] = new Integer(activity);
		OSCMessage msg = new OSCMessage(oscmsg, args);
    	try {
    		sender.send((OSCPacket)msg);
    	} catch (Exception e) {
    		Log.i("osc", "Couldn't send");
    	}
		}
	
	public String getLocalIpAddress() {
		if (myIP == null)
		{
			try {
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
				{
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
					{
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress())
						{
							myIP = inetAddress.getHostAddress().toString();
						}
					}
				}
			} catch (SocketException ex){}	
		}
		return myIP;
		}

	public void disconnect() { if (sender != null) { sender.close(); sender = null;	} }
	protected OnUpdateListener listener;
	
    public void setOnUpdateListener(OnUpdateListener l) {
        listener = l;
    }

    public interface OnUpdateListener {
        void onDisconnect();
     //   void onSocketCommand(SocketCommand socketCommand);
        //void onRead(byte[] data, int length);
    }
    

}
