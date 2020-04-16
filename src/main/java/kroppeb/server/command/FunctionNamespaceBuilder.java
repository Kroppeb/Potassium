package kroppeb.server.command;

import kroppeb.server.command.Arguments.Resource;
import kroppeb.server.command.commands.Buildable;
import kroppeb.server.command.commands.Summon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
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
	final private List<Buildable> fields = new ArrayList<>();
	final private List<FunctionBuilder> functions = new ArrayList<>();
	
	public FunctionNamespaceBuilder(ClassVisitor writer, String name) {
		this.writer = writer;
		this.name = "kroppeb/potassium/generated/" + name;
		writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, this.name, null, "java/lang/Object", null);
	}
	
	public FunctionBuilder addFunction(String name) {
		FunctionBuilder fb = new FunctionBuilder(name);
		functions.add(fb);
		return fb;
	}
	
	public class FunctionBuilder {
		final String name;
		final List<Command> commands = new ArrayList<>();
		
		public FunctionBuilder(String name) {
			this.name = name;
		}
		
		public void addCommand(Command cmd) {
			cmd.addFields(this);
			commands.add(cmd);
		}
		
		public void build(){
			MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_FINAL, name, "(Lnet/minecraft/server/command/ServerCommandSource;)V", null, null);
			mv.visitCode();
			int stackSize = 0;
			for (Command command : commands) {
				mv.visitVarInsn(ALOAD, 1);
				int innerStackSize = command.buildTo(mv, FunctionNamespaceBuilder.this.name) + 1;
				if(stackSize < innerStackSize)
					stackSize = innerStackSize;
			}
			mv.visitInsn(RETURN);
			mv.visitMaxs(stackSize, 2);
			mv.visitEnd();
		}
		
		public void addField(Buildable buildable){
			buildable.setIndex(fields.size());
			fields.add(buildable);
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
		
		Command cmd = new Summon(new Resource(null, new String[]{"creeper"}), BuildablePos.SELF, nbt0);
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
		for (Buildable b : fields)
			writer
					.visitField(ACC_FINAL | ACC_STATIC, b.getName(), b.getDescriptor(), null, null)
					.visitEnd();
		
		{
			MethodVisitor clinit = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
			clinit.visitCode();
			int stackSize = 0;
			for (Buildable b : fields) {
				int s = b.buildTo(clinit);
				clinit.visitFieldInsn(PUTSTATIC, this.name, b.getName(), b.getDescriptor());
				if (s > stackSize)
					stackSize = s;
			}
			clinit.visitInsn(RETURN);
			clinit.visitMaxs(stackSize, 0);
			clinit.visitEnd();
		}
		
		for(FunctionBuilder fb : functions){
			fb.build();
		}
		
		writer.visitEnd();
	}
}
