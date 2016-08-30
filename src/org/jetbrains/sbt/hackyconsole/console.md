# Sbt Console Planned Features

## Essentials

* interactive console, send commands to sbt, read output
* It needs to be at least as usable as sbt console in a terminal window
    * same autocompletion as in sbt console, but probably triggered by ctrl+space.
    * history
 
## Must-haves

* jump to code errors linked in sbt 
* replace make with sbt compile in sbt projects
* run tests via sbt

## Rather important things

* run/debug via sbt process
* sbt continuous compilation switch
* highlight files with compile errors in project view
* highlight compile errors in editor even if internal compiler doesn't get it
* run tasks in selected submodule
* sbt compilation output goes to "Messages" window

## Neat things

* Refresh project definition "online" without full dependency resolution and stuff 
* common sbt tasks from SBT side tab menu
* jump to definition on `inspect`
* provide sbt inspections in the editor

