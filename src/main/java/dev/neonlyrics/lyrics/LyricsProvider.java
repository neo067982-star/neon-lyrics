package dev.neonlyrics.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Lyrics retrieval and conversion for LyricsPlus JSON, Apple TTML, and LRC. */
public final class LyricsProvider {
   private static final String LYRICSPLUS = "https://lp.rmmreviv.al/v2/lyrics/get";
   private static final String LRC_RED = "https://lrc.red";
   private static final String LRCLIB = "https://lrclib.net/api";
   private static final String SOURCES = "apple,unison,lyricsplus,qq,netease,kugou,soda,deezer,musixmatch";
   private static final java.util.regex.Pattern LRC = java.util.regex.Pattern.compile("\\[(\\d{1,3}):(\\d{1,2}(?:[.:]\\d{1,3})?)]");
   private static final java.util.regex.Pattern WORD_TAG = java.util.regex.Pattern.compile("<(\\d{1,3}):(\\d{1,2}(?:[.:]\\d{1,3})?)>");
   private static final Map<String, List<NowPlaying.Line>> CACHE = new HashMap<>();
   private static final ExecutorService PROVIDER_EXEC = Executors.newFixedThreadPool(3, r -> {
      Thread t = new Thread(r, "NeonLyrics-Provider");
      t.setDaemon(true);
      return t;
   });

   private LyricsProvider() {}
   public static synchronized List<NowPlaying.Line> cached(String trackId) {
      List<NowPlaying.Line> hit = CACHE.get(trackId);
      // An empty result is a failed/negative lookup, not durable proof that
      // the track has no lyrics. Retry it after a provider/parser transient.
      return hit != null && hit.isEmpty() ? null : hit;
   }

   public static List<NowPlaying.Line> fetch(String trackId, String title, String artist, String album, long durationMs) throws Exception {
      List<NowPlaying.Line> hit = cached(trackId);
      if (hit != null) return hit;
      // Start every provider concurrently. Selection below still follows the
      // quality order, but a slow/dead provider cannot block the others.
      Future<List<NowPlaying.Line>> lyricsPlus = PROVIDER_EXEC.submit(() -> fetchLyricsPlus(title, artist, album, durationMs));
      Future<List<NowPlaying.Line>> lrcRed = PROVIDER_EXEC.submit(() -> {
         try { return fetchLrcRed(title, artist, durationMs); } catch (Exception ignored) { return null; }
      });
      Future<List<NowPlaying.Line>> lrcLib = PROVIDER_EXEC.submit(() -> {
         try { return fetchLrcLib(title, artist, album, durationMs); } catch (Exception ignored) { return null; }
      });

      List<NowPlaying.Line> primary = await(lyricsPlus, 12500L);
      List<NowPlaying.Line> richer = await(lrcRed, 2000L);
      List<NowPlaying.Line> lines;
      if (hasWordTiming(primary)) lines = primary;
      else if (hasWordTiming(richer)) lines = richer;
      else if (primary != null && !primary.isEmpty()) lines = primary;
      else if (richer != null && !richer.isEmpty()) lines = richer;
      else lines = await(lrcLib, 1000L);
      lyricsPlus.cancel(true);
      lrcRed.cancel(true);
      lrcLib.cancel(true);
      if (lines != null && lines.isEmpty()) lines = null;
      synchronized (LyricsProvider.class) {
         CACHE.put(trackId, lines == null ? Collections.emptyList() : lines);
         if (CACHE.size() > 60) CACHE.clear();
         return lines;
      }
   }

   private static List<NowPlaying.Line> fetchLyricsPlus(String title, String artist, String album, long durationMs) {
      try {
         StringBuilder u = new StringBuilder(LYRICSPLUS).append("?title=").append(enc(title)).append("&artist=").append(enc(artist)).append("&source=").append(enc(SOURCES));
         if (album != null && !album.isBlank()) u.append("&album=").append(enc(album));
         if (durationMs > 0) u.append("&duration=").append(Math.round(durationMs / 1000.0));
         Http.Response r = Http.get(u.toString(), Map.of("Accept", "application/json, application/xml"));
         return r.code() == 200 && !r.body().isBlank() ? parseLyricsPlus(r.body()) : null;
      } catch (Exception ignored) { return null; }
   }

