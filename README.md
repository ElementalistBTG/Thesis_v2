# Thesis (Indoor positioning using cloud anchors, wifi and cellular signals)
(this is the final program for my thesis and introduces indoor mapping using Network Signals and Augmented Reality technologies)

This system consists of a front end (Android app) and a backend (Intelij java servlets).
To recreate this setup you must port forward the ports 9090->8080. Then you can run the JavaWebApp_war.war on the xampp server (tomcat). The mysql database must have the tables "signalsmapped" and "signalsmeasured" and these tables must have the form shown in the image "mysql".

Also, in myApplication helper method in Android, you must change the IP address to the one of your pc network (your home for example).

Last step is to copy your personal google-services.json file to the app folder.This is due to the fact that the app is currently connected to the author's firebase, you must change the firebase account so as to be able to write/delete cloud anchrors ids.
