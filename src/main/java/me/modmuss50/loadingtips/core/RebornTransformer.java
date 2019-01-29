package me.modmuss50.loadingtips.core;

import com.google.common.collect.Lists;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RebornTransformer implements IClassTransformer {

	List<ClassTransformer> classTransformers = new ArrayList<>();

	public ClassTransformer transformClass(String className) {
		ClassTransformer classTransformer = new ClassTransformer(className);
		classTransformers.add(classTransformer);
		return classTransformer;
	}

	public static class ClassTransformer {

		String name;
		List<MethodTransformer> methodTransformers = new ArrayList<>();

		public ClassTransformer(String name) {
			this.name = name;
		}

		public MethodTransformer findMethod(String name, String desc) {
			MethodTransformer methodTransformer = new MethodTransformer(name, desc);
			methodTransformers.add(methodTransformer);
			return methodTransformer;
		}

		private List<MethodTransformer> getMethodTransformers(MethodNode methodNode) {
			return methodTransformers.stream().filter(methodTransformer -> methodTransformer.name.equals(methodNode.name) && methodTransformer.desc.equals(methodNode.desc)) //TODO handle srg names in a nice way ;)
				.collect(Collectors.toList());
		}

		private void handle(ClassNode classNode) {
			classNode.methods.forEach(methodNode -> getMethodTransformers(methodNode).forEach(methodTransformer -> methodTransformer.handle(methodNode)));
		}

	}

	public static class MethodTransformer {

		String name;
		String desc;

		ClassTransformer classTransformer;
		List<Consumer<MethodNode>> methodTransformers = new ArrayList<>();

		public MethodTransformer(String name, String desc) {
			this.name = name;
			this.desc = desc;
		}

		public MethodTransformer(ClassTransformer classTransformer) {
			this.classTransformer = classTransformer;
		}

		public ClassTransformer getClassTransformer() {
			return classTransformer;
		}

		public MethodTransformer transform(Consumer<MethodNode> methodNodeConsumer) {
			methodTransformers.add(methodNodeConsumer);
			return this;
		}

		public MethodTransformer staticMethodCall(String owner, String method, String desc) {
			methodTransformers.add(methodNode -> {
				InsnList insnList = new InsnList();

				insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, method, desc, false));

				AbstractInsnNode first = methodNode.instructions.getFirst();
				methodNode.instructions.insert(first, insnList);
			});
			return this;
		}

		public void findInstruction(Function<List<AbstractInsnNode>, AbstractInsnNode> function, Consumer<Instruction> instructionConsumer) {
			methodTransformers.add(methodNode -> {
				AbstractInsnNode node = function.apply(Lists.newArrayList(methodNode.instructions.iterator()));
				Validate.notNull(node);
				Instruction instruction = new Instruction(methodNode, node);
				instructionConsumer.accept(instruction);
			});
		}

		private void handle(MethodNode methodNode) {
			methodTransformers.forEach(methodNodeConsumer -> methodNodeConsumer.accept(methodNode));
		}
	}

	public static class Instruction {

		MethodNode methodNode;
		AbstractInsnNode node;

		public Instruction(MethodNode methodNode, AbstractInsnNode node) {
			this.methodNode = methodNode;
			this.node = node;
		}
	}

	public List<ClassTransformer> getTransformers(String className) {
		return classTransformers.stream().filter(classTransformer -> classTransformer.name.equals(className)).collect(Collectors.toList());
	}

	//Implimentation :D

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		List<ClassTransformer> classTransformers = getTransformers(name);
		if (!classTransformers.isEmpty()) {
			ClassNode classNode = readClassFromBytes(basicClass);

			classTransformers.forEach(classTransformer -> classTransformer.handle(classNode));

			return writeClassToBytes(classNode);
		}
		return basicClass;
	}

	public static ClassNode readClassFromBytes(byte[] bytes) {
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		return classNode;
	}

	public static byte[] writeClassToBytes(ClassNode classNode) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

}