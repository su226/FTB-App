package net.creeperhost.creeperlauncher.data.forge.installerv1;

import com.google.gson.*;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.maven.MavenNotation;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by covers1624 on 25/1/22.
 */
public class InstallProfile {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(VersionManifest.OS.class, new VersionManifest.OsDeserializer())
            .registerTypeAdapter(MavenNotation.class, new MavenNotationAdapter())
            .create();

    public Install install;
    public VersionInfo versionInfo;

    public static class Install {

        public String profileName;
        public String target;
        public MavenNotation path;
        public String version;
        public String filePath;
        public String minecraft;

        @Nullable
        public JsonElement transform;
        public boolean stripMeta;
    }

    public static class VersionInfo {

        public String id;
        public String time;
        public String releaseTime;
        public String type;
        public String minecraftArguments;
        public String mainClass;
        @Nullable
        public String inheritsFrom;
        @Nullable
        public String jar;
        public JsonObject logging;
        public List<Library> libraries = new LinkedList<>();
    }

    public static class Library extends VersionManifest.Library {

        public List<String> checksums = new ArrayList<>();
        @Nullable
        public Boolean serverreq;
        @Nullable
        public Boolean clientreq;
    }
}
