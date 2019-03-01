/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.utils;

import com.jagrosh.jmusicbot.JMusicBot;
import com.jagrosh.jmusicbot.entities.Prompt;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class OtherUtil
{
    public final static String NEW_VERSION_AVAILABLE = "There is a new version of JMusicBot available!\n"
                    + "現在のバージョン: %s\n"
                    + "新しいバージョン: %s\n\n"
                    + "https://github.com/jagrosh/MusicBot/releases/latest から最新ビルドをダウンロードしてください。";
    
    public static InputStream imageFromUrl(String url)
    {
        if(url==null)
            return null;
        try 
        {
            URL u = new URL(url);
            URLConnection urlConnection = u.openConnection();
            urlConnection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36");
            return urlConnection.getInputStream();
        }
        catch(IOException | IllegalArgumentException ignore) {}
        return null;
    }
    
    public static Game parseGame(String game)
    {
        if(game==null || game.trim().isEmpty() || game.trim().equalsIgnoreCase("default"))
            return null;
        String lower = game.toLowerCase();
        if(lower.startsWith("playing"))
            return Game.playing(game.substring(7).trim());
        if(lower.startsWith("listening to"))
            return Game.listening(game.substring(12).trim());
        if(lower.startsWith("listening"))
            return Game.listening(game.substring(9).trim());
        if(lower.startsWith("watching"))
            return Game.watching(game.substring(8).trim());
        if(lower.startsWith("streaming"))
        {
            String[] parts = game.substring(9).trim().split("\\s+", 2);
            if(parts.length == 2)
            {
                return Game.streaming(parts[1], "https://twitch.tv/"+parts[0]);
            }
        }
        return Game.playing(game);
    }
    
    public static OnlineStatus parseStatus(String status)
    {
        if(status==null || status.trim().isEmpty())
            return OnlineStatus.ONLINE;
        OnlineStatus st = OnlineStatus.fromKey(status);
        return st == null ? OnlineStatus.ONLINE : st;
    }
    
    public static String checkVersion(Prompt prompt)
    {
        // Get current version number
        String version = getCurrentVersion();
        
        // Check for new version
        String latestVersion = getLatestVersion();
        
        if(latestVersion!=null && !latestVersion.equals(version))
        {
            prompt.alert(Prompt.Level.WARNING, "Version", String.format(NEW_VERSION_AVAILABLE, version, latestVersion));
        }
        
        // Return the current version
        return version;
    }
    
    public static String getCurrentVersion()
    {
        if(JMusicBot.class.getPackage()!=null && JMusicBot.class.getPackage().getImplementationVersion()!=null)
            return JMusicBot.class.getPackage().getImplementationVersion();
        else
            return "UNKNOWN";
    }
    
    public static String getLatestVersion()
    {
        try
        {
            Response response = new OkHttpClient.Builder().build()
                    .newCall(new Request.Builder().get().url("https://api.github.com/repos/jagrosh/MusicBot/releases/latest").build())
                    .execute();
            ResponseBody body = response.body();
            if(body != null)
            {
                try(Reader reader = body.charStream())
                {
                    JSONObject obj = new JSONObject(new JSONTokener(reader));
                    return obj.getString("tag_name");
                }
                finally
                {
                    response.close();
                }
            }
            else
                return null;
        }
        catch(IOException | JSONException | NullPointerException ex)
        {
            return null;
        }
    }
}
