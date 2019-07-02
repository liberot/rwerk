#!bin/sh
'''
/Users/vico/Workspace/adt-bundle-mac-x86_64-20131030/sdk/tools/android create project --target 1 --name RRRadioApp --path ./ --activity RRRadioActivity --package de.appwerk.radioapp
/Users/vico/Workspace/adt-bundle-mac-x86_64-20131030/sdk/tools/android create lib-project --target 1 --path ./libs/appwerk --package de.appwerk.radioapp.lib
/Users/vico/Workspace/adt-bundle-mac-x86_64-20131030/sdk/tools/android update project --target 1 --path ./ --library ./libs/appwerk 
'''
