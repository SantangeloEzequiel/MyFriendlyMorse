# MyFriendlyMorse
MyFriendlyMorse is an app that translates text into Morse code and plays it back via Flash and audio. It also allows you to receive external Morse code via audio and soon via light.

Dependencies

This project uses the following local .aar libraries:

Library	Repository / Download
MorseCoreLib	https://github.com/SantangeloEzequiel/MorseCoreLib.git

MorseCoreLib implements Morse signal detection using the Goertzel algorithm to analyze energy at multiple target frequencies.
The algorithm measures the signal energy across these frequencies, maps the results, and determines which frequency is being used to transmit the Morse message.

After this, the library enters a learning phase, where it waits to detect two distinct symbols whose energy levels fall below an arbitrary threshold. Once this condition is met, the system automatically adapts and begins real-time Morse code translation.