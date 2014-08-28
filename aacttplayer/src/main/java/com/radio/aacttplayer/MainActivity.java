package com.radio.aacttplayer;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.view.KeyEvent;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings.System;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;






import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.radio.aacttplayer.data.information;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.google.ads.AdRequest;
import com.google.ads.InterstitialAd;

import com.bugsense.trace.BugSenseHandler;



/**
 * This is the main activity.
 */
public class MainActivity extends Activity implements View.OnClickListener, PlayerCallback {

    //Digital Clock
    Calendar c;
    int seconds;
    int min;
    int hour;

    //hidden toplayout
    RelativeLayout topFeedbackLayout;
    ImageView feedbackContentLayout;
    Button emailToRadiouahidButton;
	
	// Notification
	private String artist = "";
	private String track = "";
	private Bitmap albumCover = null;
	private static String lastfm_api_key = information.LastFm;
	private InterstitialAd interstitial;
	NotificationCompat.Builder NotiBld;
	private static final int NOTIFY_ME_ID=12121;
	private NotificationManager MiNotyr=null;
    private static final String LOG = "AACPlayerActivity";
    private History history;
    private SeekBar volControl;
    private Button btnPlay;
    private Button btnStop;
    private TextView txtStatus;
    private TextView txtMetaTitle;
    private TextView txtMetaGenre;
    private boolean wasPlayingBeforePhoneCall = false;
    private AudioManager VolUm;
    private TelephonyManager telephonyManager;
	private ContentObserver mVolumeObserver;
    private ProgressBar progress;
    private Handler uiHandler;
    private String radioTitle = information.RadioName;
    private String StreamUrl = information.StreamURL;
    private String PublisherId = information.PublisherID;
    private MultiPlayer multiPlayer;
    private static final int AAC_BUFFER_CAPACITY_MS = 2500;
	private static final int AAC_DECODER_CAPACITY_MS = 700;
	private static boolean isExitMenuClicked;
	private ImageView logo;
	private UiLifecycleHelper uiHelper;
	TextView dj;
	private TextView clock;
    ////////////////////////////////////////////////////////////////////////////
    // PlayerCallback
    ////////////////////////////////////////////////////////////////////////////

    private boolean playerStarted;

    public void playerStarted() {
        uiHandler.post( new Runnable() {
            public void run() {
                btnPlay.setEnabled( false );
                btnStop.setEnabled( true );
                txtStatus.setText( R.string.text_buffering );
                progress.setProgress( 0 );
                progress.setVisibility( View.VISIBLE );

                playerStarted = true;
            }
        });
    }

    public void playerPCMFeedBuffer( final boolean isPlaying,
                                     final int audioBufferSizeMs, final int audioBufferCapacityMs ) {

        uiHandler.post( new Runnable() {
            public void run() {
                progress.setProgress( audioBufferSizeMs * progress.getMax() / audioBufferCapacityMs );
                if (isPlaying) txtStatus.setText( R.string.text_playing );
            }
        });
    }


    public void playerStopped( final int perf ) {
        uiHandler.post( new Runnable() {
            public void run() {
                btnPlay.setEnabled( true );
                btnStop.setEnabled( false );
                if(NotiBld!=null && MiNotyr!=null) {
					
					NotiBld.setContentText("stopped").setWhen(0);
					MiNotyr.notify(NOTIFY_ME_ID,NotiBld.build());
				}
              
               
                txtStatus.setText( R.string.text_stopped );
                txtStatus.setText( "" + perf + " %" );
                progress.setVisibility( View.INVISIBLE );

                playerStarted = false;
            }
        });
    }


