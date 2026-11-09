# Release builds

- Release builds run R8 (shrinking, obfuscation, resource shrinking), which Google Play requires by Feb 2027. It doesn't run on debug builds, so reflection and name lookups only break in a release build; test those there. The bundle carries `mapping.txt`, so Play can translate stack traces.