   private static List<NowPlaying.Line> await(Future<List<NowPlaying.Line>> future, long timeoutMs) {
      try { return future.get(timeoutMs, TimeUnit.MILLISECONDS); }
      catch (Exception ignored) { return null; }
   }

   private static List<NowPlaying.Line> fetchLrcRed(String title, String artist, long durationMs) throws Exception {
      Http.Response search = Http.get(LRC_RED + "/search.json?q=" + enc(title + " " + artist), Map.of(
         "User-Agent", "NeonLyricsHUD/2.1 (+https://lrc.red)"
      ));
      if (search.code() != 200) return null;
      JsonObject result = JsonParser.parseString(search.body()).getAsJsonObject();
      if (!result.has("hits") || !result.get("hits").isJsonArray()) return null;
      JsonObject best = null; double bestScore = Double.MAX_VALUE;
      for (JsonElement item : result.getAsJsonArray("hits")) {
         JsonObject hit = item.getAsJsonObject();
         double duration = hit.has("duration") && !hit.get("duration").isJsonNull() ? hit.get("duration").getAsDouble() : -1;
         double score = durationMs > 0 && duration > 0 ? Math.abs(duration - durationMs / 1000.0) : 30;
         String hitTitle = string(hit, "title");
         String hitArtist = string(hit, "artist");
         if (hitTitle != null && hitArtist != null && hitTitle.equalsIgnoreCase(title) && hitArtist.equalsIgnoreCase(artist)) score -= 10;
         if (score < bestScore && (durationMs <= 0 || duration < 0 || score < 8)) { bestScore = score; best = hit; }
      }
      if (best == null || !best.has("isrc")) return null;
      String isrc = best.get("isrc").getAsString();
      Http.Response ttml = Http.get(LRC_RED + "/s/" + enc(isrc) + ".ttml", Map.of(
         "User-Agent", "NeonLyricsHUD/2.1 (+https://lrc.red)"
      ));
      if (ttml.code() == 200 && !ttml.body().isBlank()) return parseTtml(ttml.body());
      Http.Response lrc = Http.get(LRC_RED + "/s/" + enc(isrc) + ".lrc", Map.of(
         "User-Agent", "NeonLyricsHUD/2.1 (+https://lrc.red)"
      ));
      return lrc.code() == 200 && !lrc.body().isBlank() ? parse(lrc.body()) : null;
   }

   private static List<NowPlaying.Line> fetchLrcLib(String title, String artist, String album, long durationMs) throws Exception {
      String url = LRCLIB + "/get?track_name=" + enc(title) + "&artist_name=" + enc(artist) + (album != null && !album.isBlank() ? "&album_name=" + enc(album) : "") + (durationMs > 0 ? "&duration=" + Math.round(durationMs / 1000.0) : "");
      String synced = null;
      Http.Response r = Http.get(url, Map.of());
      if (r.code() == 200) synced = string(JsonParser.parseString(r.body()).getAsJsonObject(), "syncedLyrics");
      if (synced == null) {
         Http.Response s = Http.get(LRCLIB + "/search?track_name=" + enc(title) + "&artist_name=" + enc(artist), Map.of());
         if (s.code() == 200) {
            double best = Double.MAX_VALUE;
            for (JsonElement e : JsonParser.parseString(s.body()).getAsJsonArray()) {
               JsonObject o = e.getAsJsonObject(); String candidate = string(o, "syncedLyrics");
               double d = durationMs <= 0 ? 0 : (o.has("duration") && !o.get("duration").isJsonNull() ? Math.abs(o.get("duration").getAsDouble() - durationMs / 1000.0) : 30);
               if (candidate != null && d < best && d < 8) { best = d; synced = candidate; }
            }
         }
      }
      return synced == null ? null : parse(synced);
   }

