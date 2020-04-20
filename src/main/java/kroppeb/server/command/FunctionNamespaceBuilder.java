package kroppeb.server.command;

import kroppeb.server.command.Arguments.Resource;
import kroppeb.server.command.commands.Buildable;
import kroppeb.server.command.commands.Summon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static kroppeb.server.command.Util.loadInt;
import static org.objectweb.asm.Opcodes.*;

public class FunctionNamespaceBuilder {
	final private ClassVisitor writer;
	final private String name;
	final private String fullName;
	final private String descriptor;
	final private List<CommandData> fields = new ArrayList<>();
	final private List<FunctionBuilder> functions = new ArrayList<>();
	
	public FunctionNamespaceBuilder(ClassVisitor writer, String name) {
		this.writer = writer;
		this.name = name;
		this.fullName = "kroppeb/potassium/generated/" + name;
		this.descriptor = "L" + fullName + ";";
		writer.visit(V1_8,  ACC_FINAL | ACC_PUBLIC | ACC_SUPER, this.name, null, "java/lang/Object", null);
		// I'm assuming this is to make accessors or something?
		// This allows the nested class to access private variables?
		// Don't know if we actually need it
		// TODO test what happens if we remove it
		writer.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);
	}
	
	public FunctionBuilder addFunction(String name) {
		FunctionBuilder fb = new FunctionBuilder(name);
		functions.add(fb);
		return fb;
	}
	
	public class FunctionBuilder {
		final String name;
		final List<CommandData> commands = new ArrayList<>();
		
		public FunctionBuilder(String name) {
			this.name = name;
		}
		
		public void addCommand(Command cmd) {
			CommandData cd = new CommandData(cmd, fields.size());
			commands.add(cd);
			fields.add(cd);
		}
		
		public void build(){
			MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, name, "(Lnet/minecraft/server/command/ServerCommandSource;)V", null, null);
			mv.visitCode();
			
			// debug info
			Label start = new Label();
			Label end = new Label();
			
			mv.visitLabel(start);
			
			for (CommandData cd : commands) {
				mv.visitFieldInsn(GETSTATIC, FunctionNamespaceBuilder.this.name, cd.name, "Lkroppeb/server/command/Command;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEINTERFACE, "kroppeb/server/command/commands/Command", "execute", "(Lnet/minecraft/server/command/ServerCommandSource;)V", true);
			}
			mv.visitInsn(RETURN);
			
			// debug info
			mv.visitLabel(end);
			{
				mv.visitLocalVariable("source", "Lnet/minecraft/server/command/ServerCommandSource;", null, start, end, 0);
			}
			
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
		
		public void addField(Buildable buildable){
			// TODO remove
		}
	}
	
	public static void main(String[] args) {
		ClassWriter writer = new ClassWriter(0);
		ClassVisitor visitor = writer;
		visitor = new CheckClassAdapter(visitor);
		visitor = new TraceClassVisitor(visitor, new PrintWriter(System.out));
		FunctionNamespaceBuilder builder = new FunctionNamespaceBuilder(visitor, "bat_grenades");
		CompoundTag nbt0 = new CompoundTag();
		
		nbt0.putString("id", "minecraft:creeper");
		nbt0.putString("CustomName", "'{\"translate\": \"entity.minecraft.bat\"}'");
		nbt0.putByte("ExplosionRadius", (byte) 1);
		nbt0.putByte("ignited", (byte) 1);
		nbt0.putShort("Fuse", (short) 0);
		ListTag tags = new ListTag();
		tags.addTag(0, StringTag.of("gm4_bat_grenade"));
		nbt0.put("Tags", tags);
		
		Command cmd = Summon.of(new Resource(null, new String[]{"creeper"}), BuildablePos.SELF, nbt0);
		FunctionBuilder fb = builder.addFunction("summon");
		fb.addCommand(cmd);
		
		builder.build();
		
		
		
		
		File f = new File("K:\\Minecraft\\fabric\\potassium\\src\\main\\java\\kroppeb\\test\\result.class");
		try (FileOutputStream fos = (new FileOutputStream(f))){
			fos.write(writer.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void build() {
		// fields
		int i = 0;
		for (CommandData cd : fields)
			writer
					.visitField(ACC_STATIC, cd.name, "Lkroppeb/server/command/Command;", null, null)
					.visitEnd();
		
		/*
		{
		
			// old <init>
			StringBuilder stringBuilder = new StringBuilder("(");
			for (int j = 0; j < fields.size(); j++)
				stringBuilder.append("Lkroppeb/server/command/Command;");
			stringBuilder.append(")V");
			
			
			MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", stringBuilder.toString(), null, null);
			
			init.visitCode();
			
			// debug info
			Label start = new Label();
			Label end = new Label();
			
			init.visitLabel(start);
			
			// call super constructor
			init.visitVarInsn(ALOAD, 0);
			init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			
			i = 1;
			for (CommandData cd : fields) {
				init.visitVarInsn(ALOAD, 0);
				init.visitVarInsn(ALOAD, i);
				init.visitFieldInsn(PUTFIELD, fullName, cd.name, "Lkroppeb/server/command/Command;");
			}
			
			
			init.visitInsn(RETURN);
			
			// debug info
			init.visitLabel(end);
			{
				init.visitLocalVariable("this", descriptor, null, start, end, 0);
				i = 1;
				for (CommandData cd : fields) {
					init.visitLocalVariable(cd.name, "Lkroppeb/server/command/Command;", null,start, end, i++);
				}
			}
			init.visitMaxs(2, fields.size() + 1);
			
			init.visitEnd();
		}*/
		
		for(FunctionBuilder fb : functions){
			fb.build();
		}
		
		
		MethodVisitor clinit = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		clinit.visitCode();
		Label start = new Label();
		clinit.visitLabel(start);
		
		clinit.visitFieldInsn(GETSTATIC, "kroppeb/server/command/CommandLoader", "functions", "Ljava/util/Map;");
		clinit.visitVarInsn(ASTORE, 0);
		
		for (FunctionBuilder function : functions) {
			clinit.visitVarInsn(ALOAD, 0);
			clinit.visitLdcInsn(function.name);
			clinit.visitInvokeDynamicInsn(
					"execute",
					"()Lkroppeb/server/command/Command;",
					new Handle(Opcodes.H_INVOKESTATIC,
							"java/lang/invoke/LambdaMetafactory",
							"metafactory",
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
							false),
					Type.getType(
							"(Lnet/minecraft/server/command/ServerCommandSource;)I"
					),
					new Handle(
							Opcodes.H_INVOKESTATIC,
							"kroppeb/test/Generated",
							function.name,
							"(Lnet/minecraft/server/command/ServerCommandSource;)I",
							false
					),
					Type.getType(
							"(Lnet/minecraft/server/command/ServerCommandSource;)I"
					));
			clinit.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
			clinit.visitInsn(POP);
		}
		
		
		
		
		clinit.visitMethodInsn(INVOKESTATIC, "kroppeb/server/command/CommandLoader", "loadAll", "()V", false);
		
		Label part2 = new Label();
		clinit.visitLabel(part2);
		
		clinit.visitFieldInsn(GETSTATIC, "kroppeb/server/command/CommandLoader", "commands", "[Lkroppeb/server/command/Command;");
		clinit.visitVarInsn(ASTORE, 0);
		
		for(int j = 0; j < functions.size(); j++){
			clinit.visitVarInsn(ALOAD, 0);
			loadInt(clinit, j);
			clinit.visitInsn(AALOAD);
			clinit.visitFieldInsn(PUTSTATIC, "kroppeb/test/Generated", "command$" + j , "Lkroppeb/server/command/Command;");
		}
		
		Label end = new Label();
		clinit.visitLabel(end);
		clinit.visitInsn(RETURN);
		clinit.visitLocalVariable("functions", "Ljava/util/Map;", null, start, part2, 0);
		clinit.visitLocalVariable("commands", "[Lkroppeb/server/command/Command;", null, part2, end, 0);
		
		clinit.visitMaxs(3, 1);
		clinit.visitEnd();
		
		writer.visitEnd();
	}
}
