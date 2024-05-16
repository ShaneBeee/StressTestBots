package com.shanebeestudios.stress.api.util;

import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * General utility methods
 */
public class Utils {

    /**
     * Get a UUID from a player name
     *
     * @param playerName Player name to fetch UUID from
     * @return UUID from player name if available
     */
    @Nullable
    public static UUID nameToUUID(String playerName) {
        try {
            URL url = new URI("https://api.mojang.com/users/profiles/minecraft/" + playerName).toURL();
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(new InputStreamReader(url.openStream()));
            String uuidString = (String) json.get("id");
            if (uuidString != null) {
                return UUID.fromString(uuidString.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            }
        } catch (ParseException | IOException | URISyntaxException ignore) {
        }
        return null;
    }

    /**
     * Create an InetAddress from host and port
     *
     * @param address Host address to connect to
     * @param port    Port to connect to
     * @return InetAddress
     */
    public static InetSocketAddress createInetAddress(String address, int port) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            return new InetSocketAddress(inetAddress.getHostAddress(), port);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
