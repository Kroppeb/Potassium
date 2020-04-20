package kroppeb.server.command;

import kroppeb.server.command.commands.Buildable;
import net.minecraft.nbt.*;
import org.objectweb.asm.MethodVisitor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Map;

import static kroppeb.server.command.Util.loadInt;
import static org.objectweb.asm.Opcodes.*;


public class BuildableTag implements Buildable {
	public final CompoundTag item;
	public String name;
	
	
	
	public BuildableTag(CompoundTag item) {
		this.item = item;
	}
	
	
	static public CompoundTag readCompoundTag(Reader reader) {
		CompoundTag tag = new CompoundTag();
		reader.read('{');
		while (!reader.tryRead('}')) {
			String key = reader.readString();
			reader.readChar(':');
			tag.put(key, readTag(reader));
		}
		return tag;
	}
	
	static public Tag readTag(Reader reader) {
		switch (reader.peek()) {
			case '{':
				return readCompoundTag(reader);
			case '[':
				return readListTag(reader);
			default:
				return readPrimitiveTag(reader);
		}
	}
	
	private static ListTag readListTag(Reader reader) {
		reader.readChar('[');
		ListTag list = new ListTag();
		byte type = -1;
		while (reader.peek() != ']') {
			Tag tag = readTag(reader);
			byte newType = tag.getType();
			if (type == -1) {
				type = newType;
			} else if (newType != type) {
				throw new RuntimeException();//LIST_MIXED.createWithContext(this.reader, tagReader_2.getCommandFeedbackName(), tagReader_1.getCommandFeedbackName());
			}
			
			list.add(tag);
			if (!reader.tryRead(',')) {
				break;
			}
			reader.moveNext();
		}
		
		reader.read(']');
		reader.moveNext();
		return list;
	}
	
	static public Tag readPrimitiveTag(Reader reader) {
		if (reader.isQuotedStringStart())
			return StringTag.of(reader.readQuotedString());
		String s = reader.readUnquotedString();
		if (s.endsWith("b")) {
			try {
				return ByteTag.of(Byte.parseByte(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("s")) {
			try {
				return ShortTag.of(Short.parseShort(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("l")) {
			try {
				return LongTag.of(Long.parseLong(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("d")) {
			try {
				return DoubleTag.of(Double.parseDouble(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("f")) {
			try {
				return FloatTag.of(Float.parseFloat(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		try {
			return DoubleTag.of(Double.parseDouble(s));
		} catch (NumberFormatException e) {
			try {
				return IntTag.of(Integer.parseInt(s));
			} catch (NumberFormatException e2) {
				return StringTag.of(s);
			}
		}
	}
	
	@Override
	public String getDescriptor() {
		return "Lnet/minecraft/nbt/CompoundTag;";
	}
	
	@Override
	public int buildTo(MethodVisitor clinit) {
		return buildCompoundTo(clinit, item);
	}
	
	@Override
	public void setIndex(int index) {
		name = "tag$" + index;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	static private int buildTagTo(MethodVisitor clinit, ListTag tag) {
		clinit.visitTypeInsn(NEW, "net/minecraft/nbt/ListTag");
		clinit.visitInsn(DUP);
		clinit.visitMethodInsn(INVOKESPECIAL, "net/minecraft/nbt/ListTag", "<init>", "()V", false);
		
		int stackSize = tag.size() > 0? 4: 2;
		
		for(int i = 0; i < tag.size(); i++){
			Tag value = tag.get(i);
			clinit.visitInsn(DUP);
			loadInt(clinit, i); // stacksize is now 3
			switch(value.getType()){
				case 1: // byte
					loadInt(clinit, ((ByteTag)value).getInt()); // now 4
					clinit.visitMethodInsn(INVOKESTATIC, "net/minecraft/nbt/ByteTag", "of", "(B)Lnet/minecraft/nbt/ByteTag;", false);
					break;
				case 2: // short
					loadInt(clinit, ((ShortTag)value).getInt());
					clinit.visitMethodInsn(INVOKESTATIC, "net/minecraft/nbt/ShortTag", "of", "(S)Lnet/minecraft/nbt/ShortTag;", false);
					break;
				case 3: // int
					loadInt(clinit, ((IntTag)value).getInt());
					clinit.visitMethodInsn(INVOKESTATIC, "net/minecraft/nbt/IntTag", "of", "(I)Lnet/minecraft/nbt/IntTag;", false);
					break;
				case 8: // string
					String v = value.asString();
					clinit.visitLdcInsn(v);
					clinit.visitMethodInsn(INVOKESTATIC, "net/minecraft/nbt/StringTag", "of", "(Ljava/lang/String;)Lnet/minecraft/nbt/StringTag;", false);
					break;
				case 9: // list
					int nestedStackSize = buildTagTo(clinit, (ListTag)value) + 3;
					if(nestedStackSize > stackSize)
						stackSize = nestedStackSize;
					break;
				case 10: // Compound
					nestedStackSize = buildCompoundTo(clinit, (CompoundTag) value) + 3;
					if(nestedStackSize > stackSize)
						stackSize = nestedStackSize;
					break;
				default:
					throw new NotImplementedException();
			}
			clinit.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/nbt/ListTag", "addTag", "(ILnet/minecraft/nbt/Tag;)Z", false);
			clinit.visitInsn(POP);
		}
		return stackSize;
	}
	
	static private int buildCompoundTo(MethodVisitor clinit, CompoundTag tag) {
		int stackSize = 2;
		clinit.visitTypeInsn(NEW, "net/minecraft/nbt/CompoundTag");
		clinit.visitInsn(DUP);
		clinit.visitMethodInsn(INVOKESPECIAL, "net/minecraft/nbt/CompoundTag", "<init>", "()V", false);
		
		for (Map.Entry<String, Tag> i : tag.tags.entrySet()) {
			if(stackSize < 4)
				stackSize = 4;
			String key = i.getKey();
			Tag value = i.getValue();
			clinit.visitInsn(DUP);
			clinit.visitLdcInsn(key);// stacksize is now 3
			switch(value.getType()){
				case 1: // byte
					loadInt(clinit, ((ByteTag)value).getInt()); // now 4
					clinit.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/nbt/CompoundTag", "putByte", "(Ljava/lang/String;B)V", false);
					break;
				case 2: // short
					loadInt(clinit, ((ShortTag)value).getInt());
					clinit.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/nbt/CompoundTag", "putShort", "(Ljava/lang/String;S)V", false);
					break;
				case 3: // int
					loadInt(clinit, ((IntTag)value).getInt());
					clinit.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/nbt/CompoundTag", "putInt", "(Ljava/lang/String;I)V", false);
					break;
				case 8: // string
					String v = value.asString();
					clinit.visitLdcInsn(v);
					clinit.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/nbt/CompoundTag", "putString", "(Ljava/lang/String;Ljava/lang/String;)V", false);
					break;
				case 9: // list
					int nestedStackSize = buildTagTo(clinit, (ListTag)value) + 3;
					if(nestedStackSize > stackSize)
						stackSize = nestedStackSize;
					clinit.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/nbt/CompoundTag", "put", "(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)V", false);
					break;
				case 10: // Compound
					nestedStackSize = buildCompoundTo(clinit, (CompoundTag) value) + 3;
					if(nestedStackSize > stackSize)
						stackSize = nestedStackSize;
					clinit.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/nbt/CompoundTag", "put", "(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)V", false);
					break;
				default:
					throw new NotImplementedException();
			}
		}
		return stackSize;
	}
}
