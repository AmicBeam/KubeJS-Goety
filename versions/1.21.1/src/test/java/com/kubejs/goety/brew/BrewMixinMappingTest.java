package com.kubejs.goety.brew;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;
import java.util.zip.ZipFile;

/** Checks packaged selectors against a production Goety jar without initializing Minecraft. */
public final class BrewMixinMappingTest {
    private static final String MIXIN = "com/kubejs/goety/mixin/BrewCauldronCapacityMixin";
    private static final String TARGET = "com/Polarice3/Goety/common/blocks/entities/BrewCauldronBlockEntity";

    public static void main(String[] args) throws Exception {
        try (ZipFile mod = new ZipFile(args[0]); ZipFile goety = new ZipFile(args[1])) {
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
                    @SuppressWarnings("unchecked")
                    List<String> selectors = (List<String>) value(annotation, "method");
                    for (String selector : selectors) {
                        String resolved = selector;
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
            String load = "loadAdditional(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V";
            if (target.methods.stream().noneMatch(method -> (method.name + method.desc).equals(load))) {
                throw new AssertionError("Expected the production 1.21.1 BlockEntity loadAdditional signature");
            }
            if (checked != 6) {
                throw new AssertionError("Expected all six cauldron injection targets, checked " + checked);
            }
            auditAnchors(mod, goety);
            auditCallbacks(mod, goety);
            System.out.println("Packaged cauldron Mixin targets passed production Mojang selector checks (" + checked + ")");
        }
    }

    private static ClassNode read(ZipFile jar, String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(jar.getInputStream(jar.getEntry(name + ".class")))
                .accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static void auditAnchors(ZipFile mod, ZipFile goety) throws Exception {
        int checked = 0;
        for (String simple : List.of("BrewCauldronBlockEntityMixin", "BrewEffectInstanceMixin",
                "BrewingCatalystProcessorMixin", "BrewingSacrificeProcessorMixin", "BrewCauldronCraftingStarterMixin")) {
            ClassNode mixin = read(mod, "com/kubejs/goety/mixin/" + simple);
            AnnotationNode definition = mixin.invisibleAnnotations.stream()
                    .filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            @SuppressWarnings("unchecked") List<Type> targets = (List<Type>) value(definition, "value");
            ClassNode target = read(goety, targets.getFirst().getInternalName());
            for (MethodNode handler : mixin.methods) {
                if (handler.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : handler.visibleAnnotations) {
                    if (!annotation.desc.endsWith("/Redirect;") && !annotation.desc.endsWith("/ModifyArg;")) continue;
                    AnnotationNode at = (AnnotationNode) value(annotation, "at");
                    String kind = (String) value(at, "value");
                    String anchor = (String) value(at, "target");
                    @SuppressWarnings("unchecked") List<String> methods = (List<String>) value(annotation, "method");
                    for (String selector : methods) {
                        MethodNode method = target.methods.stream().filter(m -> m.name.equals(selector)
                                || (m.name + m.desc).equals(selector)).findFirst().orElseThrow(
                                () -> new AssertionError(simple + ": missing target method " + selector));
                        int count = 0;
                        for (AbstractInsnNode instruction : method.instructions) {
                            if ("NEW".equals(kind) && instruction instanceof TypeInsnNode node
                                    && node.getOpcode() == Opcodes.NEW && node.desc.equals(anchor)) count++;
                            if ("INVOKE".equals(kind) && instruction instanceof MethodInsnNode node
                                    && ("L" + node.owner + ";" + node.name + node.desc).equals(anchor)) count++;
                        }
                        Object ordinal = value(at, "ordinal");
                        int minimum = ordinal instanceof Integer number && number >= 0 ? number + 1 : 1;
                        if (count < minimum) throw new AssertionError(simple + ": missing " + kind + " "
                                + anchor + " in " + selector);
                        checked++;
                    }
                }
            }
        }
        if (checked < 9) throw new AssertionError("Expected all brew redirects and starter anchors, checked " + checked);
        System.out.println("Production brew bytecode anchors passed (" + checked + ")");
    }

    private static void auditCallbacks(ZipFile mod, ZipFile goety) throws Exception {
        for (String simple : List.of("BrewCauldronCapacityMixin", "DarkAltarBlockEntityMixin")) {
            ClassNode mixin = read(mod, "com/kubejs/goety/mixin/" + simple);
            AnnotationNode definition = mixin.invisibleAnnotations.stream()
                    .filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            @SuppressWarnings("unchecked") List<Type> targets = (List<Type>) value(definition, "value");
            ClassNode target = read(goety, targets.getFirst().getInternalName());
            for (MethodNode handler : mixin.methods) {
                if (handler.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : handler.visibleAnnotations) {
                    if (!annotation.desc.endsWith("/Inject;")) continue;
                    Type[] parameters = Type.getArgumentTypes(handler.desc);
                    int callback = 0;
                    while (callback < parameters.length && !parameters[callback].getDescriptor()
                            .startsWith("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo")) callback++;
                    if (callback == 0) continue;
                    @SuppressWarnings("unchecked") List<String> selectors = (List<String>) value(annotation, "method");
                    for (String selector : selectors) {
                        MethodNode method = target.methods.stream().filter(m -> m.name.equals(selector)
                                || (m.name + m.desc).equals(selector)).findFirst().orElseThrow();
                        Type[] expected = Type.getArgumentTypes(method.desc);
                        if (expected.length != callback) throw new AssertionError(simple + ": callback arity differs for " + selector);
                        for (int i = 0; i < callback; i++) {
                            if (!expected[i].equals(parameters[i])) throw new AssertionError(simple + ": callback argument " + i + " differs for " + selector);
                        }
                    }
                }
            }
        }
        System.out.println("Production ritual and cauldron callback descriptors passed");
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
