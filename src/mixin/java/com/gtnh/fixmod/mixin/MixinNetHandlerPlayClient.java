package com.gtnh.fixmod.mixin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final Set<String> pendingTeams = new HashSet<String>();
    private static boolean lastWorldNull = false;
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    @Inject(method = "handleTeams", at = @At("HEAD"), cancellable = true)
    private void scoreboardfix$handleTeamsResilient(S3EPacketTeams packet, CallbackInfo ci) {

        if (packet == null) return;

        String teamName = packet.func_149312_c();
        int action = packet.func_149307_h(); // 0=Create, 1=Remove, 2=Update, 3=Add, 4=RemovePlayer

        // --- 1. 世界未初始化保护 ---
        if (Minecraft.getMinecraft().theWorld == null) {
            if (teamName != null && !teamName.isEmpty()) {
                pendingTeams.add(teamName);
            }
            ci.cancel();
            lastWorldNull = true;
            return;
        }

        // --- 2. 世界加载后的缓存清理 ---
        if (lastWorldNull) {
            lastWorldNull = false;
            if (!pendingTeams.isEmpty()) {
                try {
                    Scoreboard sb = Minecraft.getMinecraft().theWorld.getScoreboard();
                    Iterator<String> it = pendingTeams.iterator();
                    while (it.hasNext()) {
                        String pendingName = it.next();
                        if (sb.getTeam(pendingName) == null) {
                            try {
                                sb.createTeam(pendingName);
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    pendingTeams.clear();
                }
            }
        }

        Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        ScorePlayerTeam team = scoreboard.getTeam(teamName);

        // --- 3. 接管 Action 0 (Create) : 修复崩溃 & 修复第一个人不显示 ---
        if (action == 0) {
            try {
                // 如果队伍不存在，创建它；如果已存在，直接使用现有对象 (避免 "Already exists" 崩溃)
                if (team == null) {
                    team = scoreboard.createTeam(teamName);
                }

                // [修正点] 根据你提供的源码，使用 setNamePrefix 和 setNameSuffix
                // 包里的 func_149311_e 是前缀 (Prefix)
                team.setNamePrefix(packet.func_149311_e());

                // 包里的 func_149309_f 是后缀 (Suffix)
                team.setNameSuffix(packet.func_149309_f());

                // 设置友伤 (Bitmask), 你的源码里这个叫 func_98298_a
                team.func_98298_a(packet.func_149308_i());

                // 执行 Bungee 修正逻辑 (关联影子玩家 + 关联真实玩家)
                fixBungeeTabList(scoreboard, team, teamName, packet.func_149311_e());

                // 原版逻辑：把包里原本携带的玩家列表也加上
                Collection<String> players = packet.func_149310_g();
                if (players != null && !players.isEmpty()) {
                    for (String player : players) {
                        scoreboard.func_151392_a(player, teamName);
                    }
                }
            } catch (Exception e) {
                System.out.println("[FixMod] [ScoreboardFix] Error handling Action 0: " + e.getMessage());
            }

            // 彻底取消原版处理
            ci.cancel();
            return;
        }

        // --- 4. 增强 Action 2 (Update) : 修复其余人不显示 ---
        if (action == 2) {
            // 防崩：如果 Update 时队伍不存在，先创建
            if (team == null) {
                try {
                    team = scoreboard.createTeam(teamName);
                } catch (Exception e) {
                    ci.cancel();
                    return;
                }
            }

            // 执行修正逻辑
            fixBungeeTabList(scoreboard, team, teamName, packet.func_149311_e());

            // Action 2 不取消，让原版继续更新它的属性
        }

        // --- 5. 防崩 Action 3/4 (Add/Remove Player) ---
        if ((action == 3 || action == 4) && team == null) {
            try {
                scoreboard.createTeam(teamName);
            } catch (Exception e) {
                ci.cancel();
            }
        }
    }

    /**
     * 核心修复逻辑：把影子名和真实名都塞进队伍
     */
    private void fixBungeeTabList(Scoreboard scoreboard, ScorePlayerTeam team, String teamName, String rawPrefix) {
        // 策略 A: 影子玩家关联 (解决 Bungee 乱码不显示)
        try {
            if (!team.getMembershipCollection()
                .contains(teamName)) {
                scoreboard.func_151392_a(teamName, teamName);
            }
        } catch (Exception ignored) {}

        // 策略 B: 真实玩家关联 (从前缀提取)
        if (rawPrefix != null && !rawPrefix.isEmpty()) {
            String likelyUsername = stripColor(rawPrefix);
            if (likelyUsername != null && likelyUsername.length() > 1) {
                try {
                    if (!team.getMembershipCollection()
                        .contains(likelyUsername)) {
                        scoreboard.func_151392_a(likelyUsername, teamName);
                        System.out
                            .println("[FixMod] [ScoreboardFix] Auto-linked: " + likelyUsername + " to " + teamName);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private String stripColor(String input) {
        if (input == null) return null;
        // try {
        // return EnumChatFormatting.func_110646_a(input);
        // } catch (Throwable t) {
        return STRIP_COLOR_PATTERN.matcher(input)
            .replaceAll("");
        // }
    }
}
