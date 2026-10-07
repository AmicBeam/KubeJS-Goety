package com.kubejs.goety.brew;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipFile;

/** Checks packaged selectors against a production Goety jar without initializing Minecraft. */
public final class BrewMixinMappingTest {
    private static final String MIXIN = "com/kubejs/goety/mixin/BrewCauldronCapacityMixin";
    private static final String TARGET = "com/Polarice3/Goety/common/blocks/entities/BrewCauldronBlockEntity";

    public static void main(String[] args) throws Exception {
        try (ZipFile mod = new ZipFile(args[0]); ZipFile goety = new ZipFile(args[1])) {
            ReferenceMapper mapper = ReferenceMapper.read(new InputStreamReader(
                    mod.getInputStream(mod.getEntry("kubejs_goety.refmap.json")), StandardCharsets.UTF_8), "test");
            mapper.setContext("searge");
            ClassNode mixin = read(mod, MIXIN);
            ClassNode target = read(goety, TARGET);
            int checked = 0;
            for (MethodNode handler : mixin.methods) {
                if (handler.visibleAnnotations == null) {
                    continue;
                }
                for (AnnotationNode annotation : handler.visibleAnnotations) {
                    if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) {
                        continue;
                    }
                    boolean remap = Boolean.TRUE.equals(value(annotation, "remap"));
                    @SuppressWarnings("unchecked")
                    List<String> selectors = (List<String>) value(annotation, "method");
                    for (String selector : selectors) {
                        String resolved = remap ? mapper.remap(MIXIN, selector) : selector;
                        if (resolved.startsWith("L")) {
                            resolved = resolved.substring(resolved.indexOf(';') + 1);
                        }
                        int descriptor = resolved.indexOf('(');
                        String name = descriptor < 0 ? resolved : resolved.substring(0, descriptor);
                        String desc = descriptor < 0 ? null : resolved.substring(descriptor);
                        boolean found = target.methods.stream().anyMatch(method -> method.name.equals(name)
                                && (desc == null || method.desc.equals(desc)));
                        if (!found) {
                            throw new AssertionError(handler.name + ": " + selector + " resolves to missing target " + resolved);
                        }
                        checked++;
                    }
                }
            }
            String load = "load(Lnet/minecraft/nbt/CompoundTag;)V";
            if (!mapper.remap(MIXIN, load).endsWith(";m_142466_(Lnet/minecraft/nbt/CompoundTag;)V")) {
                throw new AssertionError("Packaged refmap must map BlockEntity.load to its production SRG name");
            }
            if (checked != 6) {
                throw new AssertionError("Expected all six cauldron injection targets, checked " + checked);
            }
            System.out.println("Packaged cauldron Mixin targets passed production mapping checks (" + checked + ")");
        }
    }

    private static ClassNode read(ZipFile jar, String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(jar.getInputStream(jar.getEntry(name + ".class")))
                .accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static Object value(AnnotationNode annotation, String name) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals(name)) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }
}
