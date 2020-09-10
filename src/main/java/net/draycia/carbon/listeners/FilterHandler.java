package net.draycia.carbon.listeners;

import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.channels.ChatChannel;
import net.draycia.carbon.events.PreChatFormatEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterHandler implements Listener {

  @NonNull
  private final CarbonChat carbonChat;

  @NonNull
  private final Map<String, List<@NonNull Pattern>> patternReplacements = new HashMap<>();

  @NonNull
  private final List<@NonNull Pattern> blockedWords = new ArrayList<>();

  public FilterHandler(@NonNull final CarbonChat carbonChat) {
    this.carbonChat = carbonChat;
    this.reloadFilters();
  }

  public void reloadFilters() {
    this.patternReplacements.clear();
    this.blockedWords.clear();

    final FileConfiguration config = this.carbonChat.getModConfig();
    final ConfigurationSection filters = config.getConfigurationSection("filters.filters");

    if (filters != null) {
      for (final String replacement : filters.getKeys(false)) {
        final List<Pattern> patterns = new ArrayList<>();

        for (final String word : filters.getStringList(replacement)) {
          if (this.carbonChat.getModConfig().getBoolean("filters.case-sensitive")) {
            patterns.add(Pattern.compile(word));
          } else {
            patterns.add(Pattern.compile(word, Pattern.CASE_INSENSITIVE));
          }
        }

        this.patternReplacements.put(replacement, patterns);
      }
    }

    for (final String replacement : config.getStringList("filters.blocked-words")) {
      if (this.carbonChat.getModConfig().getBoolean("filters.case-sensitive")) {
        this.blockedWords.add(Pattern.compile(replacement));
      } else {
        this.blockedWords.add(Pattern.compile(replacement, Pattern.CASE_INSENSITIVE));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onFilter(final PreChatFormatEvent event) {
    if (!this.carbonChat.getModConfig().getBoolean("filters.enabled")) {
      return;
    }

    if (event.user().online()) {
      if (event.user().player().hasPermission("carbonchat.filter.exempt")) {
        return;
      }
    }

    if (!this.channelUsesFilter(event.channel())) {
      return;
    }

    String message = event.message();

    for (final Map.Entry<String, List<Pattern>> entry : this.patternReplacements.entrySet()) {
      for (final Pattern pattern : entry.getValue()) {
        final Matcher matcher = pattern.matcher(message);

        if (entry.getKey().equals("_")) {
          message = matcher.replaceAll("");
        } else {
          message = matcher.replaceAll(entry.getKey());
        }
      }
    }

    event.message(message);

    for (final Pattern blockedWord : this.blockedWords) {
      if (blockedWord.matcher(event.message()).find()) {
        event.setCancelled(true);
        break;
      }
    }
  }

  private boolean channelUsesFilter(@NonNull final ChatChannel chatChannel) {
    final Object filter = chatChannel.context("filter");

    return filter instanceof Boolean && ((Boolean) filter);
  }

}
