/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.wildfly.extension.criu.CRIUIntegration.getCriuPath;
import static org.wildfly.extension.criu.CRIUIntegration.getDumpPath;
import static org.wildfly.extension.criu.CRIUIntegration.getMarkerPath;
import static org.wildfly.extension.criu.Messages.ROOT_LOGGER;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.as.controller.ModelController.CheckpointIntegration.CheckpointStrategy;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.server.ElapsedTime;

/**
 * Integrates the WildFly kernel with OpenJ9 {@code org.eclipse.openj9.criu.CRIUSupport}
 * via pre-checkpoint and post-restore hook runnables.
 * <p>
 *     Note: This class heavily uses reflection in order to avoid compile-time dependencies on classes only
 *     available in OpenJ9 (and only in Linux versions of it).
 * </p>
 */
final class J9Integration {

    private static final String CRIU_TYPE = "OpenJ9 CRIU";

    /**
     * Installs the OpenJ9 CRIU integration.
     * @param supportedStrategies checkpointing strategies supported by the server kernel. Cannot be {@code null}.
     * @param processStateNotifier notifier for tracking process state changes. Cannot be {@code null}.
     * @param elapsedTime tracker for process elapsed time. Cannot be {@code null}.
     * @return a checkpoint integration object the management kernel can use, or {@code null} if the OpenJ9 CRIU
     *         integration classes are not available or if they report CRIU is not enabled.
     */
    static CRIUExecutor install(Set<CheckpointStrategy> supportedStrategies, ProcessStateNotifier processStateNotifier,
                                ElapsedTime elapsedTime) {
        CRIUExecutor result = null;
        if (ReflectionHolder.HAS_J9_CRIU) {
            Path crDir = getCriuPath();
            if (cleanDirectory(crDir)) {
                Path dumpDir = getDumpPath();
                try {
                    if (!Files.exists(dumpDir)) {
                        Files.createDirectories(dumpDir);
                    }
                    Object criuSupport = ReflectionHolder.CRIU_SUPPORT_CTOR.newInstance(dumpDir);
                    final CheckpointIntegrationImpl cibase = new J9CRIUExecutor(supportedStrategies, processStateNotifier,
                            elapsedTime, criuSupport);
                    ReflectionHolder.SET_FILE_LOCKS.invoke(criuSupport, true);
                    ReflectionHolder.SET_SHELL_JOB.invoke(criuSupport, true);
                    ReflectionHolder.SET_TCP_ESTABLISHED.invoke(criuSupport, true);
                    ReflectionHolder.SET_UNPRIVILEGED.invoke(criuSupport, true);
                    ReflectionHolder.REGISTER_PRE_CHECKPOINT.invoke(criuSupport, (Runnable) cibase::beforeCheckpoint,
                            ReflectionHolder.HOOK_MODE_CONCURRENT, 10);
                    ReflectionHolder.REGISTER_POST_RESTORE.invoke(criuSupport, (Runnable) cibase::afterRestore,
                            ReflectionHolder.HOOK_MODE_CONCURRENT, 10);
                    registerRestoreEnvFile(criuSupport);
                    result = cibase;
                    ROOT_LOGGER.criuImplementationEnabled("OpenJ9 CRIU");
                } catch (IOException | InstantiationException | IllegalAccessException e) {
                    ROOT_LOGGER.criuIntegrationFailed(CRIU_TYPE, e);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getCause();
                    t = t == null ? e : t;
                    ROOT_LOGGER.criuIntegrationFailed(CRIU_TYPE, t);
                }
            } else {
                ROOT_LOGGER.checkpointDirAlreadyExists(crDir);
            }
        }
        return result;
    }

    private static void registerRestoreEnvFile(Object criuSupport) throws InvocationTargetException, IllegalAccessException {
        Path configDir = new File(System.getProperty("jboss.server.config.dir")).toPath();
        Path envVars = configDir.resolve("criu-restore-env-vars.txt");
        if (Files.exists(envVars)) {
            ReflectionHolder.REGISTER_RESTORE_ENV_FILE.invoke(criuSupport, envVars);
        }
    }

    private static boolean cleanDirectory(Path dir) {
        boolean result = !Files.exists(dir);
        if (!result) {
            if (Files.exists(getMarkerPath())) {
                //noinspection ResultOfMethodCallIgnored
                getMarkerPath().toFile().delete();
            }
            try (Stream<Path> stream = Files.walk(dir)) {
                //noinspection ResultOfMethodCallIgnored
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                result = !Files.exists(dir);
            } catch (IOException e) {
                ROOT_LOGGER.debugf(e, "Cannot empty %s", dir);
            }
        }
        return result;
    }

    /**
     * Holder class to reflect on the OpenJ9 CRIU classes when first needed.
     * The primary point of using a holder is to defer any associated logging
     * until J9Integration.install is called.
     */
    private static class ReflectionHolder {

        private static final boolean HAS_J9_CRIU;
        private static final Constructor<?> CRIU_SUPPORT_CTOR;
        private static final Enum<?> HOOK_MODE_CONCURRENT;
        private static final Method SET_FILE_LOCKS;
        private static final Method SET_SHELL_JOB;
        private static final Method SET_TCP_ESTABLISHED;
        private static final Method SET_UNPRIVILEGED;
        private static final Method REGISTER_PRE_CHECKPOINT;
        private static final Method REGISTER_POST_RESTORE;
        private static final Method REGISTER_RESTORE_ENV_FILE;
        private static final Method CHECKPOINT_JVM;

