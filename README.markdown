See the project homepage at http://www.rcode.net/assetserver/ .

This project is being used internally by the author and is evolving rapidly.  In order to build off of the repo, make sure you have Java 1.6+ and Ant 1.8+.  Then the following should get you going:

    git clone git://github.com/stellaeof/assetserver.git assetserver
    cd assetserver
    ant dist
   
This will build a zip and tarballs.  The full cross-platform distribution will be under build/dist/assetserver-dev.  Binaries (assetserver and assetserver.exe) are in build/dist/bin/assetserver-dev/bin.

The quick start on the website is more complete, but to see something in one step from here, run:
   
    build/dist/bin/assetserver-dev/bin/assetserver serve addons/htmlpack/testsite

and point your browser at http://localhost:4080/test.html

Integrations
------------

   * node.js: https://gist.github.com/828185

Enjoy!
