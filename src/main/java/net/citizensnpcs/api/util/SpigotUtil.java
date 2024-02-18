package net.citizensnpcs.api.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class SpigotUtil {
    private static interface ThrowingConsumer<T> {
        void accept(T t) throws ClassNotFoundException;
    }

    public static boolean checkYSafe(double y, World world) {
        if (!SUPPORT_WORLD_HEIGHT || world == null)
            return y >= 0 && y <= 255;

        try {
            return y >= world.getMinHeight() && y <= world.getMaxHeight();
        } catch (Throwable t) {
            SUPPORT_WORLD_HEIGHT = false;
            return y >= 0 && y <= 255;
        }
    }

    public static int getMaxNameLength(EntityType type) {
        return isUsing1_13API() ? 256 : 64;
    }

    public static String getMinecraftPackage() {
        if (MINECRAFT_PACKAGE == null) {
            int[] version = getVersion();
            if (version == null)
                throw new IllegalStateException();
            String versionString = "v" + version[0] + "_" + version[1] + "_R";
            ThrowingConsumer<String> versionChecker = s -> Class
                    .forName("org.bukkit.craftbukkit." + s + ".CraftServer");
            if (Bukkit.getServer().getClass().getName().equals("org.bukkit.craftbukkit.CraftServer")) {
                Messaging.log("Using mojmapped server, avoiding server package checks");
                versionChecker = s -> Class.forName("net.citizensnpcs.nms." + s + ".util.NMSImpl");
            }
            String revision = null;
            for (int i = 1; i <= 3; i++) {
                try {
                    versionChecker.accept(versionString + i);
                    revision = versionString + i;
                    break;
                } catch (ClassNotFoundException e) {
                }
            }
            if (revision == null)
                throw new IllegalStateException();
            MINECRAFT_PACKAGE = revision;
        }
        return MINECRAFT_PACKAGE;

    }

    public static int[] getVersion() {
        if (BUKKIT_VERSION == null) {
            String version = Bukkit.getBukkitVersion();

            if (version == null || version.isEmpty())
                return BUKKIT_VERSION = new int[] { 1, 8 };

            String[] parts = version.split("\\.");
            if (parts[1].contains("-")) {
                parts[1] = parts[1].split("-")[0];
            }
            return BUKKIT_VERSION = new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
        }
        return BUKKIT_VERSION;
    }

    public static boolean isUsing1_13API() {
        if (using1_13API == null) {
            try {
                Enchantment.getByKey(Enchantment.ARROW_DAMAGE.getKey());
                using1_13API = true;
            } catch (Exception ex) {
                using1_13API = false;
            } catch (NoSuchMethodError ex) {
                using1_13API = false;
            }
        }
        return using1_13API;
    }

    public static Duration parseDuration(String raw, TimeUnit defaultUnits) {
        if (defaultUnits == null) {
            Integer ticks = Ints.tryParse(raw);
            if (ticks != null)
                return Duration.ofMillis(ticks * 50);
        } else if (NUMBER_MATCHER.matcher(raw).matches())
            return Duration.of(Longs.tryParse(raw), toChronoUnit(defaultUnits));
        if (raw.endsWith("t"))
            return Duration.ofMillis(Integer.parseInt(raw.substring(0, raw.length() - 1)) * 50);
        raw = DAY_MATCHER.matcher(raw).replaceFirst("P$1T").replace("min", "m").replace("hr", "h");
        if (raw.charAt(0) != 'P') {
            raw = "PT" + raw;
        }
        return Duration.parse(raw);
    }

    private static ChronoUnit toChronoUnit(TimeUnit tu) {
        switch (tu) {
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case HOURS:
                return ChronoUnit.HOURS;
            case DAYS:
                return ChronoUnit.DAYS;
            default:
                throw new AssertionError();
        }
    }

    private static int[] BUKKIT_VERSION = null;
    private static Pattern DAY_MATCHER = Pattern.compile("(\\d+d)");
    private static String MINECRAFT_PACKAGE;
    private static Pattern NUMBER_MATCHER = Pattern.compile("(\\d+)");
    private static boolean SUPPORT_WORLD_HEIGHT = true;
    private static Boolean using1_13API;
}
