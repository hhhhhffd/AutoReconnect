# Auto Reconnect 1.0.0

Client mod for Minecraft Java **1.21.11** with **Fabric Loader 0.18.4+**.
Fabric API is not required. Put the release JAR in the client's `mods` folder.
You do not need to install it on the server.

After a disconnect from a multiplayer server:

- **Reconnect** immediately reconnects to the same server using Minecraft's normal connection flow.
- **Auto** toggles automatic reconnect. Switch it off to cancel the countdown, or click the
  vanilla **Back** button to leave without reconnecting.
- Enter the first retry delay in seconds and click **Set delay (s)** or press Enter.
  It starts at **5** and accepts **3–2147483647** seconds. Changes are saved to
  `config/autoreconnect.properties`. Only these settings are stored; no address is saved.

Ordinary kicks can trigger one automatic attempt. After a network failure the retry budget
is three attempts total, with delays of 1×, 2× and 4× the configured value, each capped at
2147483647 seconds. A stable 30-second rejoin resets the retry budget. The automatic
feature does not retry an initial connection that never succeeded, a deliberate
Disconnect, or recognized bans, allowlist/authentication denials and protocol errors.
Realms and automatic server transfers are not handled by the mod.

Build using Java 21:

```
./gradlew build
```

Policy tests, also with Java 21:

```
javac -d build/policy-test src/client/java/com/autoreconnect/RetryPolicy.java tests/RetryPolicyTest.java
java -cp build/policy-test RetryPolicyTest
```
