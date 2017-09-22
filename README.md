JustaTV
=======

Overview
--------

This project contains code for a project of mine, where I make my smart TV
dumb by adding buttons to it using Android Things on an RPi 3.

It connects to my LG Smart TV using a USB serial adapter connected to the TV.
Some LG TVs support a serial command protocol that allows the TV to be
controlled.

The Android Things project listens for button presses on three GPIO lines,
and changes the HDMI input depending on the button pressed.  It also sends
a command to the TV that surpressed the On Screen Display (OSD).

You can find more details here:
http://www.jpuderer.net/2017/09/making-my-tv-dumb-again.html

License
-------

Apache Licence Version 2.0
http://www.apache.org/licenses/