    public void playerException( final Throwable t) {
        uiHandler.post( new Runnable() {
            public void run() {
                new AlertDialog.Builder( MainActivity.this )
                    .setTitle( R.string.text_exception )
                    .setMessage(R.string.text_off )
                    .setNeutralButton( R.string.button_close,
                        new DialogInterface.OnClickListener() {
                            public void onClick( DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }
                     )
                    .show();

                txtStatus.setText( R.string.text_stopped );

                if (playerStarted) playerStopped( 0 );
            }
        });
    }


    public void playerMetadata( final String key, final String value ) {
        TextView tv = null;

        if ("StreamTitle".equals( key ) || "icy-name".equals( key ) || "icy-description".equals( key )) {
            
            
        }
        
        else if ("icy-genre".equals( key )) {
            
            
        }
        else return;

       

        uiHandler.post( new Runnable() {
            public void run() {
            	Runnable runnable = new Runnable() {
            	    public void run () {
            	    	updateAlbum();
            	    }
            	};
            	Handler handler = new Handler();
            	handler.postDelayed(runnable, 3000);
            	
            	
            	
            	artist = getArtistFromAAC(value);
				track = getTrackFromAAC(value);
            	
            	txtMetaTitle.setText(getTrackFromAAC(value));
            	txtMetaGenre.setText(getArtistFromAAC(value));
                if(NotiBld!=null && MiNotyr!=null) {
					
					NotiBld.setContentText("Now playing: "+value).setWhen(0);
					MiNotyr.notify(NOTIFY_ME_ID,NotiBld.build());
				}
            }
        });
    }
    
    
    
    
    
    private String getArtistFromAAC(String streamTitle) {
		int end = streamTitle.indexOf("-");
		if (end <= 0)
			end = streamTitle.indexOf(":");

		String title;
		if (end > 0)
			title = streamTitle.substring(0, end);
		else
			title = streamTitle;
		return title.trim();
	}
    private String getTrackFromAAC(String streamTitle) {
		int start = streamTitle.indexOf("-") + 1;
		if (start <= 0)
			start = streamTitle.indexOf(":") + 1;

		String track;
		if (start > 0)
			track = streamTitle.substring(start);
		else
			track = streamTitle;

		int end = streamTitle.indexOf("(");
		if (end > 0)
			track = streamTitle.substring(start, end);

		end = streamTitle.indexOf("[");
		if (end > 0)
			track = streamTitle.substring(start, end);

		return track.trim();
	}


    public void playerAudioTrackCreated( AudioTrack atrack ) {
    }


    ////////////////////////////////////////////////////////////////////////////
    // OnClickListener
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Called when a view has been clicked.
     */
    public void onClick( View v ) {
        try {
            switch (v.getId()) {
                case R.id.view_main_button_play: start(); break; 
                case R.id.view_main_button_stop: stop(); break;
            }
        }
        catch (Exception e) {
            Log.e( LOG, "exc" , e );
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Log.e("Activity", String.format("Error: %s", error.toString()) + error.getMessage());
                error.printStackTrace();
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                Log.i("Activity", "Success!");
            }
        });
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Protected
    ////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
    	
        super.onCreate( savedInstanceState );
        requestWindowFeature(Window.FEATURE_NO_TITLE);
      
        setContentView( R.layout.activity_main );
        
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        BugSenseHandler.initAndStartSession(this, "a3cc0490");

        topFeedbackLayout = (RelativeLayout) findViewById(R.id.top_layout);
        feedbackContentLayout = (ImageView) findViewById(R.id.feedback_layout);
        emailToRadiouahidButton = (Button) findViewById(R.id.email_to_radiouahid_button);

        generateFedbackViewListeners();
        
        Typeface font2 = Typeface.createFromAsset(getAssets(),
				"ASansBlack.ttf");
		dj = (TextView) findViewById(R.id.djname);
		dj.setText(radioTitle);
		dj.setTypeface(font2);
		
		//instantiate uihelper
		uiHelper = new UiLifecycleHelper(this, null);
		uiHelper.onCreate(savedInstanceState);
		
		clock = (TextView) findViewById(R.id.digitalClock1);
		clock.setTypeface(font2);
		
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (telephonyManager != null) {
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_CALL_STATE);
		}
		
		interstitial = new InterstitialAd(this, PublisherId );
		AdRequest adRequest = new AdRequest();
		interstitial.loadAd(adRequest);
		isExitMenuClicked = false;
		
		logo = (ImageView) findViewById(R.id.logo);
		
        // get the instance of AudioManager class

        
  //volume start  
     // volume control
     		VolUm = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

     		int maxVolume = VolUm
     				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
     		int curVolume = VolUm.getStreamVolume(AudioManager.STREAM_MUSIC);
     		volControl = (SeekBar) findViewById(R.id.volumebar);
     		volControl.setMax(maxVolume);
     		volControl.setProgress(curVolume);
     		volControl
     				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

     					@Override
     					public void onStopTrackingTouch(SeekBar arg0) {
     					}

     					@Override
     					public void onStartTrackingTouch(SeekBar arg0) {
     					}

     					@Override
     					public void onProgressChanged(SeekBar a0, int a1,
     							boolean a2) {
     						VolUm.setStreamVolume(AudioManager.STREAM_MUSIC,
     								a1, 0);
     					}
     				});

     		Handler mHandler = new Handler();
     		// in onCreate put
     		mVolumeObserver = new ContentObserver(mHandler) {
     			@Override
     			public void onChange(boolean selfChange) {
     				super.onChange(selfChange);
     				if (volControl != null && VolUm != null) {
     					int volume = VolUm
     							.getStreamVolume(AudioManager.STREAM_MUSIC);
     					volControl.setProgress(volume);
     				}
     			}

     		};
     		this.getContentResolver()
     				.registerContentObserver(
     						System.getUriFor(System.VOLUME_SETTINGS[AudioManager.STREAM_MUSIC]),
     						false, mVolumeObserver);
