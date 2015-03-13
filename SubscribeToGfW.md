# Introduction #
In this mode of operation, Growl for Android will register itself with an instance of Growl for Windows every minute or so, whenever that instance of GfW is reachable. GfW will then automatically forward any notifications to Growl for Android.

# How To #
On your computer:
  1. Install [Growl for Windows](http://growlforwindows.com)
  1. Open the Growl options dialog, go to the Security tab.
  1. In the Password Manager section add a new password to be used by your Android device. It can have any name and any value.
  1. Switch to the Network tab and leave it open while you configure your device.

On your Android device:
  1. Install Growl for Android from the Android Market.
  1. In the main window, press the toggle button to start the Growl listener service.
  1. Press the menu button, then choose Preferences.
  1. Choose Subscriptions from the Preference window. The Subscriptions window will be empty, because we haven't set up any subscriptions yet.
  1. Press the menu button, then choose Add Subscription.
  1. Enter any name you like for the Name of the subscription.
  1. For the Address field, enter the domain name or IP address of the computer running Growl for Windows.
  1. For the Password, enter the password you set up on earlier on the computer.
  1. Press the Add button.
  1. The list will update to show the status of the connection. If it doesn't show Subscribed after a second or two, check to make sure that the details you entered are correct and you have an active WiFi connection.
  1. Your Android device should now be listed in the Forwarding list of Growl for Windows.

**Important:** If you already have applications registered with Growl for Windows, you will need to re-register them for them to show up in Growl for Android. Most applications re-register their notifications at startup, so re-starting the application on your computer should make it show up.

To see a list of all registered applications in Growl for Android, press the menu button from the main window and choose Applications.