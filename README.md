# Auto Reconnect 1.0.1

Client mod for Minecraft Java **1.21.11** and **Fabric Loader 0.18.4 or later**.
Put the release JAR in your Minecraft `mods` folder. Nothing is required on the server,
and Auto Reconnect itself does not require Fabric API.

The multiplayer disconnect screen shows two buttons directly under Minecraft's
**Back to Server List** button:

- **Reconnect** immediately reconnects to the same server.
- **Auto Reconnect** shows the countdown and toggles automatic reconnect.
  Switch it off to cancel the current countdown, or use **Back to Server List** to leave.

With optional **Mod Menu 17**, open **Mods → Auto Reconnect → Configure**.
There you can toggle the default automatic reconnect behavior and set the first retry
delay. Click **Done** to save. The default is **5 seconds**; allowed values are
**3–2147483647 seconds**. Without Mod Menu, the two disconnect screen buttons
still work. To change the delay without Mod Menu, exit Minecraft and edit or create
`config/autoreconnect.properties`:

```properties
autoReconnect=true
delaySeconds=5
```

Settings from 1.0.0 carry over. The server address is only kept in memory.

An ordinary kick can trigger one automatic try. Network failures can use up to three
tries with delays of 1×, 2× and 4× the configured value, each capped at 2147483647 seconds.
A stable 30-second rejoin resets that budget. The mod will not automatically retry
an initial failed connection, a deliberate Disconnect, or recognized bans, login
denials, allowlist denials and protocol errors. It skips Realms and server transfers.

Build with Java 21:

```sh
./gradlew build
```

Run the retry policy tests with Java 21:

```sh
javac -d build/policy-test src/client/java/com/autoreconnect/RetryPolicy.java tests/RetryPolicyTest.java
java -cp build/policy-test RetryPolicyTest
```
