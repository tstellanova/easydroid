package com.easybotics.easydroid;

import com.easybotics.bluetooth.BTSessionService;
import com.easybotics.bluetooth.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CommandBotActivity extends Activity {
    // Debugging
    private static final String TAG = "CommandBotActivity";
    private static final boolean D = true;
    
    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;
    
	protected MainApplication sharedApp = null;

    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;
    private Button mSendButton;
    private Spinner mCommandSelector;
    TextView mParamView1;
    TextView mParamView2;
    
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedApp = ((MainApplication)getApplicationContext());

        if(D) Log.w(TAG, "+++ ON CREATE +++");
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.bot_command);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
        mCommandSelector = (Spinner) findViewById(R.id.commandPicker1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.bot_commands_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCommandSelector.setAdapter(adapter);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.w(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!sharedApp.mBTSession.mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the session
        } else {
        	setupSession();
        }
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.w(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        
        if (null == mOutStringBuffer) {
        	setupSession();
        }
        
    	sharedApp.mBTSession.setStreamHandler(this.mStreamHandler);
    }

    @Override
    public synchronized void onPause() {
        if(D) Log.w(TAG, "- ON PAUSE -");

    	super.onPause();
    	sharedApp.mBTSession.setStreamHandler(null);
    }
    
    private void setupSession() {
        if(D) Log.i(TAG, "setupSession()");
        
		mParamView1 = (TextView) findViewById(R.id.commandParam1);
		mParamView2 = (TextView) findViewById(R.id.commandParam2);
        
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.chat_message);
        mConversationView = (ListView) findViewById(R.id.ioView);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
            	String selectedCmd = (String) mCommandSelector.getSelectedItem();
                String param1 = mParamView1.getText().toString();
                String param2 = mParamView2.getText().toString();

                //Substitute input parameters 
                String message = selectedCmd;
                if ((null != param1) && (param1.length() > 0) ) {
                	message = message.replace("(a)",param1);
                    if ((null != param2) && (param2.length() > 0) ) {
                    	message = message.replace("(b)",param2);
                    }
                }

                sendMessage(message);
            }
        });
 

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    
    private void updateListViewScroll() {
        mConversationView.post(new Runnable() {
            public void run() {
                // Select the last row so it will scroll into view...
            	mConversationView.smoothScrollToPosition(mConversationArrayAdapter.getCount() - 1);
                // Just add something to scroll to the top ;-)
            }
        });
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
    	
    	if (sharedApp.mBTSession.mState !=   BTSessionService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            sharedApp.mBTSession.mSessionThread.write(send);

            // Reset out string buffer to zero 
            mOutStringBuffer.setLength(0);
        }
    }
    
	  // The Handler that gets information back from the BluetoothChatService
    private final Handler mStreamHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BTSessionService.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                btDataSent(writeBuf);
                break;
            case BTSessionService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                btDataReceived(readBuf);
                break;
            case BTSessionService.MESSAGE_EOF:
            	//TODO handle somehow?
                break;
            }
        }
    };
    
	public void btDataSent(byte[] data) {
        // construct a string from the buffer
        String writeMessage = new String(data);
        mConversationArrayAdapter.add("> " + writeMessage);
        this.updateListViewScroll();
	}

	public void btDataReceived(byte[] data) {
        // construct a string from the valid bytes in the buffer
        String readMessage = new String(data);
        mConversationArrayAdapter.add("< " + readMessage);
        this.updateListViewScroll();
	}

}
