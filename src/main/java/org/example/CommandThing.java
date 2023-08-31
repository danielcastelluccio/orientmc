package org.example;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class CommandThing implements CommandExecutor {

    @Override
    public void apply(@NotNull CommandSender sender, @NotNull CommandContext context) {
        Player player = (Player) sender;
        Instance instance = player.getInstance();

        int[][] heightMap = new int[18][18];
        for (int i = -9; i < 9; i++) {
            for (int j = -9; j < 9; j++) {
                int height = 0;
                while (instance.getBlock(i, height, j) != Block.AIR) {
                    height++;
                }
                heightMap[i + 9][j + 9] = height;


                if ((height - 40) % 3 == 0) {
                    Main.countourLocations.add(new Vec(i, 0, j));
                }
            }
        }

        Pos position = player.getPosition();
        sender.sendMessage(String.valueOf(heightMap[position.blockX() + 9][position.blockZ() + 9]));
    }

}
