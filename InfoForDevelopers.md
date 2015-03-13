## Required Components ##

  * [The Android SDK](http://developer.android.com/sdk/index.html)
  * [Eclipse 3.5 (Galileo)](http://www.eclipse.org/downloads/)
  * [Android Development Tools (ADT) for Eclipse](http://developer.android.com/sdk/eclipse-adt.html)
  * A Subversion client. If you're on Windows, try [TortoiseSVN](http://tortoisesvn.net/downloads).


## Getting Started ##

  * Install the above components according to their respective instructions.
  * Make sure you install the SDK Platform Android 2.0 package in the Android SDK Manager.
  * Create an Android 2.0 virtual device in the Android SDK Manager.
  * [Check out](http://code.google.com/p/growlforandroid/source/checkout) the source code.
> > The main code is in `trunk/GrowlForAndroid/` so check this folder out into your Eclipse workspace. On Windows, your workspace folder will by default be `%USERPROFILE%\workspace`.
  * Upon checking out the code, start up Eclipse. If the `GrowlForAndroid` project doesn't appear in the Package Explorer, choose **File -> Refresh** to update your workspace.
  * You should now be able to run the application on the virtual device by clicking the Debug button in Eclipse and choosing to run the project as an Android Application.

## Sending GNTP Notifications to the Emulator ##

  * Make sure that you don't have Growl running on your development machine.
  * Open a command prompt, navigate to the Android SDK tools directory and run:
> > `adb forward tcp:23053 tcp:23053`


> to forward TCP connections from the local machine to the emulator.
  * Use GrowlNotify to send a notification by running something like:
> > `growlnotify /a:applicationName /r:notificationType /n:notificationType /t:title text`


> You should see the message:

> _Notification sent successfully_

> on the console, and the notification should appear in the status bar on the Android virtual device.

### Note ###
The Android emulator will only accept connections from the local machine, not from the network. If you wish to test network notifications, you'll need to use a TCP proxy such as  [Simple TCP proxy/datapipe 0.4.6a (stcpipe)](http://aluigi.altervista.org/mytoolz.htm) to listen on a local port and forward the connections to the port used by the emulator.