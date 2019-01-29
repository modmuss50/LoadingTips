package me.modmuss50.loadingtips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.SplashProgress;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.lwjgl.opengl.GL11.glTranslatef;

public class LoadingTipsHooks {

	private static FontRenderer fontRenderer = null;
	private static LoadingTipsConfig config;
	private static int frames = 1;
	private static int tipNum;

	private static void setup() {
		try {
			Field field = SplashProgress.class.getDeclaredField("fontRenderer");
			field.setAccessible(true);
			fontRenderer = (FontRenderer) field.get(null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to setup Loading Tips", e);
		}
		File confDir = new File("config");
		try {
			config = LoadingTipsConfig.load(new File(confDir, "loadingtips.json"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load loadingtips config", e);
		}
		config.loadOnline();
	}

	public static void draw() {
		if (fontRenderer == null) {
			setup();
		}

		if (config == null || config.tips.isEmpty()) {
			return;
		}

		GL11.glPushMatrix();
		{
			glTranslatef(0, 0, 0);
			List<String> tips = config.getAllTips();
			String tip = tips.get(tipNum);
			drawString(tip, config.color);
		}
		GL11.glPopMatrix();
		frames++;
		if (frames % 500 == 0) {
			tipNum++;
			if (tipNum == config.getAllTips().size()) {
				tipNum = 0;
			}
		}
	}

	private static void drawString(String text, int col) {
		GL11.glPushMatrix();
		{
			GL11.glPushMatrix();
			color(col);
			GL11.glScalef(2, 2, 1);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			fontRenderer.drawString(text, 35, 80, 0);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glPopMatrix();
		}
		GL11.glPopMatrix();

	}

	private static void color(int color) {
		GL11.glColor3ub((byte) ((color >> 16) & 0xFF), (byte) ((color >> 8) & 0xFF), (byte) (color & 0xFF));
	}

}
