package dk.mineconed.banliste;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Made by Mineconed
 *
 * Banlist: a menu with every banned player (newest first), reason,
 * who banned them, date and when the ban ends. Click to unban.
 * Works on 1.8 to 1.21.
 */
public final class Banliste extends JavaPlugin implements Listener {

    private static final int PR_SIDE = 45;

    // ─────────────── Menu holders ───────────────

    private static final class ListeMenu implements InventoryHolder {
        Inventory inv;
        final List<String> navne;
        final String filter;
        final int side;

        ListeMenu(List<String> navne, String filter, int side) {
            this.navne = navne;
            this.filter = filter;
            this.side = side;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static final class BekraeftMenu implements InventoryHolder {
        Inventory inv;
        final String navn;
        final ListeMenu tilbage;

        BekraeftMenu(String navn, ListeMenu tilbage) {
            this.navn = navn;
            this.tilbage = tilbage;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private YamlConfiguration msg;

    private void indlaesBeskeder() {
        for (String n : new String[]{"messages_en.yml", "messages_da.yml"}) {
            if (!new File(getDataFolder(), n).exists()) {
                saveResource(n, false);
            }
        }
        File f = new File(getDataFolder(), "messages_" + getConfig().getString("language", "en") + ".yml");
        if (!f.exists()) {
            f = new File(getDataFolder(), "messages_en.yml");
        }
        msg = YamlConfiguration.loadConfiguration(f);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        indlaesBeskeder();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Banlist is enabled. Made by Mineconed.");
    }

    // ─────────────── Hjælpere ───────────────

    private String m(String noegle, String... ph) {
        String s = msg == null ? noegle : msg.getString(noegle, noegle);
        for (int i = 0; i + 1 < ph.length; i += 2) {
            s = s.replace(ph[i], ph[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String f(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Items that work on both old (1.8) and new (1.13+) servers. */
    private static ItemStack item(String nyt, String gammelt, int data) {
        Material mt = Material.matchMaterial(nyt);
        if (mt != null) {
            return new ItemStack(mt);
        }
        mt = Material.matchMaterial(gammelt);
        return new ItemStack(mt == null ? Material.STONE : mt, 1, (short) data);
    }

    private static ItemStack navngiv(ItemStack i, String navn, List<String> lore) {
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(f(navn));
        if (lore != null) {
            List<String> l = new ArrayList<String>();
            for (String s : lore) {
                l.add(f(s));
            }
            meta.setLore(l);
        }
        i.setItemMeta(meta);
        return i;
    }

    private static void lyd(Player p, String... navne) {
        for (String n : navne) {
            try {
                p.playSound(p.getLocation(), Sound.valueOf(n), 1f, 1f);
                return;
            } catch (IllegalArgumentException ignore) {
                // prøv næste navn
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static BanList liste() {
        return Bukkit.getBanList(BanList.Type.NAME);
    }

    private String dato(Date d) {
        if (d == null) {
            return m("ukendt");
        }
        return new SimpleDateFormat(getConfig().getString("dato_format", "dd/MM/yyyy HH:mm")).format(d);
    }

    private static String tilbage(Date d) {
        long s = Math.subtractExact(d.getTime(), System.currentTimeMillis()) / 1000L;
        if (s <= 0) {
            return "0m";
        }
        long dg = s / 86400L;
        long t = (s % 86400L) / 3600L;
        long mn = (s % 3600L) / 60L;
        if (dg > 0) {
            return dg + "d " + t + "t";
        }
        if (t > 0) {
            return t + "t " + mn + "m";
        }
        return mn + "m";
    }

    private String grund(BanEntry e) {
        String g = e.getReason();
        return g == null || g.trim().isEmpty() ? m("ingen_grund") : ChatColor.stripColor(g);
    }

    /** filter: alle, temp eller perm. Nyeste først. */
    @SuppressWarnings("unchecked")
    private List<String> hent(String filter) {
        List<BanEntry> l = new ArrayList<BanEntry>();
        for (Object o : liste().getBanEntries()) {
            BanEntry e = (BanEntry) o;
            boolean temp = e.getExpiration() != null;
            if (filter.equals("temp") && !temp) {
                continue;
            }
            if (filter.equals("perm") && temp) {
                continue;
            }
            l.add(e);
        }
        Collections.sort(l, new Comparator<BanEntry>() {
            @Override
            public int compare(BanEntry a, BanEntry b) {
                long ta = a.getCreated() == null ? 0 : a.getCreated().getTime();
                long tb = b.getCreated() == null ? 0 : b.getCreated().getTime();
                return Long.compare(tb, ta);
            }
        });
        List<String> navne = new ArrayList<String>();
        for (BanEntry e : l) {
            navne.add(e.getTarget());
        }
        return navne;
    }

    // ─────────────── Items ───────────────

    @SuppressWarnings("deprecation")
    private ItemStack hoved(String navn, boolean kanUnban) {
        ItemStack i = item("PLAYER_HEAD", "SKULL_ITEM", 3);
        SkullMeta sm = (SkullMeta) i.getItemMeta();
        sm.setOwner(navn);
        i.setItemMeta(sm);
        BanEntry e = liste().getBanEntry(navn);
        if (e == null) {
            return navngiv(i, "&7" + navn, Arrays.asList("&a" + m("ikke_bannet_mere")));
        }
        Date udl = e.getExpiration();
        List<String> l = new ArrayList<String>();
        l.add("&8&m                         ");
        l.add(m("lore_type", "{type}", udl == null ? m("permanent") : m("tempban")));
        l.add(m("lore_grund", "{grund}", grund(e)));
        l.add(m("lore_af", "{af}", e.getSource() == null ? "?" : e.getSource()));
        l.add(m("lore_dato", "{dato}", dato(e.getCreated())));
        l.add(udl == null ? m("lore_udloeber_aldrig") : m("lore_udloeber", "{dato}", dato(udl), "{tid}", tilbage(udl)));
        l.add("&8&m                         ");
        if (kanUnban) {
            l.add(m("lore_klik_unban"));
        }
        return navngiv(i, "&c&l" + navn, l);
    }

    // ─────────────── Menuer ───────────────

    private void aabnListe(Player p, String filter, int side) {
        List<String> navne = hent(filter);
        int sider = Math.max(1, (int) Math.ceil(navne.size() / (double) PR_SIDE));
        side = Math.max(1, Math.min(side, sider));
        ListeMenu h = new ListeMenu(navne, filter, side);
        String titel = f(m("titel", "{side}", String.valueOf(side), "{sider}", String.valueOf(sider)));
        if (titel.length() > 32) {
            titel = titel.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(h, 54, titel);
        h.inv = inv;
        boolean kan = p.hasPermission("banliste.unban");
        int start = Math.multiplyExact(Math.subtractExact(side, 1), PR_SIDE);
        for (int i = 0; i < PR_SIDE && start + i < navne.size(); i++) {
            inv.setItem(i, hoved(navne.get(start + i), kan));
        }
        ItemStack glas = navngiv(item("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 7), " ", null);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glas);
        }
        if (side > 1) {
            inv.setItem(45, navngiv(new ItemStack(Material.ARROW), m("knap_forrige"), null));
        }
        if (side < sider) {
            inv.setItem(53, navngiv(new ItemStack(Material.ARROW), m("knap_naeste"), null));
        }
        if (navne.isEmpty()) {
            inv.setItem(22, navngiv(item("LIME_WOOL", "WOOL", 5), m("ingen_bannede"), null));
        }
        String fNavn = filter.equals("temp") ? m("filter_temp") : filter.equals("perm") ? m("filter_perm") : m("filter_alle");
        inv.setItem(49, navngiv(new ItemStack(Material.BOOK), m("info_titel", "{antal}", String.valueOf(navne.size())),
                Arrays.asList(m("info_filter", "{filter}", fNavn), m("info_nyeste"), m("info_soeg"))));
        inv.setItem(47, navngiv(item("CLOCK", "WATCH", 0), m("knap_filter"),
                Arrays.asList(m("filter_linje", "{f}", filter.equals("alle") ? "&a" : "&7", "{navn}", m("filter_alle")),
                        m("filter_linje", "{f}", filter.equals("temp") ? "&a" : "&7", "{navn}", m("filter_temp")),
                        m("filter_linje", "{f}", filter.equals("perm") ? "&a" : "&7", "{navn}", m("filter_perm")),
                        "", m("knap_filter_klik"))));
        inv.setItem(51, navngiv(new ItemStack(Material.BARRIER), m("knap_luk"), null));
        p.openInventory(inv);
    }

    private void aabnBekraeft(Player p, String navn, ListeMenu tilbage) {
        BekraeftMenu h = new BekraeftMenu(navn, tilbage);
        String titel = f(m("titel_bekraeft", "{navn}", navn));
        if (titel.length() > 32) {
            titel = titel.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(h, 27, titel);
        h.inv = inv;
        ItemStack glas = navngiv(item("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 15), " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glas);
        }
        inv.setItem(11, navngiv(item("LIME_WOOL", "WOOL", 5), m("knap_ja"), Arrays.asList(m("knap_ja_lore", "{navn}", navn))));
        inv.setItem(13, hoved(navn, false));
        inv.setItem(15, navngiv(item("RED_WOOL", "WOOL", 14), m("knap_nej"), Arrays.asList(m("knap_nej_lore"))));
        p.openInventory(inv);
    }

    private void unban(CommandSender s, String navn) {
        String cmd = getConfig().getString("unban_kommando", "");
        if (cmd == null || cmd.trim().isEmpty()) {
            liste().pardon(navn);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{navn}", navn));
        }
        s.sendMessage(m("unbannet", "{navn}", navn));
        getLogger().info(s.getName() + " unbanned " + navn);
    }

    // ─────────────── Klik ───────────────

    @EventHandler
    public void onKlik(InventoryClickEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (!(h instanceof ListeMenu) && !(h instanceof BekraeftMenu)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) {
            return;
        }
        if (h instanceof ListeMenu) {
            final ListeMenu lm = (ListeMenu) h;
            if (slot == 45 && lm.side > 1) {
                senere(p, lm.filter, Math.subtractExact(lm.side, 1));
            } else if (slot == 53) {
                senere(p, lm.filter, lm.side + 1);
            } else if (slot == 47) {
                String ny = lm.filter.equals("alle") ? "temp" : lm.filter.equals("temp") ? "perm" : "alle";
                senere(p, ny, 1);
            } else if (slot == 51) {
                p.closeInventory();
            } else if (slot < PR_SIDE && p.hasPermission("banliste.unban")) {
                int nr = Math.multiplyExact(Math.subtractExact(lm.side, 1), PR_SIDE) + slot;
                if (nr < lm.navne.size()) {
                    final String navn = lm.navne.get(nr);
                    Bukkit.getScheduler().runTask(this, new Runnable() {
                        @Override
                        public void run() {
                            aabnBekraeft(p, navn, lm);
                        }
                    });
                }
            }
            return;
        }
        final BekraeftMenu bm = (BekraeftMenu) h;
        if (slot == 11 && p.hasPermission("banliste.unban")) {
            unban(p, bm.navn);
            lyd(p, "LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    aabnListe(p, bm.tilbage.filter, bm.tilbage.side);
                }
            }, 5L);
        } else if (slot == 15) {
            senere(p, bm.tilbage.filter, bm.tilbage.side);
        }
    }

    private void senere(final Player p, final String filter, final int side) {
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                aabnListe(p, filter, side);
            }
        });
    }

    @EventHandler
    public void onTraek(InventoryDragEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof ListeMenu || h instanceof BekraeftMenu) {
            e.setCancelled(true);
        }
    }

    // ─────────────── Kommando ───────────────

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("banliste.se")) {
            s.sendMessage(m("ingen_adgang"));
            return true;
        }
        if (a.length > 0 && a[0].equalsIgnoreCase("reload") && s.hasPermission("banliste.unban")) {
            reloadConfig();
            indlaesBeskeder();
            s.sendMessage(m("genindlaest"));
            return true;
        }
        if (a.length > 0 && (a[0].equalsIgnoreCase("temp") || a[0].equalsIgnoreCase("tempbans") || a[0].equalsIgnoreCase("perm"))) {
            if (s instanceof Player) {
                aabnListe((Player) s, a[0].toLowerCase().startsWith("temp") ? "temp" : "perm", 1);
            }
            return true;
        }
        if (a.length > 0) {
            BanEntry e = liste().getBanEntry(a[0]);
            if (e == null) {
                s.sendMessage(m("ikke_bannet", "{navn}", a[0]));
                return true;
            }
            Date udl = e.getExpiration();
            s.sendMessage("");
            s.sendMessage(m("chat_titel", "{navn}", e.getTarget()));
            s.sendMessage(m("chat_linje", "{tekst}", m("lore_type", "{type}", udl == null ? m("permanent") : m("tempban"))));
            s.sendMessage(m("chat_linje", "{tekst}", m("lore_grund", "{grund}", grund(e))));
            s.sendMessage(m("chat_linje", "{tekst}", m("lore_af", "{af}", e.getSource() == null ? "?" : e.getSource())));
            s.sendMessage(m("chat_linje", "{tekst}", m("lore_dato", "{dato}", dato(e.getCreated()))));
            s.sendMessage(m("chat_linje", "{tekst}", udl == null ? m("lore_udloeber_aldrig")
                    : m("lore_udloeber", "{dato}", dato(udl), "{tid}", tilbage(udl))));
            s.sendMessage("");
            return true;
        }
        if (!(s instanceof Player)) {
            s.sendMessage(m("antal_konsol", "{antal}", String.valueOf(hent("alle").size())));
            return true;
        }
        aabnListe((Player) s, "alle", 1);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> ud = new ArrayList<String>();
        if (a.length == 1 && s.hasPermission("banliste.se")) {
            String start = a[0].toLowerCase();
            for (String n : hent("alle")) {
                if (n.toLowerCase().startsWith(start)) {
                    ud.add(n);
                }
            }
            for (String n : new String[]{"temp", "perm"}) {
                if (n.startsWith(start)) {
                    ud.add(n);
                }
            }
        }
        return ud;
    }
}
