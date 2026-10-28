package dev.neonlyrics;

/** Licensing compatibility shim. The mod is fully enabled without activation. */
public final class License {
   private License() {}

   public static boolean ok() { return true; }
   public static String uuid() { return ""; }
   public static String name() { return "Player"; }
   public static String normalize(String code) { return code == null ? "" : code; }
   public static boolean isRedeemCode(String code) { return false; }
   public static boolean check(String uuid, String code) { return true; }
}
