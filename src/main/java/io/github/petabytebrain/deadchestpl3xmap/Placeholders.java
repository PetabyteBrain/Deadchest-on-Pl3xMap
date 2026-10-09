package io.github.petabytebrain.deadchestpl3xmap;

/**
 * Fills {placeholders} in the tooltip/popup templates.
 */
final class Placeholders {
    private Placeholders() {
    }

    static String apply(String template, ChestSnapshot chest, Settings s, long now) {
        String status;
        if (chest.publicLoot() == null) {
            status = "?";
        } else {
            status = chest.publicLoot() ? s.statusPublic() : s.statusPrivate();
        }

        String timeLeft;
        Long left = chest.timeLeftMillis();
        if (left == null) {
            timeLeft = "?";
        } else if (left < 0) {
            timeLeft = s.timeNever();
        } else {
            timeLeft = formatDuration(left);
        }

        return template
                .replace("{player}", escape(chest.playerName()))
                .replace("{world}", escape(chest.worldName()))
                .replace("{x}", Integer.toString(chest.x()))
                .replace("{y}", Integer.toString(chest.y()))
                .replace("{z}", Integer.toString(chest.z()))
                .replace("{items}", Integer.toString(chest.items()))
                .replace("{xp}", Integer.toString(chest.xp()))
                .replace("{status}", status)
                .replace("{time_left}", timeLeft)
                .replace("{died}", formatDuration(Math.max(0L, now - chest.deathTimeMillis())))
                .replace("\n", "");
    }

    static String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    static String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
