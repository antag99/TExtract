## TExtract [![Build Status](https://travis-ci.org/Antag99/TExtract.svg)](https://travis-ci.org/Antag99/TExtract)
TExtract is an extractor for [Terraria](https://terraria.org)s content files, which
are compiled to the XNB format. Only the things needed to extract Terrarias files
are implemented, so don't expect it to work for other games without modification.

See [the thread at Terraria forums](https://forums.terraria.org/index.php?threads/textract-extract-terrarias-content-files.937/) for more information.

### Building from source ###
[Gradle](gradle.org) is used for building/testing.
Fire up a shell and run `./gradlew eclipse`.
Eclipse project files will be generated, which can then be imported from eclipse.
[Launch4j] is required to be extracted into `lib/launch4j`,
you can download the version for your operating system [here](https://sourceforge.net/projects/launch4j/files/launch4j-3/3.12/)

To build the project without using an ide you can run `./gradlew build` and `./gradlew jar`.

### Contributing ###
Make sure to include the license headers and to use the eclipse formatter.
Templates to automatically insert the headers are in eclipse-templates.xml.
Ensure that 'Automatically add comments for new methods and types' is enabled.

### License ###
TExtract is available under the MIT License, except for some
Third-Party software available under other licenses:

It incorporates the [PNGJ library](https://github.com/leonbloy/pngj), available under the "Apache License".
It incorporates [Apache Commons IO](https://commons.apache.org/proper/commons-io/), available under the Apache License, version 2.0.
It incorporates parts of [FFmpeg](ffmpeg.org), available under the GNU LGPL version 2.1 or later.
It incorporates [MinLog](https://github.com/EsotericSoftware/minlog), available under the 3-Clause BSD License.
It incorperates the `WinRegistry.java' file from Apache NPanday, available under the Apache License, version 2.0.
It also incorporates some files from MonoGame:
 - `LzxDecoder.cs`, dual-licensed under GNU LGPL version 2.1 and MS-PL (LICENSE.MS-PL).
 - `MSADPCMToPCM.cs`, put into the public domain by its author.
 - `WaveBank.cs`, originally adapted from Mono.XNA, available under the MIT License.

Some parts of `WaveBank.java` were ported from some Perl code of unknown origin.

The FFmpeg binary file included in this repository was compiled from the FFmpeg sources using MinGW
with the following additional options being passed to `configure`:

```
--disable-everything --enable-muxer=wav --enable-encoder=pcm_s16le
--enable-demuxer=xwma --enable-decoder=wmav2
--enable-protocol=file --enable-filter=aresample
```
You may obtain the FFmpeg sources at https://ffmpeg.org/download.html.

The ProGuard distribution is included in this repository. It is avaiable
under the GNU GPL, version 2.0. You may obtain the ProGuard sources at
https://sourceforge.net/projects/proguard/.
