package ru.arcein.plugins.itemcps;

import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemCPS extends JavaPlugin {

    List<String> integrations = new ArrayList<>();

    private FileConfiguration config;

    public Map<Player, Long> playerCD = new HashMap<Player, Long>(){
        public boolean removeEldestEntry(Entry<Player, Long> eldest) {
            return (Long)eldest.getValue() + 10000L <= System.currentTimeMillis();
        }
    };


    public void onLoad() {

    }

    public void onEnable() {

        this.saveDefaultConfig();
        config = this.getConfig();

        resolveIntegrations();

        getServer().getPluginManager().registerEvents(new ItemCPSListener(this), this);

        Bukkit.getLogger().info(ChatColor.GREEN + "The plugin has been enabled!");


    }

    private void resolveIntegrations(){

        integrations = new ArrayList<>();

        PluginManager pluginManager = this.getServer().getPluginManager();

        List<String> possibleIntegrations = new ArrayList<>();

        possibleIntegrations.add("MMOItems");

        for(String pluginName : possibleIntegrations){
            if(pluginManager.isPluginEnabled(pluginName)){
                Bukkit.getLogger().info(ChatColor.GREEN + "" + pluginName + " plugin found! " +
                        "Enabling integration...");
                integrations.add(pluginName);
            }
        }
    }

    private long resolveItemCooldown(Player player){
        ItemStack item =  player.getInventory().getItemInMainHand();

        double cps = this.config.getDouble("default-cps");
        long attackCD = Math.round(1000.0D/cps);

        if (integrations.contains("MMOItems")){
            net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tags = nmsItem.getTag();

            if(tags.hasKey("MMOITEMS_ATTACK_SPEED"))
                attackCD = Math.round(1000.0D/tags.getDouble("MMOITEMS_ATTACK_SPEED"));
        }

        return attackCD;
    }

    public void onDisable(){
        Bukkit.getLogger().info(ChatColor.GREEN + "The plugin has been disabled.");
    }

    private class ItemCPSListener implements Listener {
        ItemCPS plugin;

        public ItemCPSListener(ItemCPS plugin){
            this.plugin = plugin;
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )

        public void onPlayerJoin(PlayerJoinEvent event) {
            event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(1000);
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityDamage(EntityDamageByEntityEvent event) {

            if (event.isCancelled()) return;
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                    event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

            Entity attackerEntity = event.getDamager();

            if (!(attackerEntity instanceof Player)) return;

            Player player = (Player) attackerEntity;

            if (plugin.playerCD.containsKey(player)){
                if (plugin.playerCD.get(player) > System.currentTimeMillis()){
                    player.sendMessage("У вас ещё кд на атаку. Подождите ещё "
                            + (plugin.playerCD.get(player) - System.currentTimeMillis())/1000.0 + " с.");
                    event.setCancelled(true);
                    return;
                }
            }

            long cd = resolveItemCooldown(player);

            player.sendMessage("Успешная атака!");

            plugin.playerCD.put(player, System.currentTimeMillis() + cd);

            return;
        }

    }
}
