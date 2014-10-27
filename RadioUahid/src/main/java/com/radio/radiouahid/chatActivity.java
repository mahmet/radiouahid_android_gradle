package com.radio.radiouahid;


import com.radio.radiouahid.data.information;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.PluginState;
import android.widget.ProgressBar;
import android.widget.TextView;

public class chatActivity extends Activity {

    WebView mWebView;
  
  String URL= information.ChatUrl;

  ProgressBar loadingProgressBar,loadingTitle;
  
  @Override
   public void onCreate(Bundle savedInstanceState) {
   super.onCreate(savedInstanceState);
   setContentView(R.layout.webview);
   TextView txttitle = (TextView) findViewById(R.id.activityTitle);
   txttitle.setText("Chat");

   
   
   mWebView = (WebView) findViewById(R.id.webview);
   mWebView.getSettings().setJavaScriptEnabled(true);
   mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
   mWebView.getSettings().setPluginState(PluginState.ON);
   mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
   mWebView.clearCache(true);
   mWebView.loadUrl(URL);
   mWebView.setWebViewClient(new MyWebViewClient());
   
   loadingProgressBar=(ProgressBar)findViewById(R.id.progressbar_Horizontal); 
   
   mWebView.setWebChromeClient(new WebChromeClient() {

   // this will be called on page loading progress

   @Override

   public void onProgressChanged(WebView view, int newProgress) {

   super.onProgressChanged(view, newProgress);


   loadingProgressBar.setProgress(newProgress);
   //loadingTitle.setProgress(newProgress);
   // hide the progress bar if the loading is complete

   if (newProgress == 100) {
   loadingProgressBar.setVisibility(View.VISIBLE);
   
   } else{
   loadingProgressBar.setVisibility(View.VISIBLE);
   
   }

   }

   });

   }
   
   

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
      if(event.getAction() == KeyEvent.ACTION_DOWN){
          switch(keyCode)
          {
          case KeyEvent.KEYCODE_BACK:
              if(mWebView.canGoBack() == true){
              	mWebView.goBack();
              }else{
                  finish();
              }
              return true;
          }

      }
      return super.onKeyDown(keyCode, event);
  }

   private class MyWebViewClient extends WebViewClient {

 
 @Override
 public boolean shouldOverrideUrlLoading(WebView view, String url) {

 view.loadUrl(url);
 return true;
 }
 }
   public void btnBack(View v) {

	   Intent back = new Intent(getApplicationContext(), MainActivity.class);
		
		startActivity(back);
	}
   
   
}
