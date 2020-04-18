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
		writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, this.name, null, "java/lang/Object", null);
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
			MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_FINAL, name, "(Lnet/minecraft/server/command/ServerCommandSource;)V", null, null);
			mv.visitCode();
			
			// debug info
			Label start = new Label();
			Label end = new Label();
			
			mv.visitLabel(start);
			
			for (CommandData cd : commands) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, FunctionNamespaceBuilder.this.name, cd.name, "Lkroppeb/server/command/Command;");
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEINTERFACE, "kroppeb/server/command/commands/Command", "execute", "(Lnet/minecraft/server/command/ServerCommandSource;)V", true);
			}
			mv.visitInsn(RETURN);
			
			// debug info
			mv.visitLabel(end);
			{
				mv.visitLocalVariable("this", descriptor, null, start, end, 0);
				mv.visitLocalVariable("source", "Lnet/minecraft/server/command/ServerCommandSource;", null, start, end, 1);
			}
			
			mv.visitMaxs(2, 2);
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
					.visitField(ACC_FINAL, cd.name, "Lkroppeb/server/command/Command;", null, null)
					.visitEnd();
		
		{
			StringBuilder stringBuilder = new StringBuilder("(");
			for (CommandData cd : fields)
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
		}
		
		for(FunctionBuilder fb : functions){
			fb.build();
		}
		
		writer.visitEnd();
	}
}
