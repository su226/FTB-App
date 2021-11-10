package net.creeperhost.creeperlauncher.minecraft.jsons;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Version manifest for a given game version.
 * <p>
 * Created by covers1624 on 8/11/21.
 */
public class VersionManifest {

    private static final boolean DEBUG = Boolean.getBoolean("VersionManifest.debug");

    public Arguments arguments;
    public AssetIndex assetIndex;
    public String assets;
    public int complianceLevel;
    public Map<String, Download> downloads;
    public String id;
    public JavaVersion javaVersion;
    public List<Library> libraries;
    public Logging logging;
    public String mainClass;
    public int minimumLauncherVersion;
    public Date time;
    public String type;
    public String inheritsFrom;

    public List<Library> getLibraries(Set<String> features) {
        assert libraries != null;
        return libraries.stream()
                .filter(e -> e.apply(features))
                .collect(Collectors.toList());
    }

    public static class Arguments {

        @JsonAdapter (ArgListDeserializer.class)
        public List<EvalValue> game;
        @JsonAdapter (ArgListDeserializer.class)
        public List<EvalValue> jvm;
    }

    public static class AssetIndex {

        public String id;
        public String sha1;
        public int size;
        public int totalSize;
        public String url;
    }

    public static class Download {

        public String sha1;
        public int size;
        public String url;
    }

    public static class JavaVersion {

        public String component;
        public int majorVersion;
    }

    public static class Library {

        public String name;
        public Extract extract;
        public Downloads downloads;
        @Nullable
        public List<Rule> rules;
        public Map<OS, String> natives;

        public boolean apply(Set<String> features) {
            return Rule.apply(rules, features);
        }
    }

    public static class Logging {

        public LoggingEntry client;
    }

    public static class LoggingEntry {

        public String argument;
        public String type;
        public Download file;
    }

    public static class LoggingDownload extends Download {

        public String id;
    }

    public static class Extract {

        public List<String> exclude;
    }

    public static class LibraryDownload extends Download {

        public String path;
    }

    public static class Downloads {

        public LibraryDownload artifact;
        public Map<String, LibraryDownload> classifiers;
    }

    public static class Rule {

        public Action action;
        @Nullable
        public OSRule os;
        @Nullable
        public Map<String, Boolean> features;

        public Rule() {
        }

        public Rule(Action action, @Nullable OSRule os, @Nullable Map<String, Boolean> features) {
            this();
            this.action = action;
            this.os = os;
            this.features = features;
        }

        // Returns null for no-preference.
        @Nullable
        public Action apply(Set<String> features) {
            if (os != null && !os.applies()) {
                // Os doesn't match. No preference for this rule.
                return null;
            }

            if (this.features != null) {
                for (Map.Entry<String, Boolean> feature : this.features.entrySet()) {
                    if (features.contains(feature.getKey()) != feature.getValue()) {
                        // Feature not enabled. No preference for this rule.
                        return null;
                    }
                }
            }

            // Default action.
            return action;
        }

        public static boolean apply(@Nullable List<Rule> rules, Set<String> features) {
            if (rules == null) return true;

            return rules.stream()
                    .map(e -> e.apply(features))
                    // This mirrors Mojang's logic, Last to return no-preference, takes precedence.
                    .reduce(Action.DISALLOW, (a, b) -> b != null ? b : a) == Action.ALLOW;
        }
    }

    public static class OSRule {

        @Nullable
        public OS name;
        @Nullable
        public String version;
        @Nullable
        public String arch;

        public OSRule() {
        }

        public OSRule(@Nullable OS name, @Nullable String version, @Nullable String arch) {
            this();
            this.name = name;
            this.version = version;
            this.arch = arch;
        }

        public boolean applies() {
            OS current = OS.current();
            if (name != null && name != current) return false;
            if (version != null) {
                try {
                    Pattern pattern = Pattern.compile(version);
                    if (!pattern.matcher(System.getProperty("os.version")).matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (arch != null) {
                try {
                    Pattern pattern = Pattern.compile(arch);
                    if (!pattern.matcher(System.getProperty("os.arch")).matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            return true;
        }
    }

    public enum OS {
        WINDOWS,
        LINUX,
        OSX,
        UNKNOWN;

        @Nullable
        private static OS current;

        public static OS current() {
            if (current != null && !DEBUG) return current;
            current = parse(System.getProperty("os.name"));
            return current;
        }

        static OS parse(String name) {
            name = name.toLowerCase(Locale.ROOT);
            if (name.contains("win")) {
                return WINDOWS;
            } else if (name.contains("mac") || name.contains("osx")) {
                return OSX;
            } else if (name.contains("linux")) {
                return LINUX;
            } else {
                return UNKNOWN;
            }
        }
    }

    public enum Action {
        ALLOW,
        DISALLOW,
    }

    public static class EvalValue {

        private final List<String> values;

        public EvalValue(List<String> values) {
            this.values = values;
        }

        public List<String> eval(Set<String> feature) {
            return values;
        }
    }

    public static class RuledEvalValue extends EvalValue {

        private final List<Rule> rules;

        public RuledEvalValue(List<String> values, List<Rule> rules) {
            super(values);
            this.rules = rules;
        }

        @Override
        public List<String> eval(Set<String> feature) {
            if (!Rule.apply(rules, feature)) return List.of();

            return super.eval(feature);
        }
    }

    public static class ArgListDeserializer implements JsonDeserializer<List<EvalValue>> {

        private static final Type RULES_LIST = new TypeToken<List<Rule>>() { }.getType();

        @Override
        public List<EvalValue> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<EvalValue> evalValues = new ArrayList<>();
            List<String> list = new ArrayList<>();
            if (!json.isJsonArray()) throw new JsonParseException("Expected JsonArray. '" + json + "'");
            for (JsonElement elm : json.getAsJsonArray()) {
                if (elm.isJsonPrimitive()) {
                    list.add(elm.getAsString());
                } else if (elm.isJsonObject()) {
                    evalValues.add(new EvalValue(List.copyOf(list)));
                    list.clear();

                    JsonObject obj = elm.getAsJsonObject();
                    if (!obj.has("value")) throw new JsonParseException("Missing 'value' element. '" + obj + "'");
                    JsonElement value = obj.get("value");
                    if (!value.isJsonPrimitive() && !value.isJsonArray()) throw new JsonParseException("Expected JsonPrimitive or JsonArray. '" + value + "'");
                    List<String> values = value.isJsonPrimitive() ? List.of(value.getAsString()) : getStringList(value.getAsJsonArray());
                    if (!obj.has("rules")) throw new JsonParseException("Missing 'rules' element. '" + obj + "'");

                    List<Rule> rules = context.deserialize(obj.get("rules"), RULES_LIST);
                    evalValues.add(new RuledEvalValue(values, rules));
                }
            }
            if (!list.isEmpty()) {
                evalValues.add(new EvalValue(List.copyOf(list)));
            }
            return evalValues;
        }

        private static List<String> getStringList(JsonArray array) {
            List<String> values = new ArrayList<>();
            for (JsonElement elm : array) {
                if (!elm.isJsonPrimitive()) throw new JsonParseException("Expected JsonPrimitive '" + array + "'");
                values.add(elm.getAsString());
            }
            return List.copyOf(values);
        }
    }
}
