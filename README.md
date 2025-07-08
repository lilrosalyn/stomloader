# stomloader

stomloader is a Fabric mod that allows you to use custom server JARs. It is specifically designed for Minehut, but could feasibly be used for other hosts as well.

# Usage

If you're using ViaProxy (enabled by default), your custom server JAR must bind to port `25565` (can be changed).<br />
Otherwise, it must bind to port `25575`.

1. Change the server type to Fabric
2. Upload stomloader to the `mods/` folder.
3. Upload your custom server JAR to the root directory, and rename it to `airbrush.jar` or configure the `serverJar` option in `stomloader.properties`.
4. You're good to go!

Keep in note that stomloader will automatically download and start ViaProxy alongside the server for version support.<br />
This functionality may be disabled in the newly generated `stomloader.properties` directory.<br />
