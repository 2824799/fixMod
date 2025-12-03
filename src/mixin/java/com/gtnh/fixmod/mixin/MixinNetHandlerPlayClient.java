package com.gtnh.fixmod.mixin;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    /**
     * 缓存跨服期间收到的 team 名称，等世界就绪后再创建。
     * 这样可以应对 Bungee 发包乱序或世界未初始化导致的丢包问题。
     */
    private static final Set<String> pendingTeams = new HashSet<String>();
    private static boolean lastWorldNull = false;

    @Inject(method = "handleTeams", at = @At("HEAD"), cancellable = true)
    private void scoreboardfix$handleTeamsResilient(S3EPacketTeams packet, CallbackInfo ci) {

        // 取包内信息（name + action）
        String teamName = packet.func_149312_c();
        int action = packet.func_149307_h(); // 0-create,1-remove,2-update,3-add,4-removePlayers

        // 如果世界尚未初始化：缓存 team name，然后丢弃这个包（否则会 NPE）
        if (Minecraft.getMinecraft().theWorld == null) {
            // 缓存 name（无论 action，为了保证后续能创建）
            if (teamName != null && !teamName.isEmpty()) {
                if (pendingTeams.add(teamName)) {
                    System.out.println(
                        "[FixMod] [Scoreboard] Cached team during world-null: '" + teamName
                            + "' (action="
                            + action
                            + ")");
                }
            } else {
                System.out.println(
                    "[FixMod] [Scoreboard] Received team packet with empty name while world==null (action=" + action
                        + ")");
            }

            // 取消原包执行以避免崩溃（world == null 时处理会 NPE）
            ci.cancel();
            lastWorldNull = true;
            return;
        }

        // 如果刚从 world==null 切换到有 world 的状态：先 flush 缓存
        if (lastWorldNull) {
            lastWorldNull = false;
            if (!pendingTeams.isEmpty()) {
                try {
                    Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
                    for (String pendingName : pendingTeams) {
                        if (pendingName == null) continue;
                        ScorePlayerTeam existing = scoreboard.getTeam(pendingName);
                        if (existing == null) {
                            // 创建缺失队伍
                            try {
                                scoreboard.createTeam(pendingName);
                                System.out.println(
                                    "[FixMod] [Scoreboard] Flushed & created pending team: '" + pendingName + "'");
                            } catch (Exception e) {
                                // createTeam 可能抛出（重名等），记录但继续
                                System.out.println(
                                    "[FixMod] [Scoreboard] Failed to create pending team '" + pendingName
                                        + "': "
                                        + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out
                        .println("[FixMod] [Scoreboard] Exception while flushing pending teams: " + e.getMessage());
                } finally {
                    pendingTeams.clear();
                }
            }
        }

        // 世界已就绪：继续按之前的精细化逻辑
        Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        ScorePlayerTeam team = scoreboard.getTeam(teamName);

        switch (action) {
            case 0: // create -> 允许原版处理（原版会创建）
            case 1: // remove -> 允许原版处理
                return;

            case 2: // update info
            case 3: // add players
            case 4: // remove players
                if (team == null) {
                    // 如果还是不存在，则创建（防止乱序）
                    try {
                        scoreboard.createTeam(teamName);
                        System.out.println(
                            "[FixMod] [Scoreboard] Created missing team on-the-fly: '" + teamName
                                + "' (action="
                                + action
                                + ")");
                    } catch (Exception e) {
                        System.out.println(
                            "[FixMod] [Scoreboard] Failed to create team '" + teamName
                                + "' on-the-fly: "
                                + e.getMessage());
                        // 如果创建失败，为安全起见仍然取消此次包，避免 NPE
                        ci.cancel();
                    }
                }
                return;

            default:
                return;
        }
    }
}