//volume
        btnPlay = (Button) findViewById( R.id.view_main_button_play );
        btnStop = (Button) findViewById( R.id.view_main_button_stop );
        txtStatus = (TextView) findViewById( R.id.view_main_text_status );
        txtMetaTitle = (TextView) findViewById( R.id.view_main_text_meta_title );
        txtMetaGenre = (TextView) findViewById( R.id.view_main_text_meta_genre );
        progress = (ProgressBar) findViewById( R.id.view_main_progress );
        btnPlay.setOnClickListener( this );
        btnStop.setOnClickListener( this );

        history = new History( this );
        history.read();

        if (history.size() == 0 ) {
            
        }

       
        uiHandler = new Handler();

        try {
            java.net.URL.setURLStreamHandlerFactory( new java.net.URLStreamHandlerFactory(){
                public java.net.URLStreamHandler createURLStreamHandler( String protocol ) {
                    Log.d( LOG, "Asking for stream handler for protocol: '" + protocol + "'" );
                    if ("icy".equals( protocol )) return new com.spoledge.aacdecoder.IcyURLStreamHandler();
                    return null;
                    
                }
            });
        }
        catch (Throwable t) {
        	
            Log.w( LOG, "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t );
        }
        
        //twitterCallback();
        
        //Instantiate clock
        c = Calendar.getInstance();
        setClock();
        updateClockTimer();
    }

    private void generateFedbackViewListeners() {
        feedbackContentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topFeedbackLayout.setVisibility(View.GONE);
            }
        });

        emailToRadiouahidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "info@radiouahid.fm", null
                ));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "info@radiouahid.fm");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback Radio Uahid Android App");

                startActivity(Intent.createChooser(emailIntent, "Sende uns dein Feedback"));
            }
        });
    }

    private void setClock() {
    	seconds = c.get(Calendar.SECOND);
    	min = c.get(Calendar.MINUTE);
    	hour = c.get(Calendar.HOUR);
    	
    	if (c.get(Calendar.AM_PM) == 1) {
			hour += 12;
		}
    	
    	final DecimalFormat df = new DecimalFormat("00");
    	
    	
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				clock.setText(df.format(hour) + " : " + df.format(min));
			}
		});
    	
    }
    
    private void updateClockTimer() {
    	Timer timer = new Timer();
    	timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				setClock();
			}
		}, 0,1000);
    }
   
    @Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			volControl = (SeekBar) findViewById(R.id.volumebar);
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				int index = volControl.getProgress();
				volControl.setProgress(index + 1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				int index = volControl.getProgress();
				volControl.setProgress(index - 1);
				return true;
			}
			
			return super.onKeyDown(keyCode, event);
		}
    
    
    @Override
    public void onBackPressed(){
    	Log.d("CDA", "OnBackPressed Called");
    	Intent setIntent = new Intent(Intent.ACTION_MAIN);
    	setIntent.addCategory(Intent.CATEGORY_HOME);
    	setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(setIntent);
    }
   
    @Override
	protected void onRestart() {
		super.onRestart();
		//interstitial = new InterstitialAd(this, PublisherId);
		//AdRequest adRequest = new AdRequest();
		//interstitial.loadAd(adRequest);
		
    }
    
    @Override
	protected void onStop() {
    	
		super.onStop();
		
		//interstitial = new InterstitialAd(this, PublisherId);
		//AdRequest adRequest = new AdRequest();
		//interstitial.loadAd(adRequest);
		
	}
    
    @Override
	protected void onStart() {
		super.onStart();
		//interstitial = new InterstitialAd(this, PublisherId);
		//AdRequest adRequest = new AdRequest();
		//interstitial.loadAd(adRequest);
		
    }
    

    @Override
    protected void onPause() {
        super.onPause();
        history.write();
        //interstitial = new InterstitialAd(this, PublisherId);
		//AdRequest adRequest = new AdRequest();
		//interstitial.loadAd(adRequest);
		uiHelper.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
        //interstitial = new InterstitialAd(this, PublisherId);
		//AdRequest adRequest = new AdRequest();
		//interstitial.loadAd(adRequest);
        if (telephonyManager != null) {
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_NONE);
		}
        uiHelper.onDestroy();
    }


    ////////////////////////////////////////////////////////////////////////////
    // Private
    ////////////////////////////////////////////////////////////////////////////

    private void start() {
    	
    	showNotification();
        
    	//interstitial.show();

        // we cannot do it in playerStarted() - it is too late:
        txtMetaTitle.setText("");
        txtMetaGenre.setText("");
       
        multiPlayer = new MultiPlayer( this, AAC_BUFFER_CAPACITY_MS, AAC_DECODER_CAPACITY_MS);
        multiPlayer.playAsync(StreamUrl);
        //----  crear aqui el string para la radio
    }


    private void stop() {
    	albumCover = null;
		ImageView logoimg = (ImageView) findViewById(R.id.logo);
		logoimg.setImageBitmap(albumCover);
    	txtMetaTitle.setText("Tippe Play um zu hören");
        txtMetaGenre.setText("");
    	//interstitial.show();
    	exitNotification();
    	
        if (multiPlayer != null) {
            multiPlayer.stop();
            multiPlayer = null;
        }
    }
    
    public void stopOncall(){
    	 if (multiPlayer != null) {
             multiPlayer.stop();
             multiPlayer = null;
         }
    	
    	
    	
    }
    
    PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				wasPlayingBeforePhoneCall = playerStarted = true;
				stopOncall();
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				if (wasPlayingBeforePhoneCall) {
				
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// A call is dialing,
				// active or on hold
				wasPlayingBeforePhoneCall = playerStarted = true;
				stopOncall();
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	};
	public void btnFacebook(View v) {

		try {
			if (FacebookDialog.canPresentShareDialog(getApplicationContext(), FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
				FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(MainActivity.this)
					.setLink("http://radiouahid.fm")
					.setDescription("Ich höre gerade " + txtMetaGenre.getText().toString() + ": " + txtMetaTitle.getText().toString() + " auf Radio Uahid!")
					.setPicture("http://radiouahid.fm/wp-content/uploads/2013/11/RadioUahid-Logo-ohne-Website-neuer-Slogan-300x250.png")
					.build();
				uiHelper.trackPendingDialogCall(shareDialog.present());
			} else {
				//publish via feed dialog
				publishFeedDialog();
			}
		
		} catch(FacebookException e) {
			e.printStackTrace();
			Log.e("FACEBOOK ERROR", e.getMessage());
		}
	}	
	
	private void publishFeedDialog() {
		Bundle params = new Bundle();
		params.putString("name", "Radio Uahid");
		params.putString("description", "Ich höre gerade: " + txtMetaGenre.getText().toString() + ": " + txtMetaTitle.getText().toString() + " auf Radio Uahid!");
		params.putString("link", "http://radiouahid.fm");
		
		
		WebDialog feedDialog = (
		        new WebDialog.FeedDialogBuilder(MainActivity.this,
		            Session.getActiveSession(),
		            params))
		        .setOnCompleteListener(new OnCompleteListener() {

		            @Override
		            public void onComplete(Bundle values,
		                FacebookException error) {
		                if (error == null) {
		                    // When the story is posted, echo the success
		                    // and the post Id.
		                    final String postId = values.getString("post_id");
		                    if (postId != null) {
		                        Toast.makeText(MainActivity.this,
		                            "Posted story, id: "+postId,
		                            Toast.LENGTH_SHORT).show();
		                    } else {
		                        // User clicked the Cancel button
		                        Toast.makeText(MainActivity.this.getApplicationContext(), 
		                            "Publish cancelled", 
		                            Toast.LENGTH_SHORT).show();
		                    }
		                } else if (error instanceof FacebookOperationCanceledException) {
		                    // User clicked the "x" button
		                    Toast.makeText(MainActivity.this.getApplicationContext(), 
		                        "Publish cancelled", 
		                        Toast.LENGTH_SHORT).show();
		                } else {
		                    // Generic, ex: network error
		                    Toast.makeText(MainActivity.this.getApplicationContext(), 
		                        "Error posting story", 
		                        Toast.LENGTH_SHORT).show();
		                }
		            }
		        })
		        .build();
		    feedDialog.show();
		
	}
	 
	
	public void btnTwitter(View v) {

		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, "Ich höre gerade " + txtMetaGenre.getText().toString() + ": " + txtMetaTitle.getText().toString() + " auf Radio Uahid!" + " http://radiouahid.fm");
		startActivity(Intent.createChooser(shareIntent, "Empfehle uns weiter"));

	}
	


	public void btnChat(View v) {

		topFeedbackLayout.setVisibility(View.VISIBLE);

	}
	
