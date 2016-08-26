# hundred-pushups

A Clojure library designed to ... well, that part is up to you.

## Usage

To get started with your new app, first cd into its directory:
cd hundred-pushups

Open iOS app in xcode and run it:
re-natal xcode

To use figwheel type:
re-natal use-figwheel
lein figwheel ios

Reload the app in simulator

At the REPL prompt type this:
(in-ns 'hundred-pushups.ios.core)

Changes you make via the REPL or by changing your .cljs files should appear live.

Try this command as an example:
(swap! app-state assoc :greeting "Hello Clojure in iOS and Android with Rum!")

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
