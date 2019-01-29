package me.modmuss50.loadingtips.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class ClassTransformer implements IClassTransformer {

	private static final RebornTransformer transformer = new RebornTransformer();

	static {
		transformer
			.transformClass("net.minecraftforge.fml.client.SplashProgress$2")
			.findMethod("run", "()V")
			.findInstruction(nodes -> {
			//Finds the 2nd glDisable call, just before the memory bar
			int count = 0;
			for (AbstractInsnNode node : nodes) {
				if (node instanceof MethodInsnNode) {
					MethodInsnNode methodNode = (MethodInsnNode) node;
					if (methodNode.name.equals("glDisable")) {
						count++;
					}
					if (count == 2) {
						return node;
					}
				}
			}
			return null; //Something really bad has happened if this gets called, RebornTransformer will crash the game
		}, instruction -> {
				//Add a simple method call to the hooks class
			MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "me/modmuss50/loadingtips/LoadingTipsHooks", "draw", "()V", false);
			instruction.methodNode.instructions.insert(instruction.node, node);
		});
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		return transformer.transform(name, transformedName, basicClass);
	}
}
