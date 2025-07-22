package org.rexi.rankupZones.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.rexi.rankupZones.RankupZones;

public class CountingCommand implements CommandExecutor {

    public CountingCommand(RankupZones plugin) {
        this.plugin = plugin;
    }

    private final RankupZones plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args.length > 2) {
            sender.sendMessage("Uso: /rz <tipo> [cantidad]");
            return false;
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "vacas", "vaca" -> sender.sendMessage(ChatColor.AQUA + "Vacas: " + plugin.countVacas);
                case "cerdos", "cerdo" -> sender.sendMessage(ChatColor.AQUA + "Cerdos: " + plugin.countCerdos);
                case "ovejas", "oveja" -> sender.sendMessage(ChatColor.AQUA + "Ovejas: " + plugin.countOvejas);
                case "conejos", "conejo" -> sender.sendMessage(ChatColor.AQUA + "Conejos: " + plugin.countConejos);
                case "gallinas", "gallina" -> sender.sendMessage(ChatColor.AQUA + "Gallinas: " + plugin.countPollos);
                case "abejas", "abeja" -> sender.sendMessage(ChatColor.AQUA + "Abejas: " + plugin.countAbejas);
                case "zombies", "zombie" -> sender.sendMessage(ChatColor.AQUA + "Zombies: " + plugin.countZombies);
                case "esqueletos", "esqueleto" -> sender.sendMessage(ChatColor.AQUA + "Esqueletos: " + plugin.countEsqueletos);
                case "arañas", "araña" -> sender.sendMessage(ChatColor.AQUA + "Arañas: " + plugin.countAranas);
                case "murcielagos", "murcielago" -> sender.sendMessage(ChatColor.AQUA + "Murciélagos: " + plugin.countMurcielagos);
                case "creepers", "creeper" -> sender.sendMessage(ChatColor.AQUA + "Creepers: " + plugin.countCreepers);
                case "blazes", "blaze" -> sender.sendMessage(ChatColor.AQUA + "Blazes: " + plugin.countBlazes);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Tipo inválido. Usa: vacas, cerdos, ovejas, conejos, gallinas, abejas, zombies, esqueletos, arañas, murcielagos, creepers o blazes.");
                    return false;
                }
            }
            return true;
        }

        if (args.length == 2) {
            String type = args[0].toLowerCase();
            int count;

            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Cantidad inválida. Debe ser un número.");
                return false;
            }

            switch (type) {
                case "vacas", "vaca" -> plugin.countVacas = count;
                case "cerdos", "cerdo" -> plugin.countCerdos = count;
                case "ovejas", "oveja" -> plugin.countOvejas = count;
                case "conejos", "conejo" -> plugin.countConejos = count;
                case "gallinas", "gallina" -> plugin.countPollos = count;
                case "abejas", "abeja" -> plugin.countAbejas = count;
                case "zombies", "zombie" -> plugin.countZombies = count;
                case "esqueletos", "esqueleto" -> plugin.countEsqueletos = count;
                case "arañas", "araña" -> plugin.countAranas = count;
                case "murcielagos", "murcielago" -> plugin.countMurcielagos = count;
                case "creepers", "creeper" -> plugin.countCreepers = count;
                case "blazes", "blaze" -> plugin.countBlazes = count;
                default -> {
                    sender.sendMessage(ChatColor.RED + "Tipo inválido. Usa: vacas, cerdos, ovejas, conejos, gallinas, abejas, zombies, esqueletos, arañas, murcielagos, creepers o blazes.");
                    return false;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Contador actualizado: " + type + " ahora tiene " + count);
            return true;
        }
        return true;
    }
}
