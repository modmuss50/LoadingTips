package me.modmuss50.loadingtips;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoadingTipsConfig {

	public int color = 0x000000;
	public String url = "";
	public List<String> tips = new ArrayList<>();

	public transient List<String> onlineTips = null;
	private static final Gson GSON = new Gson();

	public static LoadingTipsConfig load(File configFile) throws IOException {
		if (configFile.exists()) {
			String json = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
			LoadingTipsConfig config = GSON.fromJson(json, LoadingTipsConfig.class);
			return config;
		} else {
			LoadingTipsConfig config = new LoadingTipsConfig();
			config.tips.add("This is an example tip");
			config.tips.add("Change your tips by editing loadingtips.json");
			String json = GSON.toJson(config);
			FileUtils.writeStringToFile(configFile, json, StandardCharsets.UTF_8);
			return config;
		}
	}

	public void loadOnline(Runnable complete) {
		if (url.isEmpty()) {
			return;
		}
		new Thread(() -> {
			try {
				String onlineJson = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
				onlineTips = GSON.fromJson(onlineJson, new TypeToken<List<String>>() {
				}.getType());
				complete.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	public List<String> getAllTips() {
		if (onlineTips == null) {
			return tips.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
		}
		return Stream.concat(tips.stream(), onlineTips.stream()).filter(s -> !s.isEmpty()).collect(Collectors.toList());
	}

}