        static {
            boolean hasCriu = false;
            Class<?> cs_cls = null;
            Constructor<?> cs_ctor = null;
            Enum<?> hm_concurrent = null;
            Method set_file_locks = null;
            Method set_shell_job = null;
            Method set_tcp_established = null;
            Method set_unprivileged = null;
            Method register_pre = null;
            Method register_post = null;
            Method register_restore_env = null;
            Method checkpoint_jvm = null;
            final String cruiclassname = "org.eclipse.openj9.criu.CRIUSupport";
            try {
                cs_cls = J9Integration.class.getClassLoader().loadClass(cruiclassname);
                Method cs_enabled = cs_cls.getMethod("isCRIUSupportEnabled");
                hasCriu = (boolean) cs_enabled.invoke(null);
            }
            catch (ClassNotFoundException cnfe) {
                // no OpenJ9 CRIU!
                ROOT_LOGGER.debug(cruiclassname + " is not available");
            }
            catch (Exception e) {
                ROOT_LOGGER.criuIntegrationFailed(CRIU_TYPE, e);
            }

            if (hasCriu) {
                hasCriu = false; // reset to false until we successfully do the rest of the reflection

                try {
                    cs_ctor = cs_cls.getConstructor(Path.class);
                    ////noinspection unchecked
                    //Class<? extends Enum<?>> hm_cls = (Class<? extends Enum<?>>) J9Integration.class.getClassLoader().loadClass(cruiclassname + ".HookMode");
                    Class<? extends Enum<?>> hm_cls = getHookModeClass(cs_cls);
                    for (Enum<?> en : hm_cls.getEnumConstants()) {
                        if ("CONCURRENT_MODE".equals(en.name())) {
                            hm_concurrent = en;
                        }
                    }
                    if (hm_concurrent == null) {
                        throw new IllegalStateException();
                    }
                    set_file_locks = cs_cls.getMethod("setFileLocks", boolean.class);
                    set_shell_job = cs_cls.getMethod("setShellJob", boolean.class);
                    set_unprivileged = cs_cls.getMethod("setUnprivileged", boolean.class);
                    set_tcp_established = cs_cls.getMethod("setTCPEstablished", boolean.class);
                    register_pre = cs_cls.getMethod("registerPreCheckpointHook", Runnable.class, hm_cls, int.class);
                    register_post = cs_cls.getMethod("registerPostRestoreHook", Runnable.class, hm_cls, int.class);
                    register_restore_env = cs_cls.getMethod("registerRestoreEnvFile", Path.class);
                    checkpoint_jvm = cs_cls.getMethod("checkpointJVM");
                    hasCriu = true;
                } catch (ClassNotFoundException | NoSuchMethodException | RuntimeException e) {
                    ROOT_LOGGER.criuIntegrationFailed(CRIU_TYPE, e);
                }
            } else if (cs_cls != null) {
                String errMsg = "";
                try {
                    Method cs_errmsg = cs_cls.getMethod("getErrorMessage");
                    errMsg = (String) cs_errmsg.invoke(null);
                } catch (Exception ignored) {
                    // ignore
                }
                ROOT_LOGGER.criuImplementationDisabled("OpenJ9 CRIU", errMsg);
            }

            HAS_J9_CRIU = hasCriu;
            CRIU_SUPPORT_CTOR = cs_ctor;
            HOOK_MODE_CONCURRENT = hm_concurrent;
            SET_FILE_LOCKS = set_file_locks;
            SET_SHELL_JOB = set_shell_job;
            SET_TCP_ESTABLISHED = set_tcp_established;
            SET_UNPRIVILEGED = set_unprivileged;
            REGISTER_PRE_CHECKPOINT = register_pre;
            REGISTER_POST_RESTORE = register_post;
            REGISTER_RESTORE_ENV_FILE = register_restore_env;
            CHECKPOINT_JVM = checkpoint_jvm;
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> getHookModeClass(Class<?> criuSupport) throws ClassNotFoundException {
        Class<? extends Enum<?>> result = null;
        for (Method method : criuSupport.getMethods()) {
            if (method.getParameterCount() == 3 && "registerPreCheckpointHook".equals(method.getName())) {
                if (result != null) {
                    throw new IllegalStateException();
                }
                Class<?> type = method.getParameterTypes()[1];
                if (!type.isEnum()) {
                    throw new IllegalStateException();
                }
                result = (Class<? extends Enum<?>>) type;
            }
        }
        if (result == null) {
            throw new ClassNotFoundException(criuSupport.getCanonicalName() + ".HookMode");
        }
        return result;
    }

    private static class J9CRIUExecutor extends CheckpointIntegrationImpl {
        private final Object criuSupport;

        private J9CRIUExecutor(Set<CheckpointStrategy> supportedStrategies, ProcessStateNotifier processStateNotifier,
                               ElapsedTime elapsedTime, Object criuSupport) {
            super(supportedStrategies, processStateNotifier, elapsedTime);
            this.criuSupport = criuSupport;
        }

        @Override
        void executeCheckpoint() throws Exception {
            try {
                ReflectionHolder.CHECKPOINT_JVM.invoke(criuSupport);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else {
                    throw e;
                }
            }
        }
    }
}