   private static List<NowPlaying.Line> parseLyricsPlus(String body) throws Exception {
      String trimmed = body.trim();
      if (trimmed.startsWith("<")) return parseTtml(trimmed);
      JsonObject root = JsonParser.parseString(trimmed).getAsJsonObject();
      if (root.has("ttml") && !root.get("ttml").isJsonNull()) return parseTtml(root.get("ttml").getAsString());
      JsonElement data = root.has("data") && root.get("data").isJsonObject() ? root.get("data") : root;
      if (!data.isJsonObject() || !data.getAsJsonObject().has("lyrics")) return null;
      List<NowPlaying.Line> out = new ArrayList<>();
      for (JsonElement e : data.getAsJsonObject().getAsJsonArray("lyrics")) {
         JsonObject line = e.getAsJsonObject(); long time = number(line, "time", number(line, "start", -1)); String text = string(line, "text");
           List<String> fragments = new ArrayList<>(); List<Long> fragmentTimes = new ArrayList<>(); List<Long> fragmentEnds = new ArrayList<>(); List<Boolean> background = new ArrayList<>();
          JsonArray syllables = line.has("syllabus") && line.get("syllabus").isJsonArray() ? line.getAsJsonArray("syllabus") : null;
           if (syllables != null) for (JsonElement s : syllables) {
             JsonObject word = s.getAsJsonObject(); String w = string(word, "text"); long wt = number(word, "time", number(word, "start", time));
              w = displayText(w);
              if (w != null && !w.isBlank()) {
                boolean bg = bool(word, "isBackground");
                if (!background.isEmpty() && bg != background.get(background.size() - 1)) {
                   int last = fragments.size() - 1;
                   if (!Character.isWhitespace(fragments.get(last).charAt(fragments.get(last).length() - 1))) fragments.set(last, fragments.get(last) + " ");
                }
              fragments.add(w); fragmentTimes.add(wt); fragmentEnds.add(wt + Math.max(0L, number(word, "duration", 0L))); background.add(bg);
              }
           }
           // Background vocals can be appended after the lead vocal in the
           // provider response even when they occur earlier in the sentence.
           // Normalize by timestamp before building display words.
           List<Integer> order = new ArrayList<>();
           for (int oi = 0; oi < fragments.size(); oi++) order.add(oi);
           List<Long> timesForSort = fragmentTimes;
           order.sort((a, b) -> Long.compare(timesForSort.get(a), timesForSort.get(b)));
           if (!order.isEmpty()) {
              List<String> sortedFragments = new ArrayList<>(); List<Long> sortedTimes = new ArrayList<>(); List<Long> sortedEnds = new ArrayList<>(); List<Boolean> sortedBackground = new ArrayList<>();
              for (int oi : order) { sortedFragments.add(fragments.get(oi)); sortedTimes.add(fragmentTimes.get(oi)); sortedEnds.add(fragmentEnds.get(oi)); sortedBackground.add(background.get(oi)); }
              fragments = sortedFragments; fragmentTimes = sortedTimes; fragmentEnds = sortedEnds; background = sortedBackground;
           }
           // Group syllable fragments into source words for display while
           // retaining the full timing interval of the grouped word.
           List<String> words = new ArrayList<>(); List<Long> times = new ArrayList<>(); List<Long> ends = new ArrayList<>(); List<Boolean> wordBackground = new ArrayList<>();
           for (int fi = 0; fi < fragments.size(); fi++) {
              boolean boundary = fi == 0 || background.get(fi) || background.get(fi - 1)
                 || Character.isWhitespace(fragments.get(fi - 1).charAt(fragments.get(fi - 1).length() - 1));
              if (boundary) {
                 words.add(fragments.get(fi).trim()); times.add(fragmentTimes.get(fi)); ends.add(fragmentEnds.get(fi)); wordBackground.add(background.get(fi));
              } else {
                 int wi = words.size() - 1;
                 words.set(wi, words.get(wi) + fragments.get(fi).trim());
                 ends.set(wi, Math.max(ends.get(wi), fragmentEnds.get(fi)));
              }
           }
          if (text != null && time >= 0) {
              String rebuilt = displayText(text);
              if (!words.isEmpty()) {
                 List<String> foreground = new ArrayList<>(); List<String> backing = new ArrayList<>();
                 for (int wi = 0; wi < words.size(); wi++) {
                    String word = words.get(wi).trim();
                    if (word.isEmpty()) continue;
                    if (wordBackground.get(wi)) backing.add(word); else foreground.add(word);
                 }
                 String lead = String.join(" ", foreground).trim();
                 String bg = String.join(" ", backing).trim();
                 if (!bg.isEmpty()) rebuilt = lead.isEmpty() ? "(" + bg + ")" : lead + " (" + bg + ")";
                 else if (!lead.isEmpty()) rebuilt = lead;
              }
              out.add(new NowPlaying.Line(time, rebuilt, words.toArray(new String[0]), times.isEmpty() ? null : toLongs(times), ends.isEmpty() ? null : toLongs(ends)));
          }
      }
      out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs())); return out;
   }

   private static List<NowPlaying.Line> parseTtml(String xml) throws Exception {
      var f = DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true);
      var doc = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
      NodeList ps = doc.getElementsByTagNameNS("*", "p"); if (ps.getLength() == 0) ps = doc.getElementsByTagName("p");
      List<NowPlaying.Line> out = new ArrayList<>();
      for (int i = 0; i < ps.getLength(); i++) {
         Element p = (Element) ps.item(i); NodeList spans = p.getElementsByTagNameNS("*", "span"); if (spans.getLength() == 0) spans = p.getElementsByTagName("span");
          long lineTime = time(timingAttr(p, "begin"), -1);
           List<String> fragments = new ArrayList<>(); List<Long> fragmentTimes = new ArrayList<>(); List<Long> fragmentEnds = new ArrayList<>(); List<Boolean> boundaries = new ArrayList<>();
          for (int j = 0; j < spans.getLength(); j++) {
             Element span = (Element) spans.item(j);
             // TTML background vocals are commonly wrapped in an untimed
             // <span ttm:role="x-bg"> containing timed child spans.  The
             // wrapper must not be treated as a lyric syllable: doing so
             // duplicates the entire backing vocal before its child timings.
             String begin = timingAttr(span, "begin");
             if (begin == null || begin.isBlank() || hasBackgroundRole(span)) continue;
             long t = time(begin, lineTime < 0 ? 0 : lineTime);
             if (lineTime < 0) lineTime = t;
             String s = directText(span).replaceAll("\\s+", " ");
              if (!s.isBlank()) { fragments.add(s); fragmentTimes.add(t); fragmentEnds.add(time(timingAttr(span, "end"), t)); boundaries.add(fragments.size() == 1 || whitespaceBefore(span)); }
           }
           List<String> words = new ArrayList<>(); List<Long> times = new ArrayList<>(); List<Long> ends = new ArrayList<>();
           for (int fi = 0; fi < fragments.size(); fi++) {
              if (fi == 0 || boundaries.get(fi)) {
                 words.add(fragments.get(fi)); times.add(fragmentTimes.get(fi)); ends.add(fragmentEnds.get(fi));
              } else {
                 int wi = words.size() - 1;
                 words.set(wi, words.get(wi) + fragments.get(fi));
                 ends.set(wi, Math.max(ends.get(wi), fragmentEnds.get(fi)));
              }
           }
          // lrc.red word TTML intentionally omits whitespace between timed
          // spans.  Reading p.textContent directly turns "I", "been",
          // "tryna", "call" into one word, so rebuild the visible line from
          // the timed units and insert a boundary between them.
           String text = displayText(fragments.isEmpty() ? p.getTextContent().replaceAll("\\s+", " ").trim() : joinTimedText(fragments));
          if (!text.isEmpty() && lineTime >= 0) {
             // Line-synced TTML has no timed child spans.  Give it the normal
             // word list so renderers can interpolate the line; an empty word
             // array makes some consumers collapse the entire line to one unit.
             out.add(fragments.isEmpty() ? line(lineTime, text)
                 : new NowPlaying.Line(lineTime, text, words.toArray(new String[0]), times.isEmpty() ? null : toLongs(times), ends.isEmpty() ? null : toLongs(ends)));
          }
      }
      out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs())); return out;
   }

   private static boolean hasWordTiming(List<NowPlaying.Line> lines) {
      if (lines == null) return false;
      for (NowPlaying.Line l : lines) {
         if (l.wordTimes() != null && l.wordTimes().length > 0 && l.wordTimes().length == l.words().length) return true;
      }
      return false;
   }

    private static String attr(Element e, String name) { String v = e.getAttribute(name); return v == null || v.isBlank() ? null : v; }
    private static String timingAttr(Element e, String name) {
       String v = attr(e, name);
       if (v != null) return v;
       for (int i = 0; i < e.getAttributes().getLength(); i++) {
          var a = e.getAttributes().item(i);
          String local = a.getLocalName();
          if (local == null) local = a.getNodeName().replaceFirst("^.*:", "");
          if (name.equals(local)) return a.getNodeValue();
       }
       return null;
    }
    private static boolean hasBackgroundRole(Element e) {
       String role = e.getAttributeNS("http://www.w3.org/ns/ttml#parameter", "role");
       if (role == null || role.isBlank()) role = e.getAttribute("ttm:role");
       return "x-bg".equalsIgnoreCase(role);
    }
     private static String directText(Element e) {
       StringBuilder out = new StringBuilder();
       for (int i = 0; i < e.getChildNodes().getLength(); i++) {
          var n = e.getChildNodes().item(i);
          if (n.getNodeType() == org.w3c.dom.Node.TEXT_NODE) out.append(n.getNodeValue());
       }
        return out.toString();
     }
     private static boolean whitespaceBefore(Element e) {
        var n = e.getPreviousSibling();
        while (n != null) {
           if (n.getNodeType() == org.w3c.dom.Node.TEXT_NODE) return n.getNodeValue().matches(".*\\s+.*");
           if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) return false;
           n = n.getPreviousSibling();
        }
        return false;
     }
    private static String joinTimedText(List<String> fragments) {
       StringBuilder out = new StringBuilder();
       for (String fragment : fragments) {
          String s = fragment.replaceAll("\\s+", " ").trim();
          if (s.isEmpty()) continue;
          if (out.length() > 0 && !Character.isWhitespace(out.charAt(out.length() - 1))) out.append(' ');
          out.append(s);
       }
       return out.toString().trim();
    }
    private static List<String> timedWords(List<String> fragments) {
       List<String> out = new ArrayList<>();
       for (String fragment : fragments) {
          for (String word : fragment.trim().split("\\s+")) if (!word.isBlank()) out.add(word);
       }
       return out;
    }
    private static List<Long> timedWordTimes(List<String> fragments, List<Long> starts) {
       List<Long> out = new ArrayList<>();
       for (int i = 0; i < fragments.size(); i++) {
          String[] parts = fragments.get(i).trim().split("\\s+");
          for (String part : parts) if (!part.isBlank()) out.add(starts.get(i));
       }
       return out;
    }
    private static List<Long> timedWordEnds(List<String> fragments, List<Long> ends) {
       List<Long> out = new ArrayList<>();
       for (int i = 0; i < fragments.size(); i++) {
          String[] parts = fragments.get(i).trim().split("\\s+");
          for (String part : parts) if (!part.isBlank()) out.add(ends.get(i));
       }
       return out;
    }
   private static long time(String value, long fallback) { if (value == null) return fallback; try { String[] p = value.split(":"); if (p.length == 3) return Math.round((Double.parseDouble(p[0]) * 3600 + Double.parseDouble(p[1]) * 60 + Double.parseDouble(p[2])) * 1000); if (p.length == 2) return Math.round((Double.parseDouble(p[0]) * 60 + Double.parseDouble(p[1])) * 1000); return Math.round(Double.parseDouble(value.replace("ms", "")) * (value.endsWith("ms") ? 1 : 1000)); } catch (Exception e) { return fallback; } }
   private static long[] toLongs(List<Long> values) { long[] a = new long[values.size()]; for (int i = 0; i < a.length; i++) a[i] = values.get(i); return a; }
   private static List<String> mergedWords(List<String> fragments) { List<String> out = new ArrayList<>(); StringBuilder current = new StringBuilder(); for (String fragment : fragments) { for (int i = 0; i < fragment.length(); i++) { char c = fragment.charAt(i); if (Character.isWhitespace(c)) { if (current.length() > 0) { out.add(current.toString()); current.setLength(0); } } else current.append(c); } } if (current.length() > 0) out.add(current.toString()); return out; }
    private static List<Long> mergeFragments(List<String> fragments, List<Long> starts) { List<Long> out = new ArrayList<>(); StringBuilder current = new StringBuilder(); long start = 0; for (int j = 0; j < fragments.size(); j++) { String fragment = fragments.get(j); long fragmentStart = starts.get(j); for (int i = 0; i < fragment.length(); i++) { char c = fragment.charAt(i); if (Character.isWhitespace(c)) { if (current.length() > 0) { out.add(start); current.setLength(0); } } else { if (current.length() == 0) start = fragmentStart; current.append(c); } } } if (current.length() > 0) out.add(start); return out; }
    private static List<Long> mergeFragmentEnds(List<String> fragments, List<Long> ends) { List<Long> out = new ArrayList<>(); StringBuilder current = new StringBuilder(); long end = 0; for (int j = 0; j < fragments.size(); j++) { String fragment = fragments.get(j); end = ends.get(j); for (int i = 0; i < fragment.length(); i++) { char c = fragment.charAt(i); if (Character.isWhitespace(c)) { if (current.length() > 0) { out.add(end); current.setLength(0); } } else current.append(c); } } if (current.length() > 0) out.add(end); return out; }
   private static long number(JsonObject o, String key, long fallback) { try { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : fallback; } catch (Exception e) { return fallback; } }
    private static String string(JsonObject o, String key) { try { return o.has(key) && !o.get(key).isJsonNull() && !o.get(key).getAsString().isBlank() ? o.get(key).getAsString() : null; } catch (Exception e) { return null; } }
    private static boolean bool(JsonObject o, String key) { try { return o.has(key) && !o.get(key).isJsonNull() && o.get(key).getAsBoolean(); } catch (Exception e) { return false; } }

   public static List<NowPlaying.Line> parse(String lrc) {
      List<NowPlaying.Line> out = new ArrayList<>();
      for (String raw : lrc.split("\\r?\\n")) { var m = LRC.matcher(raw); List<Long> times = new ArrayList<>(); int end = 0; while (m.find() && m.start() == end) { times.add(Math.round(Long.parseLong(m.group(1)) * 60000 + Double.parseDouble(m.group(2).replace(':', '.')) * 1000)); end = m.end(); } if (!times.isEmpty()) for (long t : times) { String text = raw.substring(end).trim(); out.add(text.indexOf('<') >= 0 ? enhancedLine(t, text) : line(t, text)); } }
      out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs())); return out;
   }
   static NowPlaying.Line enhancedLine(long t, String text) { var m = WORD_TAG.matcher(text); List<String> words = new ArrayList<>(); List<Long> times = new ArrayList<>(); int last = -1; long lastT = t; while (m.find()) { if (last >= 0) addWords(text.substring(last, m.start()), lastT, words, times); lastT = Math.round(Long.parseLong(m.group(1)) * 60000 + Double.parseDouble(m.group(2).replace(':', '.')) * 1000); last = m.end(); } if (last >= 0) addWords(text.substring(last), lastT, words, times); String clean = WORD_TAG.matcher(text).replaceAll("").replaceAll("\\s+", " ").trim(); return words.isEmpty() ? line(t, clean) : new NowPlaying.Line(t, clean, words.toArray(new String[0]), toLongs(times)); }
   private static void addWords(String chunk, long time, List<String> words, List<Long> times) { for (String w : chunk.trim().split("\\s+")) if (!w.isBlank()) { words.add(w); times.add(time); } }
    public static NowPlaying.Line line(long t, String text) { text = displayText(text); return new NowPlaying.Line(t, text, text.isBlank() ? new String[0] : text.trim().split("\\s+")); }
    private static String displayText(String text) {
       if (text == null) return "";
       return text.replace("\\\\n", " ").replace("\\\\r", " ").replace("\\\\t", " ").replace("\\\\\"", "\"").replace("\\\\'", "'").replace("\\\\", "\\");
    }
   static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
   public static List<NowPlaying.Line> demo() {
      String[] text = {"Neon lights across a frozen sky", "every block a little brighter tonight", "auction's open, put your money down", "highest bid takes the purple crown", "snow is falling on the spawner farm", "sixty four skellys safe from harm", "pay me once and I will pay you twice", "if you win the gamble, that's the price", "time is ticking, only ten to go", "sold to you in the falling snow"};
      List<NowPlaying.Line> out = new ArrayList<>(); long t = 1500L;
      for (String s : text) { out.add(line(t, s)); t += 3400L; }
      out.add(line(t, "")); return out;
   }
}
