package org.rexi.rankupZones.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rexi.rankupZones.RankupZones;

import java.util.List;

public class PlayerListener implements Listener {

    RankupZones plugin = RankupZones.getInstance();

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (world.getName().equalsIgnoreCase("mobs")) {
            long entities = world.getLivingEntities().stream()
                    .filter(entity -> !(entity instanceof Player))
                    .count();
            if (entities > 250) {
                plugin.removeWorld();
                System.out.println("[RankupZones] Demasiadas entidades en el mundo 'mobs'. Se han eliminado todas las entidades: "+ entities);
            }
        }
    }

    // Para las minas
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        List<String> rawNames = plugin.getConfig().getStringList("blocked-names");
        List<String> blockedNames = rawNames.stream()
                .map(name -> ChatColor.translateAlternateColorCodes('&', name))
                .toList();

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName(); // incluye colores

                // Compara exactamente (con color incluido)
                if (blockedNames.contains(displayName)) {
                    event.getInventory().setResult(null);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getItem(0); // ítem original
        ItemStack result = event.getResult(); // ítem resultante con el nuevo nombre

        if (firstItem == null || result == null) return;

        ItemMeta originalMeta = firstItem.getItemMeta();
        ItemMeta resultMeta = result.getItemMeta();

        if (originalMeta == null || resultMeta == null) return;
        if (!originalMeta.hasDisplayName() || !resultMeta.hasDisplayName()) return;

        // Cargar los nombres bloqueados (con colores)
        List<String> rawNames = plugin.getConfig().getStringList("blocked-names");
        List<String> blockedNames = rawNames.stream()
                .map(name -> ChatColor.translateAlternateColorCodes('&', name))
                .toList();

        String originalName = originalMeta.getDisplayName();
        String newName = resultMeta.getDisplayName();

        // Si el ítem original tiene un nombre bloqueado y el jugador intenta renombrarlo, bloqueamos el resultado
        if (blockedNames.contains(originalName) && !originalName.equals(newName)) {
            event.setResult(null); // bloquea el resultado del yunque
        }
    }
}
