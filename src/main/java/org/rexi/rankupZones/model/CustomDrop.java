package org.rexi.rankupZones.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CustomDrop {
    private final Material material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final boolean enchanted;

    public CustomDrop(Material material, int amount, String name, List<String> lore, boolean enchanted) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.enchanted = enchanted;
    }

    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name.replace("&", "§"));
            meta.setLore(lore.stream().map(line -> line.replace("&", "§")).toList());
            if (enchanted) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}

