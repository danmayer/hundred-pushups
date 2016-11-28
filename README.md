# hundred-pushups

A mobile app based on the plan at http://www.100pushups.com/

## Usage

Install deps:

`brew install node
 brew install watchman`

Install React Native CLI tool:

`npm install -g react-native-cli`

Install `re-natal`:

`npm install -g re-natal`

To open emacs with access to inf-clojure and a REPL, do

`emacs .dir-locals.el` and type `y` at prompt

To get started with your new app, first cd into its directory:
`cd hundred-pushups`


### Running in iOS
Open iOS app in xcode and run it:

`react-native run-ios`

To use figwheel type:

`re-natal use-figwheel
 lein figwheel ios`

Reload the app in simulator

At the REPL prompt type this:
`(in-ns 'hundred-pushups.ios.core)`


### Running in Android

Open Android project in Android Studio

* Upgrade Android Studio
* Android Studio, open project `./android`
* open emulator or connect device
* Run the app
  * `react-native run-android`
  * run app "click play button" (select simulator or device)
  * either way app will fail until figwheel is running, but you need the initial build to start figwheel
* run figwheel
* re-run the app as above this time it should connect to figwheels prompt 

To use figwheel type:

```bash
re-natal use-android-device avd
re-natal use-figwheel
lein figwheel android
```

If you get an error make sure you have a simulator or real device already running.

Reload the app in simulator

At the REPL prompt type this:
`(in-ns 'hundred-pushups.android.core)`

### Interacting with Running App

Changes you make via the REPL or by changing your .cljs files should appear live.

Try this command as an example: 
`(dispatch [:set-greeting "Hi everyone"])`


### Tests

`lein with-profile +tools test-refresh :changes-only`

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
