/*
 * Copyright 2016 John Grosh (jagrosh).
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
package com.jagrosh.jmusicbot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.*;
import com.jagrosh.jmusicbot.commands.admin.*;
import com.jagrosh.jmusicbot.commands.dj.*;
import com.jagrosh.jmusicbot.commands.general.*;
import com.jagrosh.jmusicbot.commands.music.*;
import com.jagrosh.jmusicbot.commands.owner.*;
import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.gui.GUI;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import java.awt.Color;
import java.util.Arrays;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class JMusicBot {
    public final static String PLAY_EMOJI = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI  = "\u23F9"; // ⏹
    public final static Permission[] RECOMMENDED_PERMS = {Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
                                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
                                Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE};
    public final static GatewayIntent[] INTENTS = {GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES};
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // startup log
        Logger log = LoggerFactory.getLogger("Startup");

        // create prompt to handle startup
        Prompt prompt = new Prompt("JMusicBot",
                "Switching to nogui mode. You can manually start in nogui mode by including the -Dnogui=true flag.",
                "true".equalsIgnoreCase(System.getProperty("nogui", "false")));
        
        // get and check latest version
        String version = OtherUtil.checkVersion(prompt);
        
        // check for valid java version
        if(!System.getProperty("java.vm.name").contains("64"))
            prompt.alert(Prompt.Level.WARNING, "Java バージョン", "サポートされていないバージョンの Java を利用しているようです。64bit 版のものを利用してください。");
        
        // load config
        BotConfig config = new BotConfig(prompt);
        config.load();
        if (!config.isValid())
            return;

        // set up the listener
        EventWaiter waiter = new EventWaiter();
        SettingsManager settings = new SettingsManager();
        Bot bot = new Bot(waiter, config, settings);

        AboutCommand aboutCommand = new AboutCommand(Color.BLUE.brighter(),
                "[簡単にホストできる Discord Bot](https://github.com/jagrosh/MusicBot) です。 (バージョン " + version + ")",
                new String[] { "簡単に自分でホストできます。" }, RECOMMENDED_PERMS);
        aboutCommand.setIsAuthor(false);
        aboutCommand.setReplacementCharacter("\uD83C\uDFB6"); // 🎶

        // set up the command client
        CommandClientBuilder cb = new CommandClientBuilder().setPrefix(config.getPrefix())
                .setAlternativePrefix(config.getAltPrefix()).setOwnerId(Long.toString(config.getOwnerId()))
                .setEmojis(config.getSuccess(), config.getWarning(), config.getError()).setHelpWord(config.getHelp())
                .setLinkedCacheSize(200).setGuildSettingsManager(settings).addCommands(aboutCommand, new PingCommand(),
                        new SettingsCmd(bot),

                        new LyricsCmd(bot), new NowplayingCmd(bot), new PlayCmd(bot), new PlaylistsCmd(bot),
                        new QueueCmd(bot), new RemoveCmd(bot), new SearchCmd(bot), new SCSearchCmd(bot),
                        new ShuffleCmd(bot), new SkipCmd(bot),

                        new ForceRemoveCmd(bot), new ForceskipCmd(bot), new MoveTrackCmd(bot), new PauseCmd(bot),
                        new PlaynextCmd(bot), new RepeatCmd(bot), new SkiptoCmd(bot), new StopCmd(bot),
                        new VolumeCmd(bot),

                        new PrefixCmd(bot), new SetdjCmd(bot), new SettcCmd(bot), new SetvcCmd(bot),

                        new AutoplaylistCmd(bot), new DebugCmd(bot), new PlaylistCmd(bot), new SetavatarCmd(bot),
                        new SetgameCmd(bot), new SetnameCmd(bot), new SetstatusCmd(bot), new ShutdownCmd(bot));
        if (config.useEval())
            cb.addCommand(new EvalCmd(bot));
        boolean nogame = false;
        if (config.getStatus() != OnlineStatus.UNKNOWN)
            cb.setStatus(config.getStatus());
        if (config.getGame() == null)
            cb.useDefaultGame();
        else if(config.getGame().getName().equalsIgnoreCase("none"))
        {
            cb.setActivity(null);
            nogame = true;
        }
        else
            cb.setActivity(config.getGame());
        
        if(!prompt.isNoGUI())
        {
            try 
            {
                GUI gui = new GUI(bot);
                bot.setGUI(gui);
                gui.init();
            } catch (Exception e) {
                log.error("GUI を起動できませんでした。" + "もしあなたが GUI を表示できるディスプレイをサーバー上で使っていない場合、"
                        + "-Dnogui=true フラグを使用することによって、CLI にて起動できます。");
            }
        }

        log.info("設定ファイルを読み込みました： " + config.getConfigLocation());

        // attempt to log in and start
        try
        {
            JDA jda = JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE)
                    .setActivity(nogame ? null : Activity.playing("起動中..."))
                    .setStatus(config.getStatus()==OnlineStatus.INVISIBLE || config.getStatus()==OnlineStatus.OFFLINE 
                            ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB)
                    .addEventListeners(cb.build(), waiter, new Listener(bot))
                    .setBulkDeleteSplittingEnabled(true)
                    .build();
            bot.setJDA(jda);
        } catch (LoginException ex) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot",
                    ex + "\正しい config.txt を編集していることを確認し、正しいトークン（「シークレット」ではありません！）"
                            + "を使用していることを確認してください。"
                            + "設定ファイルの場所: " + config.getConfigLocation());
            System.exit(1);
        } catch (IllegalArgumentException ex) {

            prompt.alert(Prompt.Level.ERROR, "JMusicBot",
                    "設定のいくつかが間違っています。 " + "invalid: " + ex + "\n設定ファイルの場所: " + config.getConfigLocation());
            System.exit(1);
        }
    }
}