public void showNotification() {
		
		
		NotiBld = new NotificationCompat.Builder(this)
		
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle(radioTitle)
		.setContentText ("")
		.setOngoing(true);
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		NotiBld.setContentIntent(pIntent);
		MiNotyr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		MiNotyr.notify(NOTIFY_ME_ID, NotiBld.build());
		
	}

public void exitNotification() {
	
	clearNotification();
	NotiBld = null;
	MiNotyr = null;
	
}
public void clearNotification() {
	
	if (MiNotyr != null)
		MiNotyr.cancel(NOTIFY_ME_ID);
		
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.main, menu);
	return true;
}

@Override
public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.rating:
		
		Intent intent = new Intent (Intent.ACTION_VIEW,Uri.parse(information.Goplay));
		
		startActivity(intent);
		
		    
		return true;
		
	case R.id.exit:
		

		String message = "Willst du Radio Uahid wirklich beenden?";
		isExitMenuClicked = true;

		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle(radioTitle);
		ad.setMessage(message);
		ad.setCancelable(true);
		ad.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						if (multiPlayer != null) {
							exitNotification();
							multiPlayer.stop();
							
							isExitMenuClicked = true;
							finish();
						}
						else{
							Intent setIntent = new Intent(Intent.ACTION_MAIN);
					    	setIntent.addCategory(Intent.CATEGORY_HOME);
					    	setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					    	startActivity(setIntent);
							
						}
					}
				});

		ad.setNegativeButton("No", null);

		ad.show();

		return true;
		
		
    }
    return super.onOptionsItemSelected(item);
		
	
	}

		
		
		public void updateAlbum() {
			
			
			
			try {
				String musicInfo[] = { artist, track};

				if (!musicInfo[0].equals("") && !musicInfo[1].equals(""))
					new LastFMCoverAsyncTask().execute(musicInfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private class LastFMCoverAsyncTask extends
				AsyncTask<String, Integer, Bitmap> {

			@Override
			protected Bitmap doInBackground(String... params) {
				Bitmap bmp = null;
				try {
					bmp = LastFMCover.getCoverImageFromTrack(lastfm_api_key,
							params[0], params[1]);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return bmp;
			}

			@Override
			protected void onPostExecute(Bitmap bmp) {
				if (bmp != null){
				albumCover = bmp;
				ImageView logoimg = (ImageView) findViewById(R.id.logo);
				logoimg.setImageBitmap(albumCover);}
				
				else {
			    albumCover = null;
				ImageView logoimg = (ImageView) findViewById(R.id.logo);
				logoimg.setImageBitmap(albumCover);}
				
				
				
			}
			
			
		}
		
		public Bitmap getAlbumCover() {
			return albumCover;
		}
		
		
		public void updatelogo() {
			
			Bitmap albumlogo = getAlbumCover();
			ImageView logoimg = (ImageView) findViewById(R.id.logo);
			logoimg.setImageBitmap(albumlogo);
			
		}
		
		
		@Override
		protected void onResume() {
			super.onResume();
			uiHelper.onResume();
		}
		
		@Override
		protected void onNewIntent(Intent intent) {
			super.onNewIntent(intent);

		}
		
		@Override
		protected void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			uiHelper.onSaveInstanceState(outState);
		}
		
		
		

				


}



	
	



