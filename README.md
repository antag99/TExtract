## TExtract ![Build Status](https://drone.io/github.com/Antag99/TExtract/status.png)
TExtract is an extractor for [Terraria](http://terraria.org)s content files, which
are compiled to the XNB format. Only the things needed to extract Terrarias files
are implemented, so don't expect it to work for other games without modification.

See [the thread at Terraria forums](http://forums.terraria.org/index.php?threads/textract-extract-terrarias-content-files.937/) for more information.

### Building from source ###
[Gradle](gradle.org) is used for building/testing.
Fire up a shell and run `./gradlew eclipse`.
Eclipse project files will be generated, which can then be imported from eclipse.

### Contributing ###
Make sure to include the license headers and to use the eclipse formatter.
Templates to automatically insert the headers are in eclipse-templates.xml.
Ensure that 'Automatically add comments for new methods and types' is enabled.

### License ###
TExtract is provided under the MIT license. It was previously licensed under
the BSD license.
