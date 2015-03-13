# Introduction #
In this mode of operation, Growl for Windows will forward notifications from the computer to Growl for Android whenever GfW can contact the Android device.


# How To #
On your Android device:
  1. Install Growl for Android from the Android Market.
  1. In the main window, press the toggle button to start the Growl listener service.
  1. Press the menu button, then choose Preferences.
  1. Scroll down and select Password Manager. The password list will be empty, as we haven't added any passwords yet.
  1. Press the menu button, the choose Add Password.
  1. Enter any name and value you like for the password.
  1. Press the Add button. The name of your password will appear in the list.
  1. Press the back button twice to return to the main window.

On your computer:
  1. Install [Growl for Windows](http://growlforwindows.com).
  1. Open the Growl options dialog, then switch to the Network tab.
  1. Press the Add button below the Forwarding list.
  1. Enter a name of your choosing for the forward.
  1. Enter the domain name or IP address of your Android device.
  1. Leave the Port as 23053 and the Protocol as GNTP.
  1. Press the OK button.
  1. Notifications from Growl should now appear on the main window of Growl for Android.

**Important:** If you already have applications registered with Growl for Windows, you will need to re-register them for them to show up in Growl for Android. Most applications re-register their notifications at startup, so re-starting the application on your computer should make it show up.

To see a list of all registered applications in Growl for Android, press the menu button from the main window and choose Applications.