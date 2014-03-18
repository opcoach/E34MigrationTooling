E34MigrationTooling
===================

Some tooling to help to migrate from Eclipse 3 to Eclipse 4

This repository will contain some ideas of tools that could help to migrate from Elipse 3 to Eclipse 4. 

The first tool is a statistic window to get information of your org.eclipse.ui dependencies. 

Install the com.opcoach.e34.tools plugin in your workspace or target platform, and launch a new IDE with it. In the runtime workspace, load your E3 plugins and then open the view 'Migration Stats'.

This view displays all the E3 extension points and a column for each plugin selected in your workpsace. You will get the number of views, perspectives, commands, ... that are created by the selected plugin. You can select several plugins to get this information. 

Deprecated elements and extension points are displayed with a red color. If you still used them, you will probably have problems for your migration. 

The upper part (not finished), will display statistics for the global selection : for instance, total number of views, preference pages, ... to be migrated, and probably compute a kind of ratio to know if your application is ready for migration ! FOr instance : 2/10 would mean, hhmmm your application is to much sticked on E3 and can not be easily migrated. 8/10 would mean, ok, you have some update to do to start your migration. 10/10 would mean, ok, no more deprecated stuff, you can start...  

Feel free to contribute to this plugin using pull request, so as to find a good method for migration. 

You can of course send me an email to talk about it : olivier.prouvost@opcoach.com

